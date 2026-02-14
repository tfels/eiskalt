package de.felsernet.android.eiskalt

import java.io.Serializable

data class Group(
    override var name: String,
    override var id: String = "",
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", "")
}
