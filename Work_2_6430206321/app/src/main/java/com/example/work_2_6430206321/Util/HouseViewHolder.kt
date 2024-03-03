package com.example.work_2_6430206321.Util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.work_2_6430206321.R

class HouseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView = view.findViewById<ImageView>(R.id.ivHouse)
    private val imageViewActionButton = view.findViewById<ImageView>(R.id.ivActionBtn)
    private val textViewHouseDetail = view.findViewById<TextView>(R.id.tvHouseDetail)
    private val textViewPrice = view.findViewById<TextView>(R.id.tvPriceTag)
    private val texttype = view.findViewById<TextView>(R.id.tvFoodType)
    private val context = view.context

    fun bind(houseItem: HouseItem) {
        Glide.with(context).load(houseItem.image_url).into(imageView);
        textViewHouseDetail.text = houseItem.location
        textViewPrice.text = houseItem.price
        texttype.text = houseItem.type
        imageViewActionButton.setOnClickListener {
            var clickedData = GlobalBox.savedHouseListItem.first() {

                it.location == houseItem.location
            }
            GlobalBox.selectedHouseItem = clickedData
            GlobalBox.savedBottomNavigation?.setItemSelected(R.id.mapPage)
        }
    }
}