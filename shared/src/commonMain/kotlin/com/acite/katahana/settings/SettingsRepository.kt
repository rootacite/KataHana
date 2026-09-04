package com.acite.katahana.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.engine.EngineProfile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

data class QualityThresholds(
    val blunder: Float = 12f,
    val bigMistake: Float = 6f,
    val mistake: Float = 3f,
    val inaccuracy: Float = 1.5f,
    val fair: Float = 0.5f,
)

@Inject
@SingleIn(AppScope::class)
class SettingsRepository {
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { settingsFilePath().toPath() },
        )

    val confirmMove: Flow<Boolean> = dataStore.data.map { it[Keys.CONFIRM_MOVE] ?: false }
    val showCoords: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_COORDS] ?: true }
    val showCandidates: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_CANDIDATES] ?: true }
    val showQuality: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_QUALITY] ?: true }
    val showConnections: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_CONNECTIONS] ?: false }
    val showOwnership: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_OWNERSHIP] ?: false }
    val showDeadStones: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_DEAD_STONES] ?: false }
    val ownershipStyle: Flow<OwnershipStyle> = dataStore.data.map {
        OwnershipStyle.fromId(it[Keys.OWNERSHIP_STYLE])
    }
    val appearanceId: Flow<String> = dataStore.data.map {
        it[Keys.APPEARANCE] ?: "sky_sakura"
    }
    val engineName: Flow<String> = dataStore.data.map { it[Keys.ENGINE_NAME] ?: "KataGo" }
    val engineUrl: Flow<String> = dataStore.data.map {
        it[Keys.ENGINE_URL] ?: EngineProfile.DEFAULT_URL
    }
    val engineToken: Flow<String> = dataStore.data.map { it[Keys.ENGINE_TOKEN] ?: "" }
    val playVisits: Flow<Int> = dataStore.data.map { it[Keys.PLAY_VISITS] ?: 400 }
    val reviewVisits: Flow<Int> = dataStore.data.map { it[Keys.REVIEW_VISITS] ?: 400 }
    val engineProfile: Flow<EngineProfile> = dataStore.data.map { prefs ->
        EngineProfile(
            name = prefs[Keys.ENGINE_NAME] ?: "KataGo",
            url = prefs[Keys.ENGINE_URL] ?: EngineProfile.DEFAULT_URL,
            token = prefs[Keys.ENGINE_TOKEN] ?: "",
            playVisits = prefs[Keys.PLAY_VISITS] ?: 400,
            reviewVisits = prefs[Keys.REVIEW_VISITS] ?: 400,
        )
    }
    val quality: Flow<QualityThresholds> = dataStore.data.map { prefs ->
        QualityThresholds(
            blunder = prefs[Keys.Q_BLUNDER] ?: 12f,
            bigMistake = prefs[Keys.Q_BIG] ?: 6f,
            mistake = prefs[Keys.Q_MISTAKE] ?: 3f,
            inaccuracy = prefs[Keys.Q_INACC] ?: 1.5f,
            fair = prefs[Keys.Q_FAIR] ?: 0.5f,
        )
    }
    val lastGame: Flow<GameConfig> = dataStore.data.map { prefs ->
        val size = prefs[Keys.LAST_SIZE] ?: 19
        GameConfig(
            boardSize = if (size == 9 || size == 13 || size == 19) size else 19,
            komi = prefs[Keys.LAST_KOMI] ?: 7.5f,
            mode = if (prefs[Keys.LAST_MODE] == "hvai") PlayMode.HumanVsAi else PlayMode.HumanVsHuman,
            rankKyu = (prefs[Keys.LAST_RANK] ?: 5).coerceIn(-2, 15),
            humanPlaysBlack = prefs[Keys.LAST_HUMAN_BLACK] ?: true,
            aiStyle = if (prefs[Keys.LAST_AI] == "full") AiStyle.Full else AiStyle.Rank,
        )
    }

    suspend fun setConfirmMove(value: Boolean) = edit { it[Keys.CONFIRM_MOVE] = value }
    suspend fun setShowCoords(value: Boolean) = edit { it[Keys.SHOW_COORDS] = value }
    suspend fun setShowCandidates(value: Boolean) = edit { it[Keys.SHOW_CANDIDATES] = value }
    suspend fun setShowQuality(value: Boolean) = edit { it[Keys.SHOW_QUALITY] = value }
    suspend fun setShowConnections(value: Boolean) = edit { it[Keys.SHOW_CONNECTIONS] = value }
    suspend fun setShowOwnership(value: Boolean) = edit { it[Keys.SHOW_OWNERSHIP] = value }
    suspend fun setShowDeadStones(value: Boolean) = edit { it[Keys.SHOW_DEAD_STONES] = value }
    suspend fun setOwnershipStyle(value: OwnershipStyle) = edit { it[Keys.OWNERSHIP_STYLE] = value.id }
    suspend fun setAppearanceId(value: String) = edit { it[Keys.APPEARANCE] = value }
    suspend fun setEngineName(value: String) = edit { it[Keys.ENGINE_NAME] = value }
    suspend fun setEngineUrl(value: String) = edit { it[Keys.ENGINE_URL] = value }
    suspend fun setEngineToken(value: String) = edit { it[Keys.ENGINE_TOKEN] = value }
    suspend fun setPlayVisits(value: Int) = edit { it[Keys.PLAY_VISITS] = value.coerceAtLeast(1) }
    suspend fun setReviewVisits(value: Int) = edit { it[Keys.REVIEW_VISITS] = value.coerceAtLeast(1) }
    suspend fun setLastGame(value: GameConfig) = edit {
        it[Keys.LAST_SIZE] = value.boardSize
        it[Keys.LAST_KOMI] = value.komi
        it[Keys.LAST_MODE] = if (value.mode == PlayMode.HumanVsAi) "hvai" else "hvh"
        it[Keys.LAST_RANK] = value.rankKyu
        it[Keys.LAST_HUMAN_BLACK] = value.humanPlaysBlack
        it[Keys.LAST_AI] = if (value.aiStyle == AiStyle.Full) "full" else "rank"
    }

    suspend fun setQuality(value: QualityThresholds) = edit {
        it[Keys.Q_BLUNDER] = value.blunder
        it[Keys.Q_BIG] = value.bigMistake
        it[Keys.Q_MISTAKE] = value.mistake
        it[Keys.Q_INACC] = value.inaccuracy
        it[Keys.Q_FAIR] = value.fair
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val CONFIRM_MOVE = booleanPreferencesKey("confirm_move")
        val SHOW_COORDS = booleanPreferencesKey("show_coords")
        val SHOW_CANDIDATES = booleanPreferencesKey("show_candidates")
        val SHOW_QUALITY = booleanPreferencesKey("show_quality")
        val SHOW_CONNECTIONS = booleanPreferencesKey("show_connections")
        val SHOW_OWNERSHIP = booleanPreferencesKey("show_ownership")
        val SHOW_DEAD_STONES = booleanPreferencesKey("show_dead_stones")
        val OWNERSHIP_STYLE = stringPreferencesKey("ownership_style")
        val APPEARANCE = stringPreferencesKey("appearance")
        val ENGINE_NAME = stringPreferencesKey("engine_name")
        val ENGINE_URL = stringPreferencesKey("engine_url")
        val ENGINE_TOKEN = stringPreferencesKey("engine_token")
        val PLAY_VISITS = intPreferencesKey("play_visits")
        val REVIEW_VISITS = intPreferencesKey("review_visits")
        val Q_BLUNDER = floatPreferencesKey("quality_blunder")
        val Q_BIG = floatPreferencesKey("quality_big")
        val Q_MISTAKE = floatPreferencesKey("quality_mistake")
        val Q_INACC = floatPreferencesKey("quality_inacc")
        val Q_FAIR = floatPreferencesKey("quality_fair")
        val LAST_SIZE = intPreferencesKey("last_board_size")
        val LAST_KOMI = floatPreferencesKey("last_komi")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val LAST_RANK = intPreferencesKey("last_rank")
        val LAST_HUMAN_BLACK = booleanPreferencesKey("last_human_black")
        val LAST_AI = stringPreferencesKey("last_ai_style")
    }
}
