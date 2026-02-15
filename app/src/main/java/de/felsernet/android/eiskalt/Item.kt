package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class Item(
    override var name: String,
    override var id: String = "",
    override var icon: IconInfo? = null,
    var quantity: Int = Random.nextInt(0, 11),
    var groupId: String? = null,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", null, 0, null, "")
}
