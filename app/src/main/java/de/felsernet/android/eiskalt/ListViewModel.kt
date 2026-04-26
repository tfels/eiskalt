package de.felsernet.android.eiskalt

import kotlinx.coroutines.launch

class ListViewModel : BaseViewModel<ListInfo>() {

    private lateinit var listRepository: ListRepository
    override val repository get() = listRepository
    override val typeName: String = "list"

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        listRepository = ListRepository()
    }
}