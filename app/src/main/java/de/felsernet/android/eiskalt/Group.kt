package de.felsernet.android.eiskalt

import java.io.Serializable

data class Group(
    var name: String,
    var id: String = "",
    var comment: String = ""
) : Serializable {

    constructor() : this("", "", "")
}
