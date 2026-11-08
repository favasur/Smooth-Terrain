using Vintagestory.API.Common;
using Vintagestory.API.MathTools;

namespace SmoothTerrain
{
    /// <summary>
    /// A terrain block whose outer edge is curved instead of square.
    ///
    /// The block occupies a full cell except for the wedge that the Bezier profile removes
    /// at the named side. Its render shape is the OBJ mesh generated from the same curve,
    /// and its collision and selection boxes come from <see cref="BezierProfile.CollisionBoxes"/>
    /// so physical geometry and visuals cannot drift apart.
    /// </summary>
    public class BlockTerrainFillet : Block
    {
        private Cuboidf[] filletBoxes;

        public override void OnLoaded(ICoreAPI api)
        {
            base.OnLoaded(api);

            FilletSide side = FilletSide.North;
            string sideCode;
            if (Variant != null && Variant.TryGetValue("side", out sideCode))
            {
                BezierProfile.TryParse(sideCode, out side);
            }

            filletBoxes = FilletCollision.Boxes(side);
        }

        public override Cuboidf[] GetCollisionBoxes(IBlockAccessor blockAccessor, BlockPos pos)
        {
            return filletBoxes ?? base.GetCollisionBoxes(blockAccessor, pos);
        }

        public override Cuboidf[] GetSelectionBoxes(IBlockAccessor blockAccessor, BlockPos pos)
        {
            return filletBoxes ?? base.GetSelectionBoxes(blockAccessor, pos);
        }
    }
}
