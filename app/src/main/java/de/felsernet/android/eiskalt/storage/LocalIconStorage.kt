package de.felsernet.android.eiskalt.storage

import android.content.Context
import android.net.Uri
import de.felsernet.android.eiskalt.IconInfo
import de.felsernet.android.eiskalt.IconType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Local file system implementation of IconStorage.
 * Stores icons in the app's private files directory under "custom_icons".
 */
class LocalIconStorage(private val context: Context) : IconStorage {

    companion object {
        private const val ICONS_DIRECTORY = "custom_icons"
    }

    private val iconsDir: File by lazy {
        File(context.filesDir, ICONS_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override suspend fun storeIcon(uri: Uri, filePrefix: String): IconInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Create a unique filename for the custom icon
                val prefix = filePrefix.takeIf { it.isNotEmpty() }?.let { "${it}_" } ?: ""
                val fileName = "${prefix}custom_icon_${UUID.randomUUID()}.png"
                val destFile = File(iconsDir, fileName)

                // Copy the selected image to app's private storage
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext null

                // Create IconInfo for the custom icon
                IconInfo(IconType.LOCAL_FILE, destFile.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun deleteIcon(iconInfo: IconInfo): Boolean {
        if (iconInfo.type != IconType.LOCAL_FILE) {
            // Only local files are managed by this storage
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val file = File(iconInfo.path)
                if (file.exists()) {
                    file.delete()
                } else {
                    true // File doesn't exist, consider it deleted
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun iconExists(iconInfo: IconInfo): Boolean {
        if (iconInfo.type != IconType.LOCAL_FILE) {
            // For non-local icons, we can't verify existence here
            return false
        }

        return withContext(Dispatchers.IO) {
            File(iconInfo.path).exists()
        }
    }

    override suspend fun listAllIcons(): List<IconInfo> {
        return withContext(Dispatchers.IO) {
            try {
                iconsDir.listFiles { file -> file.isFile }?.map { file ->
                    IconInfo(IconType.LOCAL_FILE, file.absolutePath)
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getResolvableUrl(iconInfo: IconInfo): String? {
        // For local storage, the path is already a resolvable file path
        return iconInfo.path
    }
}
