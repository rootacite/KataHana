package com.acite.katahana.recents

import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.settings.appDir
import com.acite.katahana.sgf.parseSgf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoadedRecent(
    val record: RecentGame,
    val config: GameConfig,
    val tree: GameTree,
)

@SingleIn(AppScope::class)
class RecentGamesRepository internal constructor(
    private val store: TextStore,
) {
    @Inject
    constructor() : this(FileTextStore("${appDir()}/recent-games.json"))

    private val mutex = Mutex()
    private val _games = MutableStateFlow(readDisk())
    val games: StateFlow<List<RecentGame>> = _games.asStateFlow()

    suspend fun upsert(game: RecentGame) {
        mutex.withLock {
            persist(RecentGamesIndex.upsert(_games.value, game))
        }
    }

    suspend fun remove(id: String) {
        mutex.withLock {
            persist(RecentGamesIndex.remove(_games.value, id))
        }
    }

    fun open(id: String): LoadedRecent? {
        val record = _games.value.find { it.id == id } ?: return null
        val parsed = runCatching { parseSgf(record.sgf) }.getOrNull() ?: return null
        parsed.tree.applyChildPath(record.currentPath)
        return LoadedRecent(record, record.toConfig(), parsed.tree)
    }

    private fun persist(games: List<RecentGame>) {
        store.write(RecentGamesIndex.encode(games))
        _games.value = games
    }

    private fun readDisk(): List<RecentGame> {
        val text = store.read() ?: return emptyList()
        return runCatching { RecentGamesIndex.decode(text) }.getOrDefault(emptyList())
    }
}

internal interface TextStore {
    fun read(): String?
    fun write(text: String)
}

internal class FileTextStore(private val path: String) : TextStore {
    override fun read(): String? = readUtf8(path)
    override fun write(text: String) = writeUtf8(path, text)
}

internal class MemoryTextStore(initial: String? = null) : TextStore {
    private var text: String? = initial
    override fun read(): String? = text
    override fun write(text: String) {
        this.text = text
    }
}
