namespace SmoothTerrain
{
    /// <summary>
    /// Per-player mirror of the world switch. The server owns the authoritative state;
    /// this file only lets the Graphics tab show the last choice the player made.
    /// </summary>
    public class SmoothTerrainClientConfig
    {
        public bool Enabled = true;
    }
}
