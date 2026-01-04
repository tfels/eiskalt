package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class InventoryItem(
    var name: String,
    var id: Long = nextId(),
    var quantity: Int = Random.nextInt(0, 11),
    var groupId: String? = null
) : Serializable {

    constructor() : this("", 0L, 0, null)

    companion object {
        private var counter: Long = 1
        private fun nextId(): Long = counter++

        fun initializeCounter(items: List<InventoryItem>) {
            val maxId = items.maxOfOrNull { it.id } ?: 0
            counter = maxId + 1
        }
    }
}
