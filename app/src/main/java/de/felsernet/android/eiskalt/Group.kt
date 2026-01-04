package de.felsernet.android.eiskalt

import java.io.Serializable

data class Group(
    var name: String,
    var id: Long = 0,
    var comment: String = ""
) : Serializable {

    constructor() : this("", 0, "")
}
