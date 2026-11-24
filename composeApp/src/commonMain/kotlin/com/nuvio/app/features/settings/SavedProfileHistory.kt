package com.nuvio.app.features.settings

import com.nuvio.app.features.watched.*
import com.nuvio.app.features.watchprogress.StoredWatchProgressPayload
import com.nuvio.app.features.watchprogress.WatchProgressStorage
import kotlinx.serialization.json.Json

private val historyJson = Json { ignoreUnknownKeys = true }

// Read only the requested profile's snapshots. Do not switch the global playback repositories.
internal fun savedProfileHistory(profile: Int): ProfileWatchStats = loadSavedProfileHistory(
    profile, WatchProgressStorage::loadPayload, WatchedStorage::loadPayload,
)

internal fun loadSavedProfileHistory(
    profile: Int, progressPayload: (Int) -> String?, watchedPayload: (Int) -> String?,
): ProfileWatchStats = readProfileHistory(profile,
    progress = { id ->
        progressPayload(id)?.takeIf(String::isNotBlank)?.let { payload ->
            historyJson.decodeFromString<StoredWatchProgressPayload>(payload).entries.map { entry ->
                HistoryRecord(entry.parentMetaType, entry.parentMetaId, entry.title, entry.seasonNumber,
                    entry.episodeNumber, entry.videoId, entry.lastUpdatedEpochMs,
                    started = entry.lastPositionMs > 0 || (entry.progressPercent ?: 0f) > 0f,
                    completed = entry.isEffectivelyCompleted)
            }
        }.orEmpty()
    },
    watched = { id ->
        watchedPayload(id)?.takeIf(String::isNotBlank)?.let { payload ->
            require(shouldRestoreWatchedPayload(payload.length))
            val stored = historyJson.decodeFromString<StoredWatchedPayload>(payload)
            val items = stored.items + stored.providerPayloads.values.flatMap {
                it.items + expandProviderWatchedItems(it.itemGroups)
            }
            items.map { item -> HistoryRecord(item.type, item.id, item.name, item.season, item.episode,
                item.videoId, item.normalizedMarkedAt().markedAtEpochMs, completed = true) }
        }.orEmpty()
    },
)
