namespace SmoothTerrain
{
    /// <summary>Horizontal direction a fillet slope descends towards.</summary>
    public enum FilletSide
    {
        North = 0,
        East = 1,
        South = 2,
        West = 3
    }

    /// <summary>
    /// The profile of a terrain fillet, i.e. the curved outer edge of a terrain block.
    ///
    /// The curve is a cubic Bezier in the vertical plane with control points
    ///
    ///     P0 = (0, 0)   P1 = (1/3, 0)   P2 = (2/3, 1)   P3 = (1, 1)
    ///
    /// For this control polygon the x component evaluates to x(t) = t exactly, so the curve
    /// is a height function and its height at normalised distance u from the low edge is the
    /// classic smoothstep
    ///
    ///     h(u) = 3u^2 - 2u^3
    ///
    /// Both inner control points sit on an edge (y = 0 and y = 1), which makes the tangent
    /// horizontal at u = 0 and at u = 1. That is what makes a fillet meet flat ground on both
    /// sides without a crease: no lip at the bottom to trip on, no ridge at the top.
    ///
    /// This file has no game dependencies on purpose. It is compiled into both the mod and
    /// tools/FilletMeshGen, so the rendered mesh and the collision boxes are sampled from
    /// literally the same function.
    /// </summary>
    public static class BezierProfile
    {
        /// <summary>Number of slices the curve is quantised into for collision boxes.</summary>
        public const int Slices = 16;

        /// <summary>Horizontal inset of the two inner control points, in block widths.</summary>
        public const double TangentInset = 1.0 / 3.0;

        /// <summary>Curve height at normalised distance u from the low edge.</summary>
        public static double Height(double u)
        {
            if (u <= 0) return 0;
            if (u >= 1) return 1;
            return 3 * u * u - 2 * u * u * u;
        }

        /// <summary>Curve slope dh/du at normalised distance u from the low edge.</summary>
        public static double Slope(double u)
        {
            if (u <= 0 || u >= 1) return 0;
            return 6 * u * (1 - u);
        }

        public static string ToCode(FilletSide side)
        {
            switch (side)
            {
                case FilletSide.North: return "north";
                case FilletSide.East: return "east";
                case FilletSide.South: return "south";
                default: return "west";
            }
        }

        public static bool TryParse(string code, out FilletSide side)
        {
            switch (code)
            {
                case "north": side = FilletSide.North; return true;
                case "east": side = FilletSide.East; return true;
                case "south": side = FilletSide.South; return true;
                case "west": side = FilletSide.West; return true;
                default: side = FilletSide.North; return false;
            }
        }
    }
}
