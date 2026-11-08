namespace SmoothTerrain
{
    /// <summary>
    /// Maps one family of vanilla terrain blocks onto the fillet blocks that replace
    /// their exposed edges. Codes are matched by prefix: "soil" matches "soil-medium-normal"
    /// and produces "smoothterrain:fillet-soil-medium-normal-north".
    /// </summary>
    public class FilletFamilyConfig
    {
        public string SourceCodePrefix = "soil";
        public string TargetCodePrefix = "fillet-soil";
        public bool Enabled = true;

        /// <summary>
        /// Match the vanilla code exactly instead of by prefix. Used for families whose
        /// vanilla block also has extra partial-height variants (for example the layered
        /// `bonysoil-1`..`bonysoil-7` blocks), which must not be filleted.
        /// </summary>
        public bool ExactMatch = false;

        public FilletFamilyConfig() { }

        public FilletFamilyConfig(string sourceCodePrefix, string targetCodePrefix)
        {
            SourceCodePrefix = sourceCodePrefix;
            TargetCodePrefix = targetCodePrefix;
        }
    }

    public class SmoothTerrainConfig
    {
        /// <summary>Master switch for the automatic smoothing pass and the command.</summary>
        public bool Enabled = true;

        /// <summary>Radius in blocks around each player that is kept smoothed.</summary>
        public int Radius = 24;

        /// <summary>Columns inspected per player per tick. Keeps the pass cheap.</summary>
        public int ColumnsPerTick = 384;

        /// <summary>
        /// Columns of a freshly loaded chunk that are converted per tick. A whole chunk column
        /// is 1024 blocks, so the default smooths two newly loaded chunks per pass.
        /// </summary>
        public int ChunkColumnsPerTick = 2048;

        /// <summary>Interval of the smoothing pass in milliseconds.</summary>
        public int TickIntervalMs = 500;

        /// <summary>
        /// Shorelines are left alone by default: a curved block next to a water cell makes
        /// water behave badly (the same problem every partial-height terrain mod runs into).
        /// </summary>
        public bool SkipLiquidCoveredSurfaces = true;

        /// <summary>
        /// Only one edge per block is curved. When a block has several lower neighbours the
        /// fillet is oriented towards the first one found, in north, east, south, west order.
        /// </summary>
        public FilletFamilyConfig[] Families = new FilletFamilyConfig[]
        {
            // Every natural walkable terrain block. Longer, more specific prefixes come first
            // so that "packedglacierice" is never mistaken for "glacierice".
            new FilletFamilyConfig("packedglacierice", "fillet-packedglacierice") { ExactMatch = true },
            new FilletFamilyConfig("dirtygravel", "fillet-dirtygravel"),
            new FilletFamilyConfig("muddygravel", "fillet-muddygravel") { ExactMatch = true },
            new FilletFamilyConfig("sludgygravel", "fillet-sludgygravel") { ExactMatch = true },
            new FilletFamilyConfig("glacierice", "fillet-glacierice") { ExactMatch = true },
            new FilletFamilyConfig("bonysoil", "fillet-bonysoil") { ExactMatch = true },
            new FilletFamilyConfig("rawclay", "fillet-rawclay"),
            new FilletFamilyConfig("snowblock", "fillet-snowblock") { ExactMatch = true },
            new FilletFamilyConfig("lakeice", "fillet-lakeice") { ExactMatch = true },
            new FilletFamilyConfig("gravel", "fillet-gravel"),
            new FilletFamilyConfig("peat", "fillet-peat"),
            new FilletFamilyConfig("soil", "fillet-soil"),
            new FilletFamilyConfig("sand", "fillet-sand"),
            new FilletFamilyConfig("rock", "fillet-rock"),
            new FilletFamilyConfig("cob", "fillet-cob")
        };
    }
}
