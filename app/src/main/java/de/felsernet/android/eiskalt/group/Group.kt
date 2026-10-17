package de.felsernet.android.eiskalt.group

import de.felsernet.android.eiskalt.base.BaseDataClass
import de.felsernet.android.eiskalt.base.IconInfo
import java.io.Serializable

data class Group(
    override var name: String,
    override var id: String = "",
    override var icon: IconInfo? = null,
    override var comment: String = ""
) : Serializable, BaseDataClass {

    constructor() : this("", "", null, "")
}
