package com.nuvio.app.features.settings

import com.nuvio.app.core.auth.AuthState
import kotlin.test.*

class AccountOverviewTest {
    private fun AuthState.view() = accountOverview("owner", " Movies ", listOf("owner", "owner"))

    @Test fun signedOutAndLoadingHideAccountActionsAndProfile() {
        for (auth in listOf(AuthState.Loading, AuthState.Unauthenticated)) {
            val overview = auth.view()
            assertFalse(overview.canSignOut)
            assertFalse(overview.canDelete(true))
            assertNull(overview.profileName)
        }
    }

    @Test fun signedInActionsRespectPolicyAndProfileOwnership() {
        val auth = AuthState.Authenticated("owner", "person@example.invalid", false)
        assertEquals("Movies", auth.view().profileName)
        assertEquals(2, auth.view().profileCount)
        assertTrue(auth.view().canDelete(true))
        assertFalse(auth.view().canDelete(false))
        val switched = auth.copy(userId = "other").view()
        assertNull(switched.profileName)
        assertNull(switched.profileCount)
    }

    @Test fun guestHasNoEmailOrAccountDeletion() {
        val guest = AuthState.Authenticated("owner", "unused@example.invalid", true).view()
        assertNull(guest.email)
        assertTrue(guest.canSignOut)
        assertFalse(guest.canDelete(true))
    }
}
