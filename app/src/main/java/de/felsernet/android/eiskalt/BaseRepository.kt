package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Base repository class that provides common database operations and statistics management.
 * All concrete repositories should extend this class.
 *
 * @param T The data type this repository handles
 * @param collectionPath The path to the Firestore collection (can be simple like "collection" or nested like "collection/document/collection")
 * @param dataClass The Class object for type T, used for Firestore serialization/deserialization
 */
abstract class BaseRepository<T : BaseDataClass>(
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
        createCollectionReferenceFromPath(baseDbRef, collectionPath)
    }

    /**
     * Creates a collection reference from a path string that may contain nested segments.
     * Supports paths like "collection" or "collection/document/collection".
     */
    private fun createCollectionReferenceFromPath(baseRef: Any, path: String): com.google.firebase.firestore.CollectionReference {
        val segments = path.split("/")
        var currentRef: Any = baseRef

        for ((index, segment) in segments.withIndex()) {
            if (segment.isBlank()) continue

            if (index % 2 == 0) {   // Even indices (0, 2, 4...) should be collections
                currentRef = if (currentRef is FirebaseFirestore) {
                    currentRef.collection(segment)
                } else {
                    (currentRef as com.google.firebase.firestore.DocumentReference).collection(segment)
                }
            } else {    // Odd indices (1, 3, 5...) should be documents
                currentRef = (currentRef as com.google.firebase.firestore.CollectionReference).document(segment)
            }
        }

        // Ensure we end with a collection reference
        if (currentRef !is com.google.firebase.firestore.CollectionReference) {
            throw IllegalArgumentException("Collection path must end with a collection name: $path")
        }

        return currentRef
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
     * The given obj will contain a valid id after the call
     */
    open suspend fun save(obj: T) {
        //Ensure the object has an ID. If not, generate a new one.
        if (obj.id.isBlank()) {
            obj.id = collectionRef.document().id
        }

        collectionRef.document(obj.id).set(obj).await()
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
        collectionRef.document(obj.id).set(obj).await()
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
     * Get the collection reference for this repository
     */
    fun getCollectionReference() = collectionRef
}
