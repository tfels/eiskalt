package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

open class GenericListAdapter<T: BaseDataClass, VH : BaseViewHolder<T>>(
    private val objectList: MutableList<T>,
    @LayoutRes private val layoutId: Int,
    private val viewHolderFactory: (View) -> VH
) : RecyclerView.Adapter<VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return viewHolderFactory(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(objectList[position])
    }

    override fun getItemCount(): Int = objectList.size
}
