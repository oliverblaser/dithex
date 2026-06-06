package ch.oliverblaser.dithex

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HexDitherTest {
    @Test
    fun buildsTightlyPackedStaggeredGrid() {
        val frame = HexDither.process(solid(16, 9, 0x808080ff.toInt()), DitherSettings(columns = 8))
        assertEquals(8, frame.columns)
        assertEquals(5, frame.rows)
        assertEquals(2f, frame.centersX[1] - frame.centersX[0])
        assertEquals(1f, frame.centersX[8] - frame.centersX[0])
        assertTrue(abs(frame.centersY[8] - frame.centersY[0] - HexFrame.SQRT_3) < .0001f)
    }

    @Test
    fun quantizesSolidExtremesDeterministically() {
        val black = HexDither.process(solid(2, 2, 0x000000ff), DitherSettings(columns = 8))
        val white = HexDither.process(solid(2, 2, 0xffffffff.toInt()), DitherSettings(columns = 8))
        assertTrue(black.tones.all { it == 0.toByte() })
        assertTrue(white.tones.all { it == 2.toByte() })
    }

    @Test
    fun adjustableGrayChangesMiddleToneThreshold() {
        val middle = solid(2, 2, 0x666666ff)
        val darkGray = HexDither.process(middle, DitherSettings(columns = 8, grayLevel = .3f))
        val lightGray = HexDither.process(middle, DitherSettings(columns = 8, grayLevel = .8f))
        assertTrue(darkGray.tones.any { it == 1.toByte() })
        assertTrue(lightGray.tones.any { it == 0.toByte() })
    }

    @Test
    fun bilinearSamplingBlendsCorners() {
        val source = SourceImage("fixture", 2, 1, intArrayOf(0x000000ff, 0xffffffff.toInt()))
        assertTrue(abs(HexDither.sampleLuminance(source, .5f, .5f) - .5f) < .001f)
    }

    @Test
    fun detailEnhancementRaisesLocalHighlightsAndDeepensShadows() {
        assertTrue(HexDither.enhanceDetail(.7f, .5f, 1f) > .7f)
        assertTrue(HexDither.enhanceDetail(.3f, .5f, 1f) < .3f)
        assertEquals(.7f, HexDither.enhanceDetail(.7f, .5f, 0f))
    }

    @Test
    fun modesProduceDistinctDeterministicPatterns() {
        val gradient = SourceImage(
            "gradient",
            16,
            1,
            IntArray(16) { index ->
                val value = index * 17
                (value shl 24) or (value shl 16) or (value shl 8) or 0xff
            },
        )
        val blue = HexDither.process(gradient, DitherSettings(columns = 32, mode = DitherMode.BLUE_NOISE))
        val ordered = HexDither.process(gradient, DitherSettings(columns = 32, mode = DitherMode.ORDERED_HEX))
        assertTrue(!blue.tones.contentEquals(ordered.tones))
        assertTrue(blue.tones.contentEquals(HexDither.process(gradient, DitherSettings(columns = 32, mode = DitherMode.BLUE_NOISE)).tones))
    }

    private fun solid(width: Int, height: Int, color: Int) =
        SourceImage("solid", width, height, IntArray(width * height) { color })
}
