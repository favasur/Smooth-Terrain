using System;
using HarmonyLib;
using Vintagestory.API.Client;
using Vintagestory.API.Common;

namespace SmoothTerrain
{
    /// <summary>
    /// Adds the on/off switch to the vanilla Graphics settings tab.
    ///
    /// Vintage Story has no public API for adding a mod setting to a vanilla settings page,
    /// so the Graphics tab composer is patched after it has been built. The switch is placed
    /// underneath the last control of that tab and the composer is rebuilt. The patch is
    /// fully guarded: if the game changes its settings layout the switch is skipped and the
    /// settings screen keeps working.
    /// </summary>
    public class SmoothTerrainClientModSystem : ModSystem
    {
        public const string ClientConfigFileName = "smoothterrain-client.json";
        private const string HarmonyId = "smoothterrain.settings";
        private const string SwitchKey = "smoothTerrainSwitch";
        private const string SettingsTypeName = "Vintagestory.Client.NoObf.GuiCompositeSettings";

        private static SmoothTerrainClientModSystem instance;

        private ICoreClientAPI capi;
        private SmoothTerrainClientConfig config;
        private Harmony harmony;

        public override void StartClientSide(ICoreClientAPI api)
        {
            base.StartClientSide(api);

            capi = api;
            instance = this;

            config = api.LoadModConfig<SmoothTerrainClientConfig>(ClientConfigFileName);
            if (config == null)
            {
                config = new SmoothTerrainClientConfig();
                api.StoreModConfig(config, ClientConfigFileName);
            }

            try
            {
                Type composite = AccessTools.TypeByName(SettingsTypeName);
                if (composite == null)
                {
                    api.Logger.Warning("[smoothterrain] {0} not found, Graphics switch disabled", SettingsTypeName);
                    return;
                }

                System.Reflection.MethodInfo target = AccessTools.Method(composite, "OnGraphicsOptions", new[] { typeof(bool) });
                if (target == null)
                {
                    api.Logger.Warning("[smoothterrain] Graphics tab method not found, Graphics switch disabled");
                    return;
                }

                harmony = new Harmony(HarmonyId);
                harmony.Patch(target, postfix: new HarmonyMethod(
                    AccessTools.Method(typeof(SmoothTerrainClientModSystem), nameof(AddGraphicsSwitch))));

                api.Logger.Notification("[smoothterrain] Graphics settings switch installed");
            }
            catch (Exception exception)
            {
                api.Logger.Error("[smoothterrain] could not install the Graphics settings switch: {0}", exception);
            }
        }

        public override void Dispose()
        {
            try
            {
                harmony?.UnpatchAll(HarmonyId);
            }
            catch
            {
                // Unpatching at shutdown is best effort only.
            }

            if (ReferenceEquals(instance, this)) instance = null;
            base.Dispose();
        }

        private static void AddGraphicsSwitch(object __instance)
        {
            SmoothTerrainClientModSystem system = instance;
            if (system == null || __instance == null) return;

            try
            {
                system.InjectSwitch(__instance);
            }
            catch (Exception exception)
            {
                system.capi?.Logger.Warning("[smoothterrain] could not add the Graphics switch: {0}", exception.Message);
            }
        }

        private void InjectSwitch(object composite)
        {
            GuiComposer composer = AccessTools.Field(composite.GetType(), "composer")?.GetValue(composite) as GuiComposer;
            if (composer == null) return;

            // The tab is rebuilt from scratch every time it is opened, so the switch is only
            // absent on the first pass. Guard against a duplicate key anyway.
            if (composer.GetSwitch(SwitchKey) != null) return;

            ElementBounds last = composer.LastAddedElementBounds;
            double y = last != null ? last.fixedY + last.fixedHeight + 16.0 : 500.0;

            composer.AddStaticText("Smooth terrain", CairoFont.WhiteSmallishText(),
                ElementBounds.Fixed(470.0, y + 4.0, 225.0, 25.0));
            composer.AddHoverText(
                "Curves terrain edges. Turning it off restores the vanilla blocks around you.",
                CairoFont.WhiteSmallText(), 260,
                ElementBounds.Fixed(470.0, y + 4.0, 225.0, 25.0));
            composer.AddSwitch(OnToggle, ElementBounds.Fixed(705.0, y, 160.0, 20.0).WithFixedPadding(10.0, 2.0), SwitchKey);

            composer.GetSwitch(SwitchKey)?.SetValue(config.Enabled);
            composer.ReCompose();
        }

        private void OnToggle(bool enabled)
        {
            if (config.Enabled == enabled) return;

            config.Enabled = enabled;
            capi.StoreModConfig(config, ClientConfigFileName);

            // The world is server-authoritative (and multiplayer settings are shared), so the
            // switch asks the server rather than changing client rendering.
            capi.SendChatMessage("/smoothterrain " + (enabled ? "on" : "off"));
        }
    }
}
