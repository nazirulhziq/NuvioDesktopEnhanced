package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.addons.*
import com.nuvio.app.features.home.MetaPreview
import java.nio.file.Files
import kotlin.test.*

class LiveTvAddonSourcesTest {
    @Test fun accountTransitionCannotUseAStaleViewingProfile() {
        assertNull(liveTvAssignmentScope("next", "previous", 2))
        assertNull(liveTvAssignmentScope(null, "previous", 2))
        assertNull(liveTvAssignmentScope("", "", 1))
        assertNull(liveTvAssignmentScope("next", "next", null))
        assertEquals(liveTvProfileKey("next", 2), liveTvAssignmentScope("next", "next", 2))
    }

    @Test fun assignmentsSurviveReloadAndRemovalIsProfileScoped() {
        val file = Files.createTempFile("live-tv-test", ".properties")
        try {
            fun store() = DesktopStorage.Store(file)
            fun assignments(store: DesktopStorage.Store) = LiveTvAddonAssignments(
                { store.getStringSet(it).orEmpty() }, store::putStringSet,
            )
            val first = liveTvProfileKey("account-a", 1)
            val second = liveTvProfileKey("account-a", 2)
            val otherAccount = liveTvProfileKey("account-b", 1)
            val initial = store()
            assignments(initial).apply {
                set(first, "one", true); set(first, "two", true); set(second, "one", true)
            }
            initial.flushSync()
            val restored = store()
            assignments(restored).apply {
                assertEquals(setOf("one", "two"), selected(first))
                assertTrue(selected(otherAccount).isEmpty())
                set(first, "one", false)
            }
            restored.flushSync()
            assignments(store()).apply {
                assertEquals(setOf("two"), selected(first)); assertEquals(setOf("one"), selected(second))
            }
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun mapsBrowsableTargetsAndRejectsUnavailableAddons() {
        val genre = AddonExtraProperty("genre", true, listOf("Sport", "News"))
        val channels = AddonCatalog("tv", "live", "Stations", listOf(genre, AddonExtraProperty("skip")))
        val search = channels.copy(id = "search", extra = listOf(AddonExtraProperty("search", true)))
        val url = "https://example.invalid/config/manifest.json"
        val addon = ManagedAddon(url, AddonManifest("id", "Test", "", "1", resources = emptyList(), types = listOf("tv"),
            catalogs = listOf(channels, search, channels), transportUrl = url))
        val targets = liveTvAddonCatalogs(addon).map { it.target }
        assertEquals(listOf("Sport", "News"), targets.map { it.genre })
        assertTrue(targets.all { it.manifestUrl == url && it.catalogId == "live" && it.contentType == "tv" && it.supportsPagination })
        assertTrue(liveTvAddonCatalogs(addon.copy(enabled = false)).isEmpty())
        assertTrue(liveTvAddonCatalogs(addon.copy(manifest = null)).isEmpty())
    }

    @Test fun playbackKeepsOpaqueChannelIdentityAndPlaylistHeaders() {
        val preview = MetaPreview("live:news/1", "channel", "News")
        val stream = assertNotNull(preview.liveTvStreamLaunch(2))
        assertEquals(2, stream.profileId); assertEquals(preview.id, stream.videoId); assertEquals(preview.type, stream.type)
        assertNull(preview.copy(type = "series").liveTvStreamLaunch(2))
        val channel = LiveTvChannel("iptv:42", "News", "https://example.invalid/live", headers = mapOf("Referer" to "test"))
        val player = channel.toPlayerLaunch(1)
        assertEquals(channel.streamUrl, player.sourceUrl); assertEquals(channel.headers, player.sourceHeaders)
    }
}
