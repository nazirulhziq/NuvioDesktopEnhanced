package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal actual object LiveTvStorage {
    private const val playlistUrlKey = "live_tv_playlist_url"
    private const val playlistsBlobKey = "live_tv_playlists_blob"
    private const val favoritesBlobKey = "live_tv_favorite_channels_blob"
    private const val lastWatchedKey = "live_tv_last_watched_channel"
    private const val recentBlobKey = "live_tv_recent_channels_blob"
    private const val viewModeKey = "live_tv_channel_view_mode"
    private const val navigationEnabledKey = "live_tv_navigation_enabled"
    private const val stalkerKey = "live_tv_stalker_settings"
    private const val xtreamKey = "live_tv_xtream_settings"
    private const val epgSourcesKey = "live_tv_epg_sources_blob"
    private const val epgAutoDetectKey = "live_tv_epg_auto_detect"

    private val store = DesktopStorage.store("nuvio_live_tv_settings")
    actual fun loadAddonSources(key: String): Set<String> = store.getStringSet(key).orEmpty()
    actual fun saveAddonSources(key: String, sources: Set<String>) = store.putStringSet(key, sources)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    actual fun loadPlaylistUrl(): String? = store.getString(playlistUrlKey)
    actual fun savePlaylistUrl(url: String) = store.putString(playlistUrlKey, url)

    actual fun loadPlaylistsBlob(): String? = store.getString(playlistsBlobKey)
    actual fun savePlaylistsBlob(blob: String) = store.putString(playlistsBlobKey, blob)

    actual fun loadFavoriteChannelIdsBlob(): String? = store.getString(favoritesBlobKey)
    actual fun saveFavoriteChannelIdsBlob(blob: String) = store.putString(favoritesBlobKey, blob)

    actual fun loadLastWatchedChannelId(): String? = store.getString(lastWatchedKey)
    actual fun saveLastWatchedChannelId(channelId: String) = store.putString(lastWatchedKey, channelId)

    actual fun loadRecentChannelIdsBlob(): String? = store.getString(recentBlobKey)
    actual fun saveRecentChannelIdsBlob(blob: String) = store.putString(recentBlobKey, blob)

    actual fun loadChannelViewMode(): String? = store.getString(viewModeKey)
    actual fun saveChannelViewMode(mode: String) = store.putString(viewModeKey, mode)

    actual fun loadNavigationEnabled(): Boolean? = store.getBoolean(navigationEnabledKey)
    actual fun saveNavigationEnabled(enabled: Boolean) = store.putBoolean(navigationEnabledKey, enabled)

    actual fun loadStalkerSettings(): LiveTvStalkerSettings =
        store.getString(stalkerKey)?.let { raw ->
            runCatching {
                val obj = json.parseToJsonElement(raw).jsonObject
                LiveTvStalkerSettings(
                    portalUrl = obj["portalUrl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    macAddress = obj["macAddress"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    username = obj["username"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    password = obj["password"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    isEnabled = obj["isEnabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                )
            }.getOrNull()
        } ?: LiveTvStalkerSettings()
    actual fun saveStalkerSettings(settings: LiveTvStalkerSettings) =
        store.putString(stalkerKey, buildJsonObject {
            put("portalUrl", settings.portalUrl)
            put("macAddress", settings.macAddress)
            put("username", settings.username)
            put("password", settings.password)
            put("isEnabled", settings.isEnabled)
        }.toString())

    actual fun loadXtreamSettings(): LiveTvXtreamSettings =
        store.getString(xtreamKey)?.let { raw ->
            runCatching {
                val obj = json.parseToJsonElement(raw).jsonObject
                LiveTvXtreamSettings(
                    serverUrl = obj["serverUrl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    username = obj["username"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    password = obj["password"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    isEnabled = obj["isEnabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                )
            }.getOrNull()
        } ?: LiveTvXtreamSettings()
    actual fun saveXtreamSettings(settings: LiveTvXtreamSettings) =
        store.putString(xtreamKey, buildJsonObject {
            put("serverUrl", settings.serverUrl)
            put("username", settings.username)
            put("password", settings.password)
            put("isEnabled", settings.isEnabled)
        }.toString())

    actual fun publishNavigationVisibility(visible: Boolean) = Unit

    actual fun loadEpgSourcesBlob(): String? = store.getString(epgSourcesKey)
    actual fun saveEpgSourcesBlob(blob: String) = store.putString(epgSourcesKey, blob)

    actual fun loadEpgAutoDetectEnabled(): Boolean? = store.getBoolean(epgAutoDetectKey)
    actual fun saveEpgAutoDetectEnabled(enabled: Boolean) = store.putBoolean(epgAutoDetectKey, enabled)
}
