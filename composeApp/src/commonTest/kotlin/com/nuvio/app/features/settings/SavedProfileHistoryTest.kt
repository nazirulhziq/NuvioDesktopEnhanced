package com.nuvio.app.features.settings

import kotlin.test.*

class SavedProfileHistoryTest {
    private val progress = """{"entries":[{"contentType":"movie","parentMetaId":"film1","parentMetaType":"movie",
        "videoId":"film1","title":"A film","lastPositionMs":5,"durationMs":100,"lastUpdatedEpochMs":7}]}"""
    private val marks = """{"items":[{"type":"film","id":"film1","name":"A film","markedAtEpochMs":8}],
        "providerPayloads":{"trakt":{"itemGroups":[{"id":"show1","type":"series","name":"A series",
        "entries":[{"season":1,"episode":3,"markedAtEpochMs":9}]}]}}}"""

    @Test fun combinesStoredPlaybackMarksAndCompactProviderHistory() {
        fun load(id: Int) = loadSavedProfileHistory(id, { if (it == 1) progress else null },
            { if (it == 2) null else marks })
        assertEquals(listOf("A series", "A film"), load(1).items.map { it.name })
        assertEquals(2, load(1).completed)
        assertEquals(1, load(1).episodes)
        assertTrue(load(2).items.isEmpty())
        assertEquals(1, loadSavedProfileHistory(1, { progress }, { null }).inProgress)
    }

    @Test fun malformedPayloadIsNotShownAsEmptyHistory() {
        assertFails { loadSavedProfileHistory(1, { "{" }, { null }) }
    }
}
