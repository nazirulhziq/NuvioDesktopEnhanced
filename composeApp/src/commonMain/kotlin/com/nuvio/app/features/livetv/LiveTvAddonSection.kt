package com.nuvio.app.features.livetv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.userId
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.profiles.ProfileRepository
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val assignments = LiveTvAddonAssignments(LiveTvStorage::loadAddonSources, LiveTvStorage::saveAddonSources)

@Composable
internal fun LiveTvAddonSection(onOpen: (LiveTvAddonCatalog) -> Unit) {
    val auth by AuthRepository.state.collectAsStateWithLifecycle()
    val profiles by ProfileRepository.state.collectAsStateWithLifecycle()
    val active = profiles.activeProfile
    val profile = liveTvAssignmentScope(auth.userId, active?.userId, active?.profileIndex)
    if (profile == null) {
        Text(stringResource(Res.string.live_tv_addon_profile))
        return
    }
    val installed by AddonRepository.uiState.collectAsStateWithLifecycle()
    var selected by remember(profile) { mutableStateOf(assignments.selected(profile)) }
    val catalogs = remember(installed) { installed.addons.associate { it.manifestUrl to liveTvAddonCatalogs(it) } }
    val available = catalogs.filterValues { it.isNotEmpty() }.keys
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(Res.string.live_tv_addon_sources), style = MaterialTheme.typography.titleMedium)
        val visible = selected.flatMap { catalogs[it].orEmpty() }
        val loading = installed.addons.any { it.enabled && it.isRefreshing }
        if (visible.isEmpty() && !loading) {
            Text(stringResource(if (available.isEmpty()) Res.string.live_tv_addon_none else Res.string.live_tv_addon_select))
        }
        (available + selected).forEach { url ->
            val addon = installed.addons.firstOrNull { it.manifestUrl == url }
            val checked = url in selected
            Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch) {
                selected = assignments.set(profile, url, it)
            }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(addon?.displayTitle ?: stringResource(Res.string.live_tv_addon_removed), Modifier.weight(1f))
                Switch(checked, onCheckedChange = null)
            }
        }
        installed.addons.filter { it.enabled && it.errorMessage != null }.forEach { addon ->
            Text("${addon.displayTitle}: ${stringResource(Res.string.live_tv_addon_failed)}")
            TextButton(onClick = { AddonRepository.refreshAddon(addon.manifestUrl, forceRefresh = true) }) { Text(stringResource(Res.string.action_retry)) }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        visible.forEach { catalog ->
            OutlinedButton(onClick = { onOpen(catalog) }, modifier = Modifier.fillMaxWidth()) {
                Text("${catalog.addonName} · ${catalog.title}")
            }
        }
    }
}
