package com.acite.katahana.sgf

import androidx.compose.runtime.staticCompositionLocalOf

interface SgfFiles {
    suspend fun open(): String?
    suspend fun save(suggestedName: String, content: String): Boolean
}

object NoOpSgfFiles : SgfFiles {
    override suspend fun open(): String? = null
    override suspend fun save(suggestedName: String, content: String): Boolean = false
}

val LocalSgfFiles = staticCompositionLocalOf<SgfFiles> { NoOpSgfFiles }
