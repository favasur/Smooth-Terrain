using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using Vintagestory.API.Common;
using Vintagestory.API.Config;
using Vintagestory.API.MathTools;
using Vintagestory.API.Server;

namespace SmoothTerrain
{
    public class SmoothTerrainModSystem : ModSystem
    {
        public const string ConfigFileName = "smoothterrain.json";

        /// <summary>Blocks above this replaceability are decoration and not a terrain surface.</summary>
        private const int MaxSurfaceReplaceable = 4000;

        private ICoreServerAPI sapi;
        private SmoothTerrainConfig config;

        private readonly Dictionary<string, int> cursors = new Dictionary<string, int>();
        private readonly List<BlockOffset> offsets = new List<BlockOffset>();
        private readonly ConcurrentQueue<Vec2i> pendingChunks = new ConcurrentQueue<Vec2i>();
        private int offsetsRadius = -1;

        private struct BlockOffset
        {
            public int Dx;
            public int Dz;
            public int DistanceSq;
        }

        public override void Start(ICoreAPI api)
        {
            base.Start(api);

            api.RegisterBlockClass("BlockTerrainFillet", typeof(BlockTerrainFillet));

            config = api.LoadModConfig<SmoothTerrainConfig>(ConfigFileName);
            if (config == null)
            {
                config = new SmoothTerrainConfig();
                api.StoreModConfig(config, ConfigFileName);
            }

            config.Radius = GameMath.Clamp(config.Radius, 1, 128);
            config.ChunkColumnsPerTick = GameMath.Clamp(config.ChunkColumnsPerTick, 64, 16384);
            api.Logger.Notification("[smoothterrain] loaded (enabled={0}, radius={1})", config.Enabled, config.Radius);
        }

        public override void StartServerSide(ICoreServerAPI api)
        {
            base.StartServerSide(api);
            sapi = api;

            api.RegisterCommand(
                "smoothterrain",
                "Curve the terrain edges around you, or turn smoothing on/off",
                "[radius|on|off]",
                (player, groupId, cmdArgs) => OnSmoothCommand(player, cmdArgs),
                Privilege.controlserver
            );

            // Every chunk column is queued as it loads, so smoothing does not depend on a
            // player walking into it or on running a command. The queue is drained gradually
            // by the same tick listener that keeps the area around players up to date.
            api.Event.ChunkColumnLoaded += OnChunkColumnLoaded;

            api.Event.RegisterGameTickListener(OnServerTick, GameMath.Clamp(config.TickIntervalMs, 50, 10000));
        }

        private void OnChunkColumnLoaded(Vec2i chunkCoord, IWorldChunk[] chunks)
        {
            if (config == null || !config.Enabled) return;
            pendingChunks.Enqueue(chunkCoord);
        }

        private TextCommandResult OnSmoothCommand(IServerPlayer player, CmdArgs cmdArgs)
        {
            string first = cmdArgs.PopWord();
            if (string.Equals(first, "on", StringComparison.OrdinalIgnoreCase))
            {
                return SetEnabled(player, true);
            }
            if (string.Equals(first, "off", StringComparison.OrdinalIgnoreCase))
            {
                return SetEnabled(player, false);
            }

            if (!config.Enabled)
            {
                return TextCommandResult.Error("Smooth Terrain is off. Use /smoothterrain on to enable it", "smoothterrain-disabled");
            }

            if (player?.Entity == null)
            {
                return TextCommandResult.Error("This command must be run by a player", "smoothterrain-noplayer");
            }

            int radius = config.Radius;
            if (!string.IsNullOrEmpty(first) && !int.TryParse(first, out radius))
            {
                return TextCommandResult.Error("Usage: /smoothterrain [radius|on|off]", "smoothterrain-usage");
            }
            radius = GameMath.Clamp(radius, 1, 128);

            IBlockAccessor ba = sapi.World.BlockAccessor;
            int cx = (int)player.Entity.Pos.X;
            int cz = (int)player.Entity.Pos.Z;

            int placed = 0;
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dx = -radius; dx <= radius; dx++)
                {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    placed += SmoothColumn(ba, cx + dx, cz + dz);
                }
            }

            return TextCommandResult.Success(
                "Curved " + placed + " terrain edge(s) within " + radius + " blocks", null);
        }

        /// <summary>
        /// The switch behind the Graphics settings checkbox and /smoothterrain on|off.
        /// Switching off restores the vanilla blocks near the player immediately, so the
        /// world looks unchanged rather than keeping stale curved blocks around.
        /// </summary>
        private TextCommandResult SetEnabled(IServerPlayer player, bool enabled)
        {
            if (config.Enabled == enabled)
            {
                return TextCommandResult.Success(
                    enabled ? "Smooth Terrain is already on" : "Smooth Terrain is already off", null);
            }

            config.Enabled = enabled;
            sapi.StoreModConfig(config, ConfigFileName);
            cursors.Clear();

            if (enabled)
            {
                EnqueueLoadedChunksAroundPlayers();
                return TextCommandResult.Success(
                    "Smooth Terrain on: terrain edges curve automatically as chunks load and you move", null);
            }

            pendingChunks.Clear();

            int reverted = 0;
            if (player?.Entity != null)
            {
                IBlockAccessor ba = sapi.World.BlockAccessor;
                int cx = (int)player.Entity.Pos.X;
                int cz = (int)player.Entity.Pos.Z;
                int radius = config.Radius;

                for (int dz = -radius; dz <= radius; dz++)
                {
                    for (int dx = -radius; dx <= radius; dx++)
                    {
                        if (dx * dx + dz * dz > radius * radius) continue;
                        reverted += RevertColumn(ba, cx + dx, cz + dz);
                    }
                }
            }

            return TextCommandResult.Success(
                "Smooth Terrain off: restored " + reverted + " vanilla terrain block(s)", null);
        }

        private int RevertColumn(IBlockAccessor ba, int x, int z)
        {
            int surfaceY;
            Block surface;
            if (!TryFindSurface(ba, x, z, out surfaceY, out surface)) return 0;
            if (surface.Code?.Domain != TerrainSmoother.TargetDomain) return 0;

            string vanillaCode = TerrainSmoother.MapToVanillaCode(surface.Code.Domain, surface.Code.Path, config);
            if (vanillaCode == null) return 0;

            Block original = ba.GetBlock(new AssetLocation(vanillaCode));
            if (original == null || original.BlockId == 0) return 0;

            ba.SetBlock(original.BlockId, new BlockPos(x, surfaceY, z));
            return 1;
        }

        /// <summary>Re-queues the chunk columns around players, used when smoothing is turned back on.</summary>
        private void EnqueueLoadedChunksAroundPlayers()
        {
            IBlockAccessor ba = sapi.World.BlockAccessor;
            int chunkSize = GlobalConstants.ChunkSize;
            int chunkRadius = (config.Radius + chunkSize - 1) / chunkSize;
            IPlayer[] players = sapi.World.AllOnlinePlayers;

            for (int p = 0; p < players.Length; p++)
            {
                if (players[p]?.Entity == null) continue;

                int pcx = (int)players[p].Entity.Pos.X / chunkSize;
                int pcz = (int)players[p].Entity.Pos.Z / chunkSize;

                for (int dz = -chunkRadius; dz <= chunkRadius; dz++)
                {
                    for (int dx = -chunkRadius; dx <= chunkRadius; dx++)
                    {
                        if (ba.GetMapChunk(pcx + dx, pcz + dz) == null) continue;
                        pendingChunks.Enqueue(new Vec2i(pcx + dx, pcz + dz));
                    }
                }
            }
        }

        /// <summary>
        /// Converts whole newly loaded chunks without waiting for a player to stand next to
        /// them. Processing is budgeted so a teleport or a large view distance cannot stall
        /// the server: whatever does not fit stays queued for the next tick.
        /// </summary>
        private void SmoothQueuedChunks(IBlockAccessor ba)
        {
            int budget = Math.Max(64, config.ChunkColumnsPerTick);
            int processed = 0;
            int chunkSize = GlobalConstants.ChunkSize;

            Vec2i coord;
            while (processed < budget && pendingChunks.TryDequeue(out coord))
            {
                int baseX = coord.X * chunkSize;
                int baseZ = coord.Y * chunkSize;

                for (int dz = 0; dz < chunkSize; dz++)
                {
                    for (int dx = 0; dx < chunkSize; dx++)
                    {
                        SmoothColumn(ba, baseX + dx, baseZ + dz);
                    }
                }

                processed += chunkSize * chunkSize;
            }
        }

        private void OnServerTick(float dt)
        {
            if (!config.Enabled || sapi == null) return;

            IBlockAccessor ba = sapi.World.BlockAccessor;
            SmoothQueuedChunks(ba);

            EnsureOffsets(config.Radius);
            if (offsets.Count == 0) return;

            IPlayer[] players = sapi.World.AllOnlinePlayers;

            for (int p = 0; p < players.Length; p++)
            {
                IPlayer player = players[p];
                if (player?.Entity == null) continue;

                int cx = (int)player.Entity.Pos.X;
                int cz = (int)player.Entity.Pos.Z;

                string uid = player.PlayerUID ?? player.PlayerName ?? "player";
                int cursor;
                if (!cursors.TryGetValue(uid, out cursor) || cursor >= offsets.Count) cursor = 0;

                int end = Math.Min(cursor + Math.Max(1, config.ColumnsPerTick), offsets.Count);
                for (int i = cursor; i < end; i++)
                {
                    BlockOffset offset = offsets[i];
                    SmoothColumn(ba, cx + offset.Dx, cz + offset.Dz);
                }

                cursors[uid] = end >= offsets.Count ? 0 : end;
            }
        }

        /// <summary>
        /// Curves one column's terrain edge. The fillet replaces the column's own surface block
        /// and slopes down to its lower neighbour, so the block the player walks off is the same
        /// block whose collision boxes follow the curve.
        /// </summary>
        private int SmoothColumn(IBlockAccessor ba, int x, int z)
        {
            int surfaceY;
            Block surface;
            if (!TryFindSurface(ba, x, z, out surfaceY, out surface)) return 0;

            string surfaceDomain = surface.Code?.Domain;
            if (surfaceDomain != "game") return 0;

            if (config.SkipLiquidCoveredSurfaces && IsLiquidAt(ba, x, surfaceY + 1, z)) return 0;

            FilletSide? side = FindSingleStepSide(ba, x, z, surfaceY);
            if (side == null) return 0;

            string code = TerrainSmoother.MapToFilletCode(surfaceDomain, surface.Code.Path, side.Value, config);
            if (code == null) return 0;

            Block target = ba.GetBlock(new AssetLocation(code));
            if (target == null || target.BlockId == 0) return 0;

            ba.SetBlock(target.BlockId, new BlockPos(x, surfaceY, z));
            return 1;
        }

        /// <summary>
        /// A fillet spans exactly one block of height, so it can only bridge a one-block drop.
        /// Taller drops are cliffs and are deliberately left alone. The neighbour height is
        /// resolved the same way as the column's own height, so decorations, snow and the exact
        /// convention of the rain map cannot shift the comparison.
        /// </summary>
        private FilletSide? FindSingleStepSide(IBlockAccessor ba, int x, int z, int surfaceY)
        {
            BlockFacing[] facings = BlockFacing.HORIZONTALS;
            for (int i = 0; i < facings.Length; i++)
            {
                BlockFacing facing = facings[i];
                int nx = x + facing.Normali.X;
                int nz = z + facing.Normali.Z;

                int neighbourY;
                Block neighbour;
                if (!TryFindSurface(ba, nx, nz, out neighbourY, out neighbour)) continue;
                if (surfaceY - neighbourY != 1) continue;

                if (config.SkipLiquidCoveredSurfaces && IsLiquidAt(ba, nx, neighbourY + 1, nz)) continue;

                if (facing == BlockFacing.NORTH) return FilletSide.North;
                if (facing == BlockFacing.EAST) return FilletSide.East;
                if (facing == BlockFacing.SOUTH) return FilletSide.South;
                return FilletSide.West;
            }

            return null;
        }

        /// <summary>
        /// Finds the topmost solid terrain block of a column, ignoring air and decorations.
        /// Uses the rain map as a starting point and walks down past air and replaceable blocks
        /// (tall grass, snow layers, ...), so the result is the block a fillet can replace.
        /// </summary>
        private static bool TryFindSurface(IBlockAccessor ba, int x, int z, out int surfaceY, out Block surface)
        {
            surfaceY = 0;
            surface = null;

            BlockPos pos = new BlockPos(x, 0, z);
            if (!ba.IsValidPos(pos) || ba.GetChunkAtBlockPos(pos) == null) return false;

            int startY = ba.GetRainMapHeightAt(pos);
            for (int y = startY; y > 1 && y > startY - 4; y--)
            {
                pos.Y = y;
                Block block = ba.GetBlock(pos);
                if (block == null || block.BlockId == 0) continue;
                if (block.Replaceable > MaxSurfaceReplaceable) continue;

                surfaceY = y;
                surface = block;
                return true;
            }

            return false;
        }

        private static bool IsLiquidAt(IBlockAccessor ba, int x, int y, int z)
        {
            Block block = ba.GetBlock(new BlockPos(x, y, z));
            return block != null && block.MatterState == EnumMatterState.Liquid;
        }

        private void EnsureOffsets(int radius)
        {
            if (offsetsRadius == radius && offsets.Count > 0) return;

            offsets.Clear();
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dx = -radius; dx <= radius; dx++)
                {
                    int distanceSq = dx * dx + dz * dz;
                    if (distanceSq > radius * radius) continue;
                    offsets.Add(new BlockOffset { Dx = dx, Dz = dz, DistanceSq = distanceSq });
                }
            }

            // Nearest columns first: ground under the player is fixed before the horizon.
            offsets.Sort((a, b) => a.DistanceSq.CompareTo(b.DistanceSq));
            offsetsRadius = radius;
        }
    }
}
