package de.felsernet.android.eiskalt

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T: BaseDataClass>(itemView: View,
                                                private val onClick: ((item: T) -> Unit)? = null,
                                                private val onLongClick: ((item: T) -> Boolean)? = null,
                                                @IdRes private val iconViewId: Int? = R.id.imageViewIcon
) : RecyclerView.ViewHolder(itemView) {
    private val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    private val imageViewIcon: ImageView? = iconViewId?.let {
        val view = itemView.findViewById<ImageView>(it)
        requireNotNull(view) {
            "IconView with ID $it was declared as mandatory but was not found in layout of ${this.javaClass.simpleName}"
        }
        view
    }

    open fun bind(obj: T) {
        textViewName.text = obj.name
        bindIcon(obj.icon)

        onClick?.let { callback ->
            itemView.setOnClickListener {
                // we do not use the parameter "position",
                // because it is correct when onBindViewHolder is called
                // but when the onClick gets called it might be outdated
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback(obj)
                }
            }
        }

        onLongClick?.let { callback ->
            itemView.setOnLongClickListener { view ->
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback(obj)
                } else {
                    false
                }
            }
        }
    }

    /**
     * Load and bind an icon to the ImageView
     */
    protected fun bindIcon(iconInfo: IconInfo?) {
        if(imageViewIcon == null)
            return

        if(iconInfo == null) {
            imageViewIcon.visibility = View.GONE
            return
        }

        // Load icon asynchronously - visibility is handled by the image loading library
        IconUtils.loadAndSetIconAsync(iconInfo, imageViewIcon)
    }
}
