package de.felsernet.android.eiskalt.storage

import android.net.Uri
import de.felsernet.android.eiskalt.IconInfo

/**
 * Interface for icon storage operations.
 * Implementations can use local file storage, cloud storage (e.g., Supabase), or other backends.
 */
interface IconStorage {

    /**
     * Stores an icon from a content URI and returns the IconInfo for the stored icon.
     *
     * @param uri The content URI of the image to store
     * @param filePrefix Optional prefix for the generated filename
     * @return IconInfo representing the stored icon, or null if storage failed
     */
    suspend fun storeIcon(uri: Uri, filePrefix: String = ""): IconInfo?

    /**
     * Deletes an icon from storage.
     *
     * @param iconInfo The IconInfo of the icon to delete
     * @return true if deletion was successful or icon didn't exist, false on error
     */
    suspend fun deleteIcon(iconInfo: IconInfo): Boolean

    /**
     * Checks if an icon exists in storage.
     *
     * @param iconInfo The IconInfo to check
     * @return true if the icon exists, false otherwise
     */
    suspend fun iconExists(iconInfo: IconInfo): Boolean

    /**
     * Lists all icons stored in the storage.
     *
     * @return List of IconInfo for all stored icons
     */
    suspend fun listAllIcons(): List<IconInfo>
}
