using System;

namespace SmoothTerrain
{
    /// <summary>
    /// Pure code-to-code mapping logic, kept free of world access so it can be reasoned
    /// about (and exercised) without a running game.
    /// </summary>
    public static class TerrainSmoother
    {
        public const string TargetDomain = "smoothterrain";

        /// <summary>
        /// Returns the fillet block code for a vanilla terrain block, or null when the block
        /// does not belong to a configured family. The vanilla variant suffix is preserved
        /// verbatim, which is why a fillet block keeps the material of the block it replaces.
        /// </summary>
        public static string MapToFilletCode(string sourceDomain, string sourcePath, FilletSide side, SmoothTerrainConfig config)
        {
            if (sourceDomain != "game" || string.IsNullOrEmpty(sourcePath) || config?.Families == null)
            {
                return null;
            }

            for (int i = 0; i < config.Families.Length; i++)
            {
                FilletFamilyConfig family = config.Families[i];
                if (family == null || !family.Enabled || string.IsNullOrEmpty(family.SourceCodePrefix))
                {
                    continue;
                }

                if (!MatchesFamily(sourcePath, family.SourceCodePrefix, family.ExactMatch))
                {
                    continue;
                }

                string variantSuffix = sourcePath.Substring(family.SourceCodePrefix.Length);
                return TargetDomain + ":" + family.TargetCodePrefix + variantSuffix + "-" + BezierProfile.ToCode(side);
            }

            return null;
        }

        /// <summary>
        /// Reverse of <see cref="MapToFilletCode"/>: turns a placed fillet block back into the
        /// vanilla block it replaced. Used when the player switches smoothing off, so turning
        /// it back off leaves the same terrain that was there before.
        /// </summary>
        public static string MapToVanillaCode(string domain, string path, SmoothTerrainConfig config)
        {
            if (domain != TargetDomain || string.IsNullOrEmpty(path) || config?.Families == null)
            {
                return null;
            }

            int sideIndex = path.LastIndexOf('-');
            if (sideIndex <= 0 || sideIndex == path.Length - 1)
            {
                return null;
            }

            FilletSide side;
            if (!BezierProfile.TryParse(path.Substring(sideIndex + 1), out side))
            {
                return null;
            }

            string basePath = path.Substring(0, sideIndex);
            for (int i = 0; i < config.Families.Length; i++)
            {
                FilletFamilyConfig family = config.Families[i];
                if (family == null || string.IsNullOrEmpty(family.TargetCodePrefix))
                {
                    continue;
                }

                if (!MatchesFamily(basePath, family.TargetCodePrefix, family.ExactMatch))
                {
                    continue;
                }

                string variantSuffix = basePath.Substring(family.TargetCodePrefix.Length);
                return "game:" + family.SourceCodePrefix + variantSuffix;
            }

            return null;
        }

        private static bool MatchesFamily(string path, string prefix, bool exactMatch)
        {
            if (path == prefix) return true;
            return !exactMatch && path.StartsWith(prefix + "-", StringComparison.Ordinal);
        }
    }
}
