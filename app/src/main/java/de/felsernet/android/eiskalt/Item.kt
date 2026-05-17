package de.felsernet.android.eiskalt

import java.io.Serializable
import kotlin.random.Random

data class Item(
    override var name: String,
    override var id: String = "",
    override var icon: IconInfo? = null,
    var quantity: Int = 1,
    var groupId: String? = null,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", null, 0, null, "")
}
