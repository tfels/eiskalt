package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class InventoryItem(
    var name: String,
    val id: Long = nextId(),
    var quantity: Int = Random.nextInt(0, 11)
) : Serializable {
    companion object {
        private var counter: Long = 1
        private fun nextId(): Long = counter++
    }
}
