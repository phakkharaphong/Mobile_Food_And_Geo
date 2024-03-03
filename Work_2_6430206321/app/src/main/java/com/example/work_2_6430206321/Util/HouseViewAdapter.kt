package com.example.work_2_6430206321.Util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.work_2_6430206321.R

class HouseViewAdapter (private val houseItemList: List<HouseItem>) : RecyclerView.Adapter<HouseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.house_template, parent, false)
        return HouseViewHolder(view)
    }

    override fun getItemCount(): Int {
        return houseItemList.size
    }

    override fun onBindViewHolder(viewHolder: HouseViewHolder, currentPage: Int) {
        val viewItem = houseItemList[currentPage]
        viewHolder.bind(viewItem)
    }
}