package com.acite.katahana.sgf

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class JvmSgfFiles : SgfFiles {
    override suspend fun open(): String? = suspendCancellableCoroutine { cont ->
        SwingUtilities.invokeLater {
            try {
                val dlg = FileDialog(null as Frame?, "Open SGF", FileDialog.LOAD)
                dlg.filenameFilter = FilenameFilter { _, name -> name.endsWith(".sgf", ignoreCase = true) }
                dlg.isVisible = true
                val file = dlg.file
                val dir = dlg.directory
                if (file == null || dir == null) {
                    cont.resume(null)
                } else {
                    cont.resume(File(dir, file).readText())
                }
            } catch (_: Exception) {
                cont.resume(null)
            }
        }
    }

    override suspend fun save(suggestedName: String, content: String): Boolean =
        suspendCancellableCoroutine { cont ->
            SwingUtilities.invokeLater {
                try {
                    val dlg = FileDialog(null as Frame?, "Save SGF", FileDialog.SAVE)
                    dlg.file = suggestedName
                    dlg.isVisible = true
                    val file = dlg.file
                    val dir = dlg.directory
                    if (file == null || dir == null) {
                        cont.resume(false)
                    } else {
                        val name = if (file.endsWith(".sgf", ignoreCase = true)) file else "$file.sgf"
                        File(dir, name).writeText(content)
                        cont.resume(true)
                    }
                } catch (_: Exception) {
                    cont.resume(false)
                }
            }
        }
}
