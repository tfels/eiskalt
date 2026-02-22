package de.felsernet.android.eiskalt.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import de.felsernet.android.eiskalt.IconInfo
import de.felsernet.android.eiskalt.IconType
import io.github.jan.supabase.SupabaseClient
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
        }
    }

    private val storage by lazy {
        supabase.storage.from(bucketName)
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
