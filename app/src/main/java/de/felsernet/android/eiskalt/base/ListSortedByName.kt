package de.felsernet.android.eiskalt.base

/**
 * Helpers for lists of entities kept sorted by name (ignoring case), without Android or Firestore
 * dependencies, so they can be unit tested.
 * All functions return a new list, the original list is not changed.
 * Lists passed to insertSorted/replaceSorted must already be sorted with sortedByName().
 */

/** Sort by name, ignoring case */
fun <T : BaseDataClass> List<T>.sortedByName(): List<T> = sortedBy { it.name.lowercase() }

/** Index where obj has to be inserted to keep the list sorted */
fun <T : BaseDataClass> List<T>.sortedIndexOf(obj: T): Int {
    return binarySearch {
        it.name.lowercase().compareTo(obj.name.lowercase())
    }.let { if (it < 0) -it - 1 else it }
}

/** Insert obj at its sorted position */
fun <T : BaseDataClass> List<T>.insertSorted(obj: T): List<T> {
    val insertIndex = sortedIndexOf(obj)
    return toMutableList().apply { add(insertIndex, obj) }
}

/** Replace the entry with the id of obj, inserting obj at its (new) sorted position */
fun <T : BaseDataClass> List<T>.replaceSorted(obj: T): List<T> {
    // Item might have been renamed, so remove old version and re-insert at new sorted position
    return deleteById(obj).insertSorted(obj)
}

/** Remove the entry with the id of obj */
fun <T : BaseDataClass> List<T>.deleteById(obj: T): List<T> {
    return filter { it.id != obj.id }
}
