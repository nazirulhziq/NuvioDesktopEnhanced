package com.nuvio.app.features.settings

import com.nuvio.app.core.auth.AuthState

internal enum class AccountLogin { Loading, SignedOut, Guest, SignedIn }

/** Display fields only. Ownership is checked because auth and profile updates arrive independently. */
internal data class AccountOverview(
    val login: AccountLogin,
    val email: String? = null,
    val profileName: String? = null,
    val profileCount: Int? = null,
) {
    val canSignOut get() = login == AccountLogin.Guest || login == AccountLogin.SignedIn
    fun canDelete(policyEnabled: Boolean) = policyEnabled && login == AccountLogin.SignedIn
}

internal fun AuthState.accountOverview(
    profileOwner: String?,
    profileName: String?,
    loadedProfileOwners: List<String>?,
): AccountOverview = when (this) {
    AuthState.Loading -> AccountOverview(AccountLogin.Loading)
    AuthState.Unauthenticated -> AccountOverview(AccountLogin.SignedOut)
    is AuthState.Authenticated -> AccountOverview(
        login = if (isAnonymous) AccountLogin.Guest else AccountLogin.SignedIn,
        email = email?.trim()?.takeIf { !isAnonymous && it.isNotEmpty() },
        profileName = profileName?.trim()?.takeIf { userId.isNotBlank() && profileOwner == userId && it.isNotEmpty() },
        profileCount = loadedProfileOwners?.takeIf { owners ->
            userId.isNotBlank() && owners.isNotEmpty() && owners.all { it == userId }
        }?.size,
    )
}
