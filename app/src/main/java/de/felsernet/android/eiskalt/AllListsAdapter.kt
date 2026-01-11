package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AllListsAdapter(
    private val listInfos: MutableList<ListInfo>,
    private val onClick: (ListInfo) -> Unit
) : RecyclerView.Adapter<ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_row, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val listInfo = listInfos[position]
        holder.onBind(holder, listInfo)

        holder.itemView.setOnClickListener {
            onClick(listInfo)
        }
    }

    override fun getItemCount(): Int = listInfos.size
}
