package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class Item(
    var name: String,
    var id: String = "",
    var quantity: Int = Random.nextInt(0, 11),
    var groupId: String? = null
) : Serializable {

    constructor() : this("", "", 0, null)
}
