package de.felsernet.android.eiskalt.inventory

import android.app.Application
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel

class ListViewModel(application: Application) : BaseViewModel<ListInfo>(application) {

    private lateinit var listRepository: ListRepository
    override val repository get() = listRepository
    override val messages = Messages(
        R.string.list_load_error,
        R.string.list_name_empty,
        R.string.list_save_error,
        R.string.list_delete_done,
        R.string.list_delete_error,
    )

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        listRepository = ListRepository()
    }
}