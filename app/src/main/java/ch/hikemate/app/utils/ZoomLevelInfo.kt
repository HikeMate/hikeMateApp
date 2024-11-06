package ch.hikemate.app.utils

data class ZoomLevelInfo(
    val level: Int,
    val tileWidthInDegrees: Double, // Width of a tile in degrees of longitude
    val metersPerPixel: Double, // Meters per pixel at the equator
)

val ZOOM_LEVELS =
    listOf(
        ZoomLevelInfo(0, 360.0, 156543.0),
        ZoomLevelInfo(1, 180.0, 78272.0),
        ZoomLevelInfo(2, 90.0, 39136.0),
        ZoomLevelInfo(3, 45.0, 19568.0),
        ZoomLevelInfo(4, 22.5, 9784.0),
        ZoomLevelInfo(5, 11.25, 4892.0),
        ZoomLevelInfo(6, 5.625, 2446.0),
        ZoomLevelInfo(7, 2.813, 1223.0),
        ZoomLevelInfo(8, 1.406, 611.496),
        ZoomLevelInfo(9, 0.703, 305.748),
        ZoomLevelInfo(10, 0.352, 152.874),
        ZoomLevelInfo(11, 0.176, 76.437),
        ZoomLevelInfo(12, 0.088, 38.219),
        ZoomLevelInfo(13, 0.044, 19.109),
        ZoomLevelInfo(14, 0.022, 9.555),
        ZoomLevelInfo(15, 0.011, 4.777),
        ZoomLevelInfo(16, 0.005, 2.389),
        ZoomLevelInfo(17, 0.003, 1.194),
        ZoomLevelInfo(18, 0.001, 0.597),
        ZoomLevelInfo(19, 0.0005, 0.299),
        ZoomLevelInfo(20, 0.00025, 0.149))
