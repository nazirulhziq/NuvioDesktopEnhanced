package com.nuvio.app.features.settings

internal data class HistoryRecord(
    val type: String, val id: String, val name: String,
    val season: Int? = null, val episode: Int? = null, val videoId: String? = null,
    val updated: Long = 0, val started: Boolean = false, val completed: Boolean = false,
)

internal data class ProfileWatchStats(val items: List<HistoryRecord>) {
    private val titleKeys = items.map { it.type to it.id }.toSet()
    val titles = titleKeys.size
    val movies = titleKeys.count { it.first == "movie" }
    val series = titleKeys.count { it.first == "series" }
    val episodes = items.count { it.season != null && it.episode != null }
    val completed = items.count { it.completed }
    val inProgress = items.size - completed
}

internal fun readProfileHistory(
    profile: Int, progress: (Int) -> List<HistoryRecord>, watched: (Int) -> List<HistoryRecord>,
): ProfileWatchStats = aggregateHistory(progress(profile) + watched(profile))

internal fun aggregateHistory(records: List<HistoryRecord>): ProfileWatchStats {
    val order = compareByDescending<HistoryRecord> { it.updated }.thenBy { it.type }.thenBy { it.id }
        .thenBy { it.season }.thenBy { it.episode }.thenBy { it.videoId }.thenBy { it.name }
    val groups = records.asSequence().filter { it.id.isNotBlank() && (it.started || it.completed) }.map {
        val type = when (val raw = it.type.trim().lowercase()) {
            "movie", "film" -> "movie"
            "series", "show", "tvshow", "anime" -> "series"
            // IPTV also uses "tv"; only classify an ambiguous TV record as a series with episode evidence.
            "tv" -> if (it.season != null && it.episode != null) "series" else raw
            else -> raw
        }
        it.copy(type = type, id = it.id.trim(), name = it.name.trim(), videoId = it.videoId?.takeIf { video ->
            type != "movie" && (it.season == null || it.episode == null) && video.isNotBlank() && video != it.id
        })
    }.groupBy { it.copy(name = "", updated = 0, completed = false, started = false) }
    return ProfileWatchStats(groups.values.map { group ->
        val ordered = group.sortedWith(order)
        ordered.first().copy(name = ordered.firstOrNull { it.name.isNotEmpty() }?.name.orEmpty(),
            started = group.any { it.started }, completed = group.any { it.completed })
    }.sortedWith(order))
}
