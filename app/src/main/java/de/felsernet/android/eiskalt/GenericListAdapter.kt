package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class GenericListAdapter<T: BaseDataClass, VH : BaseViewHolder<T>>(
    private val objectList: MutableList<T>,
    @LayoutRes private val layoutId: Int,
    private val viewHolderFactory: (View) -> VH,
    private val onClick: (item: T) -> Unit,
    private val onLongClick: ((item: T) -> Boolean)? = null
) : RecyclerView.Adapter<VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return viewHolderFactory(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, objectList[position])

        holder.itemView.setOnClickListener {
            // we do not use the parameter "position",
            // because it is correct when onBindViewHolder is called
            // but when the onClick gets called it might be outdated
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onClick(objectList[adapterPosition])
            }
        }

        holder.itemView.setOnLongClickListener { view ->
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                return@setOnLongClickListener onLongClick?.invoke(objectList[adapterPosition]) ?: false
            }
            false
        }
    }

    override fun getItemCount(): Int = objectList.size
}
