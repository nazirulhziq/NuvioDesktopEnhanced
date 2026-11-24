package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileWatchStatsTest {
    private val movie = HistoryRecord("movie", "shared", "Film", updated = 10, started = true)
    private val episode = HistoryRecord("show", "shared", "Series", season = 1, episode = 2, updated = 12, completed = true)

    @Test fun emptyAndNoEvidence() {
        val stats = aggregateHistory(listOf(movie.copy(started = false), episode.copy(id = " ")))
        assertTrue(stats.items.isEmpty())
        assertEquals(0, stats.titles)
    }

    @Test fun partialAndUnknownTypes() {
        val stats = aggregateHistory(listOf(movie, episode, movie.copy(type = "video", name = "", updated = 0)))
        assertEquals(3, stats.titles)
        assertEquals(1, stats.episodes)
        assertEquals(2, stats.inProgress)
        assertEquals(1, stats.completed)
        assertEquals("Series", stats.items.first().name)
    }

    @Test fun liveChannelsAreNotEpisodesOrSeries() {
        // Live TV player launches use "tv" without season/episode coordinates.
        val channel = movie.copy(type = "tv", id = "channel:news")
        val stats = aggregateHistory(listOf(channel, episode, episode.copy(type = "tv")))
        assertEquals(2, stats.titles)
        assertEquals(1, stats.series)
        assertEquals(1, stats.episodes)
    }

    @Test fun duplicatesAcrossMarksAndPlayback() {
        val records = listOf(movie, movie.copy(type = "film", completed = true, updated = 15), episode,
            episode.copy(type = "series", videoId = "other", updated = 8), episode.copy(episode = 3))
        val stats = aggregateHistory(records)
        assertEquals(2, stats.titles)
        assertEquals(2, stats.episodes)
        assertEquals(3, stats.completed)
        assertEquals(stats, aggregateHistory(records.reversed()))
    }

    @Test fun switchingProfilesUsesOnlyRequestedStorage() {
        fun load(profile: Int) = readProfileHistory(profile,
            progress = { if (it == 1) listOf(movie) else emptyList() },
            watched = { if (it == 2) listOf(episode) else emptyList() })
        assertEquals(listOf("Film"), load(1).items.map { it.name })
        assertEquals(listOf("Series"), load(2).items.map { it.name })
        assertTrue(load(3).items.isEmpty())
        assertEquals(load(1), load(1))
    }
}
