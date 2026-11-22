package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.BaseDataClass
import de.felsernet.android.eiskalt.base.IconInfo
import java.io.Serializable
import java.util.Date

data class Item(
    override var name: String,
    override var id: String = "",
    override var icon: IconInfo? = null,
    var quantity: Int = 1,      // a new item starts with 1
    var groupId: String? = null,
    var createdAt: Date? = Date(),  // a new item is created today, can be changed by the user
    var deletedAt: Date? = null,    // only set in lists/<id>/deleted (recently deleted items)
    override var comment: String = ""
) : Serializable, BaseDataClass {

    // used by Firestore: items saved before the creation date existed have none (null)
    constructor() : this("", "", null, 0, null, null, null, "")
}
