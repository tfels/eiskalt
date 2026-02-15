package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T: BaseDataClass>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewName: TextView = itemView.findViewById(R.id.textViewName)

    open fun bind(holder: BaseViewHolder<T>, obj: T) {
        textViewName.text = obj.name
    }
}
