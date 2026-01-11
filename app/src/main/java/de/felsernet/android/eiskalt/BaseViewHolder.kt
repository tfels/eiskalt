package de.felsernet.android.eiskalt

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun onBind(holder: BaseViewHolder<T>, obj: T)
}
