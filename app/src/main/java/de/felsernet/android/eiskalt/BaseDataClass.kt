package de.felsernet.android.eiskalt

import java.io.Serializable

interface BaseDataClass {
    var id: String
    var name: String
    var icon: IconInfo?
    var comment: String
}

data class IconInfo(
    val type: IconType,
    val path: String,
) : Serializable {
    constructor() : this(IconType.UNKNOWN, "")
}

enum class IconType {
    ASSET,
    UNKNOWN,
}