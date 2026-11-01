package de.felsernet.android.eiskalt.inventory

import android.app.Application
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel

class ListViewModel(application: Application) : BaseViewModel<ListInfo>(application) {

    private lateinit var listRepository: ListRepository
    override val repository get() = listRepository
    override val typeNameRes = R.string.type_list
    override val typeNamePluralRes = R.string.type_lists

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        listRepository = ListRepository()
    }
}