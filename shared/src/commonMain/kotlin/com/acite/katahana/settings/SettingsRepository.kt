package com.acite.katahana.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.PlayerSeat
import com.acite.katahana.domain.parseAiStyle
import com.acite.katahana.domain.parseSeatKind
import com.acite.katahana.domain.seatsFromLegacy
import com.acite.katahana.domain.toStorageId
import com.acite.katahana.engine.EngineProfile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

const val FORECAST_DROP_MS_DEFAULT = 400
const val FORECAST_DROP_MS_MAX = 3000
const val COORD_EDGE_PAD_DP_DEFAULT = 5
const val COORD_GRID_PAD_DP_DEFAULT = 3
const val COORD_PAD_DP_MAX = 24

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
    val drawerAcrylic: Flow<Float> = dataStore.data.map {
        (it[Keys.DRAWER_ACRYLIC] ?: 0.55f).coerceIn(0f, 1f)
    }
    val ownershipStyle: Flow<OwnershipStyle> = dataStore.data.map {
        OwnershipStyle.fromId(it[Keys.OWNERSHIP_STYLE])
    }
    val appearanceId: Flow<String> = dataStore.data.map {
        it[Keys.APPEARANCE] ?: "sky_sakura"
    }
    val analysisLayoutMode: Flow<AnalysisLayoutMode> = dataStore.data.map {
        AnalysisLayoutMode.fromId(it[Keys.ANALYSIS_LAYOUT])
    }
    val engineName: Flow<String> = dataStore.data.map { it[Keys.ENGINE_NAME] ?: "KataGo" }
    val engineUrl: Flow<String> = dataStore.data.map {
        it[Keys.ENGINE_URL] ?: EngineProfile.DEFAULT_URL
    }
    val engineToken: Flow<String> = dataStore.data.map { it[Keys.ENGINE_TOKEN] ?: "" }
    val playVisits: Flow<Int> = dataStore.data.map { it[Keys.PLAY_VISITS] ?: 400 }
    val reviewVisits: Flow<Int> = dataStore.data.map { it[Keys.REVIEW_VISITS] ?: 400 }
    val forecastDropMs: Flow<Int> = dataStore.data.map {
        (it[Keys.FORECAST_DROP_MS] ?: FORECAST_DROP_MS_DEFAULT).coerceIn(0, FORECAST_DROP_MS_MAX)
    }
    val coordEdgePadDp: Flow<Int> = dataStore.data.map {
        (it[Keys.COORD_EDGE_PAD_DP] ?: COORD_EDGE_PAD_DP_DEFAULT).coerceIn(0, COORD_PAD_DP_MAX)
    }
    val coordGridPadDp: Flow<Int> = dataStore.data.map {
        (it[Keys.COORD_GRID_PAD_DP] ?: COORD_GRID_PAD_DP_DEFAULT).coerceIn(0, COORD_PAD_DP_MAX)
    }
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
        val boardSize = if (size == 9 || size == 13 || size == 19) size else 19
        val komi = prefs[Keys.LAST_KOMI] ?: 7.5f
        val (black, white) = if (prefs[Keys.LAST_BLACK_KIND] != null || prefs[Keys.LAST_WHITE_KIND] != null) {
            PlayerSeat(
                kind = parseSeatKind(prefs[Keys.LAST_BLACK_KIND]),
                rankKyu = (prefs[Keys.LAST_BLACK_RANK] ?: 5).coerceIn(-2, 15),
            ) to PlayerSeat(
                kind = parseSeatKind(prefs[Keys.LAST_WHITE_KIND]),
                rankKyu = (prefs[Keys.LAST_WHITE_RANK] ?: 5).coerceIn(-2, 15),
            )
        } else {
            seatsFromLegacy(
                mode = if (prefs[Keys.LAST_MODE] == "hvai") PlayMode.HumanVsAi else PlayMode.HumanVsHuman,
                humanPlaysBlack = prefs[Keys.LAST_HUMAN_BLACK] ?: true,
                aiStyle = parseAiStyle(prefs[Keys.LAST_AI]),
                rankKyu = (prefs[Keys.LAST_RANK] ?: 5).coerceIn(-2, 15),
            )
        }
        GameConfig(boardSize = boardSize, komi = komi, black = black, white = white)
    }

    suspend fun setConfirmMove(value: Boolean) = edit { it[Keys.CONFIRM_MOVE] = value }
    suspend fun setShowCoords(value: Boolean) = edit { it[Keys.SHOW_COORDS] = value }
    suspend fun setShowCandidates(value: Boolean) = edit { it[Keys.SHOW_CANDIDATES] = value }
    suspend fun setShowQuality(value: Boolean) = edit { it[Keys.SHOW_QUALITY] = value }
    suspend fun setShowConnections(value: Boolean) = edit { it[Keys.SHOW_CONNECTIONS] = value }
    suspend fun setShowOwnership(value: Boolean) = edit { it[Keys.SHOW_OWNERSHIP] = value }
    suspend fun setShowDeadStones(value: Boolean) = edit { it[Keys.SHOW_DEAD_STONES] = value }
    suspend fun setDrawerAcrylic(value: Float) = edit { it[Keys.DRAWER_ACRYLIC] = value.coerceIn(0f, 1f) }
    suspend fun setOwnershipStyle(value: OwnershipStyle) = edit { it[Keys.OWNERSHIP_STYLE] = value.id }
    suspend fun setAppearanceId(value: String) = edit { it[Keys.APPEARANCE] = value }
    suspend fun setAnalysisLayoutMode(value: AnalysisLayoutMode) = edit {
        it[Keys.ANALYSIS_LAYOUT] = value.id
    }
    suspend fun setEngineName(value: String) = edit { it[Keys.ENGINE_NAME] = value }
    suspend fun setEngineUrl(value: String) = edit { it[Keys.ENGINE_URL] = value }
    suspend fun setEngineToken(value: String) = edit { it[Keys.ENGINE_TOKEN] = value }
    suspend fun setPlayVisits(value: Int) = edit { it[Keys.PLAY_VISITS] = value.coerceAtLeast(1) }
    suspend fun setReviewVisits(value: Int) = edit { it[Keys.REVIEW_VISITS] = value.coerceAtLeast(1) }
    suspend fun setForecastDropMs(value: Int) = edit {
        it[Keys.FORECAST_DROP_MS] = value.coerceIn(0, FORECAST_DROP_MS_MAX)
    }
    suspend fun setCoordEdgePadDp(value: Int) = edit {
        it[Keys.COORD_EDGE_PAD_DP] = value.coerceIn(0, COORD_PAD_DP_MAX)
    }
    suspend fun setCoordGridPadDp(value: Int) = edit {
        it[Keys.COORD_GRID_PAD_DP] = value.coerceIn(0, COORD_PAD_DP_MAX)
    }
    suspend fun setLastGame(value: GameConfig) = edit {
        it[Keys.LAST_SIZE] = value.boardSize
        it[Keys.LAST_KOMI] = value.komi
        it[Keys.LAST_MODE] = if (value.mode == PlayMode.HumanVsAi) "hvai" else "hvh"
        it[Keys.LAST_RANK] = value.rankKyu
        it[Keys.LAST_HUMAN_BLACK] = value.humanPlaysBlack
        it[Keys.LAST_AI] = value.aiStyle.toStorageId()
        it[Keys.LAST_BLACK_KIND] = value.black.kind.toStorageId()
        it[Keys.LAST_WHITE_KIND] = value.white.kind.toStorageId()
        it[Keys.LAST_BLACK_RANK] = value.black.rankKyu
        it[Keys.LAST_WHITE_RANK] = value.white.rankKyu
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
        val DRAWER_ACRYLIC = floatPreferencesKey("drawer_acrylic")
        val OWNERSHIP_STYLE = stringPreferencesKey("ownership_style")
        val APPEARANCE = stringPreferencesKey("appearance")
        val ANALYSIS_LAYOUT = stringPreferencesKey("analysis_layout")
        val ENGINE_NAME = stringPreferencesKey("engine_name")
        val ENGINE_URL = stringPreferencesKey("engine_url")
        val ENGINE_TOKEN = stringPreferencesKey("engine_token")
        val PLAY_VISITS = intPreferencesKey("play_visits")
        val REVIEW_VISITS = intPreferencesKey("review_visits")
        val FORECAST_DROP_MS = intPreferencesKey("forecast_drop_ms")
        val COORD_EDGE_PAD_DP = intPreferencesKey("coord_edge_pad_dp")
        val COORD_GRID_PAD_DP = intPreferencesKey("coord_grid_pad_dp")
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
        val LAST_BLACK_KIND = stringPreferencesKey("last_black_kind")
        val LAST_WHITE_KIND = stringPreferencesKey("last_white_kind")
        val LAST_BLACK_RANK = intPreferencesKey("last_black_rank")
        val LAST_WHITE_RANK = intPreferencesKey("last_white_rank")
    }
}
