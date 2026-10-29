package de.felsernet.android.eiskalt.base

/**
 * Checks and normalizes Firestore paths (no Firestore dependency, so it can be unit tested).
 */
object FirestorePath {

    /**
     * Normalize the path of a collection, e.g. "lists/<listId>/items":
     * leading and trailing slashes are removed.
     *
     * @throws IllegalArgumentException if the path is empty, contains an empty segment
     *   (e.g. "lists//items" for a missing list id) or does not end with a collection
     */
    fun collectionPath(path: String): String {
        val segments = path.trim('/').split('/')
        require(segments.none { it.isBlank() }) { "Empty segment in Firestore path: '$path'" }
        require(segments.size % 2 == 1) { "Firestore path must end with a collection: '$path'" }
        return segments.joinToString("/")
    }
}
