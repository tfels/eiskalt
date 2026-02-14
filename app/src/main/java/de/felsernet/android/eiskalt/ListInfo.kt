package de.felsernet.android.eiskalt

import java.io.Serializable

data class ListInfo(
    override var name: String,
    override var id: String = "",
    override var icon: String? = null,
    var itemCount: Int,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", null, 0, "")
}
