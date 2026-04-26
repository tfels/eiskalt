package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemHeaderViewHolder(itemView: View) : BaseViewHolder<Group>(itemView, iconViewId = null) {
    override fun isSwipeAllowed(): Boolean = false
}
