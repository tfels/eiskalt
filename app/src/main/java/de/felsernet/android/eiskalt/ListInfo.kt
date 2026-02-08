package de.felsernet.android.eiskalt

data class ListInfo(
    override var name: String,
    override var id: String = "",
    val itemCount: Int
) : BaseDataClass
