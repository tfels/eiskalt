package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class InventoryItem(
    val name: String,
    val id: Long = System.currentTimeMillis(),
    val quantity: Int = Random.nextInt(0, 11)
) : Serializable
