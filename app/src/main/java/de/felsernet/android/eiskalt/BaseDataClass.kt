package de.felsernet.android.eiskalt

import java.io.Serializable

interface BaseDataClass {
    var id: String
    var name: String
    var icon: IconInfo?
    var comment: String
}

data class IconInfo(
    val assetPath: String,
    val iconName: String
) : Serializable {
    constructor() : this("", "")
}