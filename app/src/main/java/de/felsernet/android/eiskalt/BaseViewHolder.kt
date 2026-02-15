package de.felsernet.android.eiskalt

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T: BaseDataClass>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    private val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewIcon)

    open fun bind(holder: BaseViewHolder<T>, obj: T) {
        textViewName.text = obj.name
        bindIcon(obj.icon)
    }

    /**
     * Load and bind an icon to the ImageView
     */
    protected fun bindIcon(iconInfo: IconInfo?) {
        if(iconInfo == null) {
            imageViewIcon.visibility = View.GONE
            return
        }

        if (IconUtils.loadAndSetIcon(iconInfo, itemView.context, imageViewIcon)) {
            imageViewIcon.visibility = View.VISIBLE
        } else {
            imageViewIcon.visibility = View.GONE
        }
    }
}
