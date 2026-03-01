package de.felsernet.android.eiskalt.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import de.felsernet.android.eiskalt.IconInfo
import de.felsernet.android.eiskalt.IconType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Supabase Storage implementation of IconStorage.
 * Stores icons in a Supabase Storage bucket.
 *
 * @param context Android context
 * @param supabaseUrl Your Supabase project URL (e.g., "https://your-project.supabase.co")
 * @param supabaseKey Your Supabase anonymous/public API key
 * @param bucketName The name of the storage bucket to use (default: "icons")
 */
class SupabaseIconStorage(
    private val context: Context,
    private val supabaseUrl: String,
    private val supabaseKey: String,
    private val bucketName: String = "icons"
) : IconStorage {

    private val supabase: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Storage)
            install(Auth) {
                // Configure auth settings if needed
            }
        }
    }

    private val storage by lazy {
        supabase.storage.from(bucketName)
    }

    /**
     * Signs in to Supabase using a Google ID token.
     */
    suspend fun signIn() {
        // Check if already authenticated with Supabase
        if (isAuthenticated()) {
            Log.d("SupabaseIconStorage", "Already authenticated with Supabase")
            return
        }

        try {
            val token = getGoogleIdToken()
            if (token != null) {
                supabase.auth.signInWith(IDToken) {
                    this.idToken = token
                    this.provider = Google
                }
                Log.d("SupabaseIconStorage", "Successfully signed in to Supabase with Google ID token")
            } else {
                Log.e("SupabaseIconStorage", "Failed to get Google ID token")
            }
        } catch (e: Exception) {
            Log.e("SupabaseIconStorage", "Failed to sign in to Supabase: ${e.message}")
        }
    }

    private suspend fun getGoogleIdToken(): String? {
         // 1. Configure the Google ID Request
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Set to true to only show accounts already used with this app
            .setServerClientId("620848965702-tfvchbati79vqucpd6t7oambgj0v476o.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        return try {
            // 2. Trigger the Google One Tap / Bottom Sheet UI
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            // 3. Extract the ID Token
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdTokenCredential.idToken
            } else {
                null
            }
        } catch (e: Exception) {
            // Handle cancellation or errors (e.g., No credentials available)
            e.printStackTrace()
            null
        }
    }

    /**
     * Signs out the current user from Supabase Auth.
     */
    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.e("SupabaseIconStorage", "Sign out failed: ${e.message}")
        }
    }

    /**
     * Checks if a user is currently authenticated with Supabase.
     *
     * @return true if a user is authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    /**
     * Gets the current authenticated user's ID, or null if not authenticated.
     *
     * @return The user ID or null
     */
    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun storeIcon(uri: Uri, filePrefix: String): IconInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Generate unique filename
                val prefix = filePrefix.takeIf { it.isNotEmpty() }?.let { "${it}_" } ?: ""
                val fileName = "${prefix}icon_${UUID.randomUUID()}.png"

                // Read file content from URI
                val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext null

                // Upload to Supabase Storage
                storage.upload(fileName, bytes) {
                    contentType = ContentType.Image.PNG
                    upsert = false
                }

                // Get public URL for the uploaded file
                val publicUrl = storage.publicUrl(fileName)

                // Return IconInfo with Supabase URL
                IconInfo(IconType.LOCAL_FILE, publicUrl)
            } catch (e: Exception) {
                Log.e("SupabaseIconStorage", "Upload failed: " + e.message)
                null
            }
        }
    }

    override suspend fun deleteIcon(iconInfo: IconInfo): Boolean {
        if (iconInfo.type != IconType.LOCAL_FILE) {
            // Only handle icons stored in Supabase
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // Extract filename from URL
                val fileName = extractFileNameFromUrl(iconInfo.path)
                    ?: return@withContext false

                storage.delete(fileName)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun iconExists(iconInfo: IconInfo): Boolean {
        if (iconInfo.type != IconType.LOCAL_FILE) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // List files and check if our file exists
                val fileName = extractFileNameFromUrl(iconInfo.path)
                    ?: return@withContext false

                val files = storage.list()
                files.any { it.name == fileName }
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun listAllIcons(): List<IconInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val files = storage.list()
                files.filter { file ->
                    file.name.endsWith(".png", ignoreCase = true) ||
                    file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                    file.name.endsWith(".webp", ignoreCase = true)
                }.map { file ->
                    val publicUrl = storage.publicUrl(file.name)
                    IconInfo(IconType.LOCAL_FILE, publicUrl)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Extracts the filename from a Supabase public URL.
     * URLs typically look like: https://project.supabase.co/storage/v1/object/public/bucket/filename.png
     */
    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            // Get the last path segment which should be the filename
            uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }
}
