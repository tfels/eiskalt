package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AllListsAdapter(
    private val listInfos: MutableList<ListInfo>,
    private val onClick: (ListInfo) -> Unit
) : RecyclerView.Adapter<AllListsAdapter.ListViewHolder>() {

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_row, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val listInfo = listInfos[position]
        holder.textView.text = listInfo.name
        holder.textViewItemCount.text = "${listInfo.itemCount}"

        holder.itemView.setOnClickListener {
            onClick(listInfo)
        }
    }

    override fun getItemCount(): Int = listInfos.size
}
