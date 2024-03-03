package com.example.work_2_6430206321.Util

import com.ismaeldivita.chipnavigation.ChipNavigationBar

object GlobalBox {
    // put everything we want to save here...
    var savedBottomNavigation: ChipNavigationBar? = null
    var savedHouseListItem: MutableList<HouseItem> = mutableListOf()
    var selectedHouseItem: HouseItem? = null
}