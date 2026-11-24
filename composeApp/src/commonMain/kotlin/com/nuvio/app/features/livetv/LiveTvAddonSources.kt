package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.catalog.supportsPagination

/** Use the viewing profile, not the profile from which it may inherit installed addons. */
internal fun liveTvProfileKey(userId: String, profileIndex: Int) = "live_tv_addons:$userId:$profileIndex"

internal fun liveTvAssignmentScope(accountId: String?, profileOwner: String?, index: Int?): String? =
    if (!accountId.isNullOrBlank() && profileOwner == accountId && index != null) liveTvProfileKey(accountId, index) else null

internal class LiveTvAddonAssignments(
    private val read: (String) -> Set<String>,
    private val write: (String, Set<String>) -> Unit,
) {
    fun selected(profile: String) = read(profile)

    fun set(profile: String, manifestUrl: String, enabled: Boolean): Set<String> {
        val old = selected(profile)
        val updated = if (enabled) old + manifestUrl else old - manifestUrl
        if (updated != old) write(profile, updated)
        return updated
    }
}

data class LiveTvAddonCatalog(val title: String, val addonName: String, val target: CatalogTarget.Addon)

internal fun liveTvAddonCatalogs(addon: ManagedAddon): List<LiveTvAddonCatalog> {
    if (!addon.isActive) return emptyList()
    return addon.manifest?.catalogs.orEmpty().flatMap { catalog ->
        val required = catalog.extra.filter { it.isRequired }
        val genres = when {
            required.isEmpty() -> listOf(null)
            required.size == 1 && required.single().name == "genre" -> required.single().options
            else -> emptyList() // Search-only or other catalogs that cannot be browsed.
        }
        genres.map { genre ->
            LiveTvAddonCatalog(
                title = listOfNotNull(catalog.name.ifBlank { catalog.id }, genre).joinToString(" · "),
                addonName = addon.displayTitle,
                target = CatalogTarget.Addon(addon.manifestUrl, catalog.type, catalog.id, genre, catalog.supportsPagination()),
            )
        }
    }.distinctBy { it.target }
}
