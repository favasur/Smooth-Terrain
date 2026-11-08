using Vintagestory.API.MathTools;

namespace SmoothTerrain
{
    /// <summary>
    /// Turns the Bezier profile into physical geometry.
    ///
    /// The solid part of a fillet is everything below the curve, approximated by
    /// <see cref="BezierProfile.Slices"/> thin boxes whose top follows the curve at the middle
    /// of each slice. Maximum deviation from the drawn surface is 1/(2*Slices) of a block,
    /// i.e. 1/32 with the default 16 slices, and each individual step is far below the
    /// player auto-step height, so a curved edge is walked, never jumped.
    ///
    /// The arrays are cached per direction and never mutated, because GetCollisionBoxes may be
    /// called from the physics thread.
    /// </summary>
    public static class FilletCollision
    {
        private static readonly Cuboidf[][] collisionBoxes = new Cuboidf[4][];

        /// <summary>Collision and selection boxes for one fillet, in block-relative 0..1 coordinates.</summary>
        public static Cuboidf[] Boxes(FilletSide side)
        {
            int index = (int)side;
            Cuboidf[] cached = collisionBoxes[index];
            if (cached != null) return cached;

            Cuboidf[] boxes = new Cuboidf[BezierProfile.Slices];
            for (int i = 0; i < BezierProfile.Slices; i++)
            {
                float u0 = i / (float)BezierProfile.Slices;
                float u1 = (i + 1) / (float)BezierProfile.Slices;
                float mid = (u0 + u1) * 0.5f;

                // The block is full height at the side opposite to its name; it falls away
                // towards the named side.
                bool highAtLowEdge = side == FilletSide.North || side == FilletSide.West;
                float height = (float)BezierProfile.Height(highAtLowEdge ? 1 - mid : mid);

                float x0 = 0, x1 = 1, z0 = 0, z1 = 1;
                switch (side)
                {
                    case FilletSide.North:
                        z0 = u0; z1 = u1;
                        break;
                    case FilletSide.South:
                        z0 = 1 - u1; z1 = 1 - u0;
                        break;
                    case FilletSide.West:
                        x0 = u0; x1 = u1;
                        break;
                    default:
                        x0 = 1 - u1; x1 = 1 - u0;
                        break;
                }

                boxes[i] = new Cuboidf(x0, 0, z0, x1, height, z1);
            }

            collisionBoxes[index] = boxes;
            return boxes;
        }
    }
}
