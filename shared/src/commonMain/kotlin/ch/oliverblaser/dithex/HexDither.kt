package ch.oliverblaser.dithex

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class DitherMode(val label: String) {
    FLOYD_STEINBERG("FLOYD"),
    BLUE_NOISE("BLUE NOISE"),
    ORDERED_HEX("ORDERED HEX"),
}

data class SourceImage(
    val name: String,
    val width: Int,
    val height: Int,
    val rgba: IntArray,
) {
    init {
        require(width > 0 && height > 0)
        require(rgba.size == width * height)
    }
}

data class DitherSettings(
    val columns: Int = 80,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val grayLevel: Float = .5f,
    val detail: Float = 0f,
    val mode: DitherMode = DitherMode.FLOYD_STEINBERG,
) {
    fun normalized() = copy(
        columns = columns.coerceIn(8, 400),
        brightness = brightness.coerceIn(-1f, 1f),
        contrast = contrast.coerceIn(.2f, 3f),
        grayLevel = grayLevel.coerceIn(.1f, .9f),
        detail = detail.coerceIn(0f, 2.5f),
    )
}

data class HexFrame(
    val columns: Int,
    val rows: Int,
    val centersX: FloatArray,
    val centersY: FloatArray,
    val tones: ByteArray,
) {
    val size: Int get() = tones.size
    val width: Float get() = columns * 2f + 1f
    val height: Float get() = if (rows == 0) 0f else 2f + (rows - 1) * SQRT_3

    companion object {
        const val SQRT_3 = 1.7320508f
    }
}

object HexDither {
    fun process(source: SourceImage, settings: DitherSettings): HexFrame {
        val normalized = settings.normalized()
        val columns = normalized.columns
        val rows = max(1, (source.height.toFloat() / source.width * columns * 2f / sqrt(3f)).roundToInt())
        val count = columns * rows
        val centersX = FloatArray(count)
        val centersY = FloatArray(count)
        val values = FloatArray(count)

        var index = 0
        for (row in 0 until rows) {
            val offset = if (row and 1 == 1) 1f else 0f
            for (column in 0 until columns) {
                centersX[index] = 1f + column * 2f + offset
                centersY[index] = 1f + row * HexFrame.SQRT_3
                val u = (column + .5f) / columns
                val v = (row + .5f) / rows
                values[index] = adjust(sampleDetailedLuminance(source, u, v, columns, rows, normalized.detail), normalized)
                index++
            }
        }

        val tones = ByteArray(count)
        when (normalized.mode) {
            DitherMode.FLOYD_STEINBERG -> {
                for (row in 0 until rows) {
                    if (row and 1 == 0) {
                        for (column in 0 until columns) {
                            diffuse(values, tones, columns, rows, row, column, normalized.grayLevel, false)
                        }
                    } else {
                        for (column in columns - 1 downTo 0) {
                            diffuse(values, tones, columns, rows, row, column, normalized.grayLevel, true)
                        }
                    }
                }
            }
            DitherMode.BLUE_NOISE, DitherMode.ORDERED_HEX -> {
                for (row in 0 until rows) {
                    for (column in 0 until columns) {
                        val threshold = if (normalized.mode == DitherMode.BLUE_NOISE) {
                            blueNoiseThreshold(column, row)
                        } else {
                            orderedHexThreshold(column, row)
                        }
                        tones[row * columns + column] = thresholdTone(
                            values[row * columns + column],
                            normalized.grayLevel,
                            threshold,
                        )
                    }
                }
            }
        }
        return HexFrame(columns, rows, centersX, centersY, tones)
    }

    private fun sampleDetailedLuminance(
        source: SourceImage,
        u: Float,
        v: Float,
        columns: Int,
        rows: Int,
        detail: Float,
    ): Float {
        val center = sampleLuminance(source, u, v)
        if (detail == 0f) return center
        val du = .7f / columns
        val dv = .7f / rows
        val blur = (
            sampleLuminance(source, u - du, v) +
                sampleLuminance(source, u + du, v) +
                sampleLuminance(source, u - du * .5f, v - dv) +
                sampleLuminance(source, u + du * .5f, v - dv) +
                sampleLuminance(source, u - du * .5f, v + dv) +
                sampleLuminance(source, u + du * .5f, v + dv)
            ) / 6f
        return enhanceDetail(center, blur, detail)
    }

    internal fun enhanceDetail(center: Float, blur: Float, detail: Float): Float =
        (center + (center - blur) * detail).coerceIn(0f, 1f)

    private fun adjust(value: Float, settings: DitherSettings): Float =
        (((value - .5f) * settings.contrast) + .5f + settings.brightness).coerceIn(0f, 1f)

    private fun diffuse(
        values: FloatArray,
        tones: ByteArray,
        columns: Int,
        rows: Int,
        row: Int,
        column: Int,
        gray: Float,
        reverse: Boolean,
    ) {
        val index = row * columns + column
        val value = values[index].coerceIn(0f, 1f)
        val tone = nearestTone(value, gray)
        tones[index] = tone.first
        val error = value - tone.second
        val direction = if (reverse) -1 else 1
        add(values, columns, rows, row, column + direction, error * 7f / 16f)

        val rowShift = row and 1
        val nearColumn = if (reverse) column + rowShift else column - 1 + rowShift
        val farColumn = if (reverse) column - 1 + rowShift else column + rowShift
        add(values, columns, rows, row + 1, nearColumn, error * 5f / 16f)
        add(values, columns, rows, row + 1, farColumn, error * 4f / 16f)
    }

    private fun add(values: FloatArray, columns: Int, rows: Int, row: Int, column: Int, error: Float) {
        if (row in 0 until rows && column in 0 until columns) {
            values[row * columns + column] += error
        }
    }

    private fun nearestTone(value: Float, gray: Float): Pair<Byte, Float> {
        val blackDistance = value
        val grayDistance = kotlin.math.abs(value - gray)
        val whiteDistance = 1f - value
        return when {
            blackDistance <= grayDistance && blackDistance <= whiteDistance -> 0.toByte() to 0f
            grayDistance <= whiteDistance -> 1.toByte() to gray
            else -> 2.toByte() to 1f
        }
    }

    private fun thresholdTone(value: Float, gray: Float, threshold: Float): Byte {
        val clamped = value.coerceIn(0f, 1f)
        return if (clamped <= gray) {
            if (clamped / gray > threshold) 1 else 0
        } else {
            if ((clamped - gray) / (1f - gray) > threshold) 2 else 1
        }.toByte()
    }

    private fun orderedHexThreshold(column: Int, row: Int): Float {
        val shiftedColumn = column + if (row and 1 == 1) 2 else 0
        return ORDERED_HEX[(row and 3) * 4 + (shiftedColumn and 3)] / 16f
    }

    // Interleaved-gradient noise gives a stable, high-frequency distribution with little clustering.
    private fun blueNoiseThreshold(column: Int, row: Int): Float {
        val inner = fract(column * .06711056f + row * .00583715f)
        return fract(52.9829189f * inner)
    }

    private fun fract(value: Float): Float = value - floor(value)

    internal fun sampleLuminance(source: SourceImage, u: Float, v: Float): Float {
        val x = (u.coerceIn(0f, 1f) * (source.width - 1))
        val y = (v.coerceIn(0f, 1f) * (source.height - 1))
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = (x0 + 1).coerceAtMost(source.width - 1)
        val y1 = (y0 + 1).coerceAtMost(source.height - 1)
        val tx = x - x0
        val ty = y - y0
        val top = luminance(source.rgba[y0 * source.width + x0]) * (1f - tx) +
            luminance(source.rgba[y0 * source.width + x1]) * tx
        val bottom = luminance(source.rgba[y1 * source.width + x0]) * (1f - tx) +
            luminance(source.rgba[y1 * source.width + x1]) * tx
        return top * (1f - ty) + bottom * ty
    }

    private fun luminance(rgba: Int): Float {
        val red = (rgba ushr 24 and 0xff) / 255f
        val green = (rgba ushr 16 and 0xff) / 255f
        val blue = (rgba ushr 8 and 0xff) / 255f
        return red * .2126f + green * .7152f + blue * .0722f
    }

    private val ORDERED_HEX = intArrayOf(
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
    )
}
