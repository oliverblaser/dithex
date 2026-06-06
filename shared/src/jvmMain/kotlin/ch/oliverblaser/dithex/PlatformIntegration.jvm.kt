package ch.oliverblaser.dithex

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual val platformComputeDispatcher: CoroutineDispatcher = Dispatchers.Default

@Composable
actual fun PlatformImagePicker(onImageLoaded: (SourceImage) -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffd4ff45), contentColor = Color(0xff10110f)),
        onClick = {
            val dialog = FileDialog(null as Frame?, "Load image", FileDialog.LOAD).apply {
                setFilenameFilter { _, name ->
                    name.endsWith(".png", true) || name.endsWith(".jpg", true) ||
                        name.endsWith(".jpeg", true) || name.endsWith(".webp", true)
                }
                isVisible = true
            }
            val selected = dialog.file?.let { File(dialog.directory, it) } ?: return@Button
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { decode(selected) } }
                    .onSuccess(onImageLoaded)
                    .onFailure { onError(it.message ?: "Could not decode image") }
            }
        },
    ) {
        Text("LOAD IMAGE")
    }
}

private fun decode(file: File): SourceImage {
    val image = Image.makeFromEncoded(file.readBytes())
    val bitmap = Bitmap.makeFromImage(image)
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = bitmap.getColor(x, y)
            pixels[index++] = (argb shl 8) or (argb ushr 24 and 0xff)
        }
    }
    bitmap.close()
    image.close()
    return SourceImage(file.name, width, height, pixels)
}
