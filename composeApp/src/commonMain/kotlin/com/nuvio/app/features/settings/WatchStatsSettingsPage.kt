package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.features.profiles.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.watchStatsContent(isTablet: Boolean) {
    item {
        val auth by AuthRepository.state.collectAsStateWithLifecycle()
        val profiles by ProfileRepository.state.collectAsStateWithLifecycle()
        val user = auth as? AuthState.Authenticated
        val profile = profiles.activeProfile?.takeIf { user != null && user.userId.isNotBlank() && it.userId == user.userId }
        if (profile == null) {
            Text(stringResource(when (auth) {
                AuthState.Loading -> Res.string.account_checking_login
                AuthState.Unauthenticated -> Res.string.stats_signed_out
                else -> Res.string.stats_no_profile
            }), modifier = Modifier.padding(16.dp))
        } else key(profile.userId, profile.profileIndex) { ProfileDashboard(profile, isTablet) }
    }
}

@Composable
private fun ProfileDashboard(profile: NuvioProfile, isTablet: Boolean) {
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { AvatarRepository.fetchAvatars() }
    var historyTab by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var stats by remember(refresh) { mutableStateOf<ProfileWatchStats?>(null) }
    var failed by remember(refresh) { mutableStateOf(false) }
    LaunchedEffect(refresh) {
        try { stats = withContext(Dispatchers.Default) { savedProfileHistory(profile.profileIndex) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 18.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActiveProfileMiniAvatar(profile, avatars, selected = false, size = 48)
            Text(profile.name, style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(Res.string.stats_scope), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth()) {
            Tab(selected = !historyTab, onClick = { historyTab = false }, modifier = Modifier.weight(1f),
                text = { Text(stringResource(Res.string.stats_overview)) })
            Tab(selected = historyTab, onClick = { historyTab = true }, modifier = Modifier.weight(1f),
                text = { Text(stringResource(Res.string.stats_history)) })
        }
        val snapshot = stats
        when {
            failed -> Text(stringResource(Res.string.stats_unavailable))
            snapshot == null -> Text(stringResource(Res.string.stats_loading))
            snapshot.items.isEmpty() -> Text(stringResource(Res.string.stats_empty))
            historyTab -> SettingsSection(stringResource(Res.string.stats_recent_limit), isTablet) {
                SettingsGroup(isTablet = isTablet) {
                    snapshot.items.take(20).forEach { item ->
                        val name = item.name.ifBlank { stringResource(Res.string.stats_untitled) }
                        val episode = if (item.season != null && item.episode != null) " · S${item.season} E${item.episode}" else ""
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(name + episode, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(if (item.completed) Res.string.stats_completed else Res.string.stats_in_progress),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            else -> SettingsSection(stringResource(Res.string.stats_activity), isTablet) {
                SettingsGroup(isTablet = isTablet) {
                    listOf(Res.string.stats_titles to snapshot.titles, Res.string.stats_movies to snapshot.movies,
                        Res.string.stats_series to snapshot.series, Res.string.stats_episodes to snapshot.episodes,
                        Res.string.stats_in_progress to snapshot.inProgress, Res.string.stats_completed to snapshot.completed
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(label), Modifier.weight(1f))
                            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        TextButton(enabled = failed || snapshot != null, onClick = { refresh++ }) {
            Text(stringResource(Res.string.stats_refresh))
        }
    }
}
