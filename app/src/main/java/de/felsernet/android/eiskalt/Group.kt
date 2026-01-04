package de.felsernet.android.eiskalt

import java.io.Serializable

data class Group(
    var name: String,
    var id: Long = 0
) : Serializable {

    constructor() : this("")
}
