package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.BaseDataClass
import de.felsernet.android.eiskalt.base.IconInfo
import java.io.Serializable

data class Item(
    override var name: String,
    override var id: String = "",
    override var icon: IconInfo? = null,
    var quantity: Int = 1,      // a new item starts with 1
    var groupId: String? = null,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", null, 0, null, "")
}
