package ch.oliverblaser.dithex

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import web.canvas.CanvasRenderingContext2D
import web.canvas.ID
import web.dom.document
import web.events.EventHandler
import web.html.HtmlTagName
import web.html.InputType
import web.html.file
import web.url.URL.Companion.createObjectURL
import web.url.URL.Companion.revokeObjectURL

// Kotlin/Wasm currently schedules Default cooperatively. The pure processor is ready to
// move behind a Worker without changing app state or rendering contracts.
actual val platformComputeDispatcher: CoroutineDispatcher = Dispatchers.Default

@Composable
actual fun PlatformImagePicker(onImageLoaded: (SourceImage) -> Unit, onError: (String) -> Unit) {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffd4ff45), contentColor = Color(0xff10110f)),
        onClick = {
            val input = document.createElement(HtmlTagName.input)
            input.type = InputType.file
            input.accept = "image/png,image/jpeg,image/webp"
            input.onchange = EventHandler {
                val file = input.files?.item(0)
                if (file != null) decodeBrowserImage(file.name, createObjectURL(file), onImageLoaded, onError)
            }
            input.click()
        },
    ) {
        Text("LOAD IMAGE")
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun decodeBrowserImage(
    name: String,
    objectUrl: String,
    onImageLoaded: (SourceImage) -> Unit,
    onError: (String) -> Unit,
) {
    val image = document.createElement(HtmlTagName.img)
    image.onload = EventHandler {
        val width = image.naturalWidth
        val height = image.naturalHeight
        val canvas = document.createElement(HtmlTagName.canvas)
        canvas.width = width
        canvas.height = height
        val context = canvas.getContext(CanvasRenderingContext2D.ID)
        if (context != null) {
            context.drawImage(image, 0.0, 0.0)
            val bytes = context.getImageData(0, 0, width, height).data
            val pixels = IntArray(width * height)
            var pixel = 0
            var byte = 0
            while (pixel < pixels.size) {
                val red: Int = bytes[byte].toInt()
                val green: Int = bytes[byte + 1].toInt()
                val blue: Int = bytes[byte + 2].toInt()
                val alpha: Int = bytes[byte + 3].toInt()
                pixels[pixel] = (red shl 24) or (green shl 16) or (blue shl 8) or alpha
                pixel++
                byte += 4
            }
            revokeObjectURL(objectUrl)
            onImageLoaded(SourceImage(name, width, height, pixels))
        }
    }
    image.src = objectUrl
}
