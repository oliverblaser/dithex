package ch.oliverblaser.dithex

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DithexUiState(
    val source: SourceImage? = null,
    val sourceRevision: Long = 0,
    val settings: DitherSettings = DitherSettings(),
    val frame: HexFrame? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
)

class DithexState(
    private val computeDispatcher: CoroutineDispatcher = platformComputeDispatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val mutableUiState = MutableStateFlow(DithexUiState())
    val uiState: StateFlow<DithexUiState> = mutableUiState.asStateFlow()
    private var processingJob: Job? = null
    private var generation = 0L

    fun load(source: SourceImage) {
        mutableUiState.value = mutableUiState.value.copy(
            source = source,
            sourceRevision = mutableUiState.value.sourceRevision + 1,
            error = null,
        )
        scheduleProcessing(0)
    }

    fun updateSettings(settings: DitherSettings) {
        mutableUiState.value = mutableUiState.value.copy(settings = settings.normalized(), error = null)
        scheduleProcessing(120)
    }

    fun setError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(isProcessing = false, error = message)
    }

    private fun scheduleProcessing(debounceMillis: Long) {
        val source = mutableUiState.value.source ?: return
        val settings = mutableUiState.value.settings
        val expectedGeneration = ++generation
        processingJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(isProcessing = true)
        processingJob = scope.launch {
            delay(debounceMillis)
            try {
                val frame = withContext(computeDispatcher) { HexDither.process(source, settings) }
                if (expectedGeneration == generation) {
                    mutableUiState.value = mutableUiState.value.copy(frame = frame, isProcessing = false)
                }
            } catch (error: Throwable) {
                if (expectedGeneration == generation) {
                    mutableUiState.value = mutableUiState.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Could not process image",
                    )
                }
            }
        }
    }
}

expect val platformComputeDispatcher: CoroutineDispatcher

@Composable
expect fun PlatformImagePicker(onImageLoaded: (SourceImage) -> Unit, onError: (String) -> Unit)
