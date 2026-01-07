package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Base repository class that provides common database operations and statistics management.
 * All concrete repositories should extend this class.
 *
 * @param T The data type this repository handles
 * @param collectionPath The path to the Firestore collection (relative to "inventory_lists" root)
 * @param dataClass The Class object for type T, used for Firestore serialization/deserialization
 */
abstract class BaseRepository<T : Any>(
    protected val collectionPath: String,
    protected val dataClass: Class<T>
) {

    companion object {
        // Global statistics tracking
        var readOperations: Int = 0
        var writeOperations: Int = 0

        // List of all repository instances for easier statistics retrieval
        private val repositoryInstances = mutableListOf<BaseRepository<*>>()

        /**
         * Get total read operations across all repositories
         */
        fun getTotalReadOperations(): Int = readOperations

        /**
         * Get total write operations across all repositories
         */
        fun getTotalWriteOperations(): Int = writeOperations

        /**
         * Reset statistics counters (useful for testing)
         */
        fun resetStatistics() {
            readOperations = 0
            writeOperations = 0
        }

        /**
         * Get all repository instances
         */
        fun getAllRepositories(): List<BaseRepository<*>> = repositoryInstances.toList()
    }

    // Firebase Firestore instance
    protected val db = FirebaseFirestore.getInstance()

    // base path, for future use we might set a user group like this:
    // private val baseDbRef = db.collection("userGroup").document("default")
    private val baseDbRef = db;

    // Specific collection reference based on the provided path
    protected val collectionRef by lazy {
        baseDbRef.collection(collectionPath)
    }

    init {
        // Add this instance to the global list
        repositoryInstances.add(this)
    }

    /**
     * Get all objects from the collection
     */
    open suspend fun getAll(): List<T> {
        val querySnapshot = collectionRef.get().await()
        readOperations++
        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(dataClass)
            // optionally set id from document id?
            //document.toObject(dataClass)?.let { obj ->
            //    setObjectId(obj, document.id)
            //}
        }
    }

    /**
     * Save an object to the collection
     * If the object has an empty ID, a new document will be created
     */
    open suspend fun save(obj: T) {
        val objWithId = ensureObjectHasId(obj)
        collectionRef.document(getObjectId(objWithId)).set(objWithId).await()
        writeOperations++
    }

    /**
     * Delete an object from the collection
     */
    open suspend fun delete(id: String) {
        collectionRef.document(id).delete().await()
        writeOperations++
    }

    /**
     * Update an existing object in the collection
     */
    open suspend fun update(obj: T) {
        collectionRef.document(getObjectId(obj)).set(obj).await()
        writeOperations++
    }

    /**
     * Get an object by its ID
     */
    open suspend fun getById(objectId: String): T? {
        val doc = collectionRef.document(objectId).get().await()
        readOperations++
        return doc.toObject(dataClass)
    }

    /**
     * Count objects in the collection without fetching them
     */
    open suspend fun count(): Int {
        val countQuery = collectionRef.count()
        val snapshot = countQuery.get(com.google.firebase.firestore.AggregateSource.SERVER).await()
        readOperations++
        return snapshot.count.toInt()
    }

    /**
     * Ensure the object has an ID. If not, generate a new one.
     */
    protected fun ensureObjectHasId(obj: T): T {
        val currentId = getObjectId(obj)
        if (currentId.isBlank()) {
            val newId = collectionRef.document().id
            return setObjectId(obj, newId)
        }
        return obj
    }

    /**
     * Get the ID of an object. Must be implemented by concrete classes.
     */
    protected abstract fun getObjectId(obj: T): String

    /**
     * Set the ID of an object. Must be implemented by concrete classes.
     */
    protected abstract fun setObjectId(obj: T, id: String): T

    /**
     * Get the collection reference for this repository
     */
    fun getCollectionReference() = collectionRef
}
