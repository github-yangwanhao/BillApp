package cn.yangwanhao.billapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DictSpinnerAdapter(
    private val context: Context,
    private val items: MutableList<DictItem> = mutableListOf()
) : BaseAdapter() {

    data class DictItem(
        val id: Long,
        val name: String
    )

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): DictItem = items[position]
    override fun getItemId(position: Int): Long = items[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)
        val textView = view as TextView
        textView.text = items[position].name
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        val textView = view as TextView
        textView.text = items[position].name
        return view
    }
}