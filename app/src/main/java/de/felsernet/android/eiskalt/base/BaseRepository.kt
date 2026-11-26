package de.felsernet.android.eiskalt.base

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import de.felsernet.android.eiskalt.common.NetworkMonitor
import de.felsernet.android.eiskalt.household.HouseholdRepository
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.tasks.await

/**
 * Base repository class that provides common database operations and statistics management.
 * All concrete repositories should extend this class.
 *
 * @param T The data type this repository handles
 * @param collectionPath The path to the Firestore collection within the active household
 *   (can be simple like "lists" or nested like "lists/<listId>/items")
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

        /**
         * Where to read from: offline directly from the local cache
         * (otherwise Firestore tries the server first, which takes seconds)
         */
        fun readSource(): Source = if (NetworkMonitor.isOnline.value) Source.DEFAULT else Source.CACHE
    }

    // Firebase Firestore instance
    protected val db = FirebaseFirestore.getInstance()

    // Collection reference in the active household, e.g. "lists" -> "households/<id>/lists"
    // (checked and normalized by FirestorePath). Determined on each access,
    // so the repositories always use the household that is active at that time.
    protected val collectionRef: CollectionReference
        get() {
            val household = HouseholdRepository.activeHousehold
                ?: throw IllegalStateException("No active household")
            return db.collection(FirestorePath.householdCollectionPath(household.id, collectionPath))
        }

    init {
        // Add this instance to the global list
        repositoryInstances.add(this)
    }

    /**
     * Get all objects from the collection
     */
    open suspend fun getAll(): List<T> {
        val querySnapshot = collectionRef.get(readSource()).await()
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

        collectionRef.document(obj.id).set(obj)
            .logFailure("save", obj.id)
            .trackPending(PendingWrites.Operation.SAVE, obj.name)
        writeOperations++
    }

    /**
     * Delete an object from the collection; the name is only shown in the pending changes
     */
    open suspend fun delete(id: String, name: String = id) {
        collectionRef.document(id).delete()
            .logFailure("delete", id)
            .trackPending(PendingWrites.Operation.DELETE, name)
        writeOperations++
    }

    /**
     * Update an existing object in the collection
     */
    open suspend fun update(obj: T) {
        collectionRef.document(obj.id).set(obj)
            .logFailure("update", obj.id)
            .trackPending(PendingWrites.Operation.UPDATE, obj.name)
        writeOperations++
    }

    /**
     * Whether the writes are listed in the pending changes (not for data the app writes on its own)
     */
    protected open val showPendingWrites = true

    private fun Task<Void>.trackPending(operation: PendingWrites.Operation, name: String) {
        if (showPendingWrites)
            PendingWrites.track(this, operation, name)
    }

    /**
     * Writes are not awaited: Firestore applies them to the local cache at once and sends them
     * to the server when online (waiting for the server would block offline).
     * So a failure is only known later and is logged.
     */
    protected fun Task<Void>.logFailure(operation: String, id: String): Task<Void> =
        addOnFailureListener { e -> AppLog.e("db", "Failed to $operation $collectionPath/$id", e) }

    /**
     * Get an object by its ID
     */
    open suspend fun getById(objectId: String): T? {
        val doc = collectionRef.document(objectId).get(readSource()).await()
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
     * Like count(), but offline the objects in the local cache are counted
     * (includes own changes that are not synced yet)
     */
    suspend fun countOrCached(): Int {
        if (NetworkMonitor.isOnline.value) {
            try {
                return count()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw e
            }
        }
        return collectionRef.get(Source.CACHE).await().size()
    }

    /**
     * Get the collection reference for this repository
     */
    fun getCollectionReference() = collectionRef
}
