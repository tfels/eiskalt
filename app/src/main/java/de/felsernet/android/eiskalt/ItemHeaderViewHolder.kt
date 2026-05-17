package de.felsernet.android.eiskalt

import android.view.View

class ItemHeaderViewHolder(itemView: View) : BaseViewHolder<Group>(itemView) {
    override fun isSwipeAllowed(): Boolean = false
}
