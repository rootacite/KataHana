package com.acite.katahana.sgf

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidSgfFiles(private val activity: ComponentActivity) : SgfFiles {
    private var openCont: ((String?) -> Unit)? = null
    private var saveCont: ((Boolean) -> Unit)? = null
    private var pendingSave: String? = null

    private val openLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val text = uri?.let { read(it) }
        openCont?.invoke(text)
        openCont = null
    }

    private val saveLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-go-sgf"),
    ) { uri: Uri? ->
        val body = pendingSave
        pendingSave = null
        val ok = if (uri != null && body != null) write(uri, body) else false
        saveCont?.invoke(ok)
        saveCont = null
    }

    override suspend fun open(): String? = suspendCancellableCoroutine { cont ->
        openCont = { cont.resume(it) }
        openLauncher.launch(arrayOf("application/x-go-sgf", "text/plain", "*/*"))
    }

    override suspend fun save(suggestedName: String, content: String): Boolean =
        suspendCancellableCoroutine { cont ->
            pendingSave = content
            saveCont = { cont.resume(it) }
            saveLauncher.launch(suggestedName)
        }

    private fun read(uri: Uri): String? = try {
        activity.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
    } catch (_: Exception) {
        null
    }

    private fun write(uri: Uri, content: String): Boolean = try {
        activity.contentResolver.openOutputStream(uri)?.use { out ->
            out.writer().use { it.write(content) }
        }
        true
    } catch (_: Exception) {
        false
    }
}
