package de.belegkasten.feature.capture

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.io.File

@Composable
internal fun CameraDialog(onDismiss: () -> Unit, onCaptured: (Uri) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) try {
                provider = future.get()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                ready = true
            } catch (_: Exception) { onError("Kamera nicht verfügbar. Bitte Datei importieren.") }
        }, ContextCompat.getMainExecutor(context))
        onDispose { disposed = true; provider?.unbind(preview, capture) }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Beleg vollständig und scharf ausrichten")
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(360.dp))
                Button(enabled = ready && !busy, onClick = {
                    busy = true
                    val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
                    val file = File.createTempFile("camera-", ".jpg", dir)
                    capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                onCaptured(FileProvider.getUriForFile(context, context.packageName + ".files", file))
                            }
                            override fun onError(exception: ImageCaptureException) { file.delete(); onError("Aufnahme fehlgeschlagen. Bitte erneut versuchen.") }
                        })
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Aufnahme …" else "Aufnehmen") }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    }
}
