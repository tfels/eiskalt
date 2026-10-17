package de.felsernet.android.eiskalt.inventory

import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel

class ListViewModel : BaseViewModel<ListInfo>() {

    private lateinit var listRepository: ListRepository
    override val repository get() = listRepository
    override val typeName: String = "list"

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        listRepository = ListRepository()
    }
}