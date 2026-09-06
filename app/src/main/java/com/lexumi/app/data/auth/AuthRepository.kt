package com.lexumi.app.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.lexumi.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/** True once Supabase has a signed-in session (backed by its own persisted storage — survives app restarts). */
sealed class AuthState {
    object SignedOut : AuthState()
    object Loading : AuthState()
    data class SignedIn(val userId: String, val email: String?) : AuthState()
}

/** Mirrors a row in the `profiles` table — only the columns the client is allowed to
 * read/write. `isAdmin`/`isPremium` are read-only from here (see SCHEMA.md: the client's
 * Postgres grants only cover `display_name`, so attempting to set those two would be
 * rejected server-side even if this code tried to). */
@Serializable
data class RemoteProfile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_premium") val isPremium: Boolean = false,
)

@Serializable
private data class ProfileUpsert(val id: String, @SerialName("display_name") val displayName: String?)

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    /** Reflects Supabase's own session state — reactive, updates automatically on sign-in/out and token refresh. */
    val authState: Flow<AuthState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> AuthState.SignedIn(status.session.user?.id.orEmpty(), status.session.user?.email)
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.RefreshFailure -> AuthState.SignedOut
        }
    }

    /** Waits past Supabase's own startup check (it restores a persisted session from disk first) and
     * reports whether the user already has a valid session — used once at app launch to decide
     * whether to show the sign-in screen. */
    suspend fun isSignedIn(): Boolean {
        val status = supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return status is SessionStatus.Authenticated
    }

    /**
     * Shows the system's "choose a Google account" sheet (Credential Manager — the current,
     * non-deprecated replacement for the old GoogleSignInClient), then exchanges the resulting
     * ID token with Supabase to establish a session there. Throws on cancellation/failure —
     * the caller decides how to surface that.
     */
    suspend fun signInWithGoogle(context: Context) {
        val rawNonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(rawNonce)
        val hashedNonce = java.security.MessageDigest.getInstance("SHA-256")
            .digest(nonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Unexpected credential type from Google Sign-In")
        }
        val googleIdToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            error("Could not parse the Google ID token: ${e.message}")
        }

        supabase.auth.signInWith(IDToken) {
            idToken = googleIdToken
            provider = Google
            this.nonce = nonce
        }

        val userId = supabase.auth.currentUserOrNull()?.id
        Log.d("AUTH_DEBUG", "Current user id = $userId")

        ensureProfileRow()
    }

    /** Creates this user's row in `profiles` the first time they ever sign in — safe to call on
     * every sign-in since it's an "insert if missing, otherwise do nothing" upsert. It never
     * touches `is_admin`/`is_premium` (Postgres grants wouldn't allow the client to anyway — see
     * SCHEMA.md), so this can't be used to self-promote to admin or premium. */
    private suspend fun ensureProfileRow() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val displayName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
        supabase.from("profiles").upsert(
            ProfileUpsert(id = user.id, displayName = displayName),
        ) {
            onConflict = "id"
            ignoreDuplicates = true
        }
    }

    /** The current user's profile row — includes `isAdmin`/`isPremium` as the server sees them. */
    suspend fun getMyProfile(): RemoteProfile? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("profiles")
            .select(io.github.jan.supabase.postgrest.query.Columns.list("id", "display_name", "is_admin", "is_premium")) {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<RemoteProfile>()
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}

