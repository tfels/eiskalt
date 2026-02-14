package de.felsernet.android.eiskalt

import java.io.Serializable

data class ListInfo(
    override var name: String,
    override var id: String = "",
    var itemCount: Int,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", 0, "")
}
