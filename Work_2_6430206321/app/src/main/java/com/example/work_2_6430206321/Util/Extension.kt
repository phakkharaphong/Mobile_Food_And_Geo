package com.example.work_2_6430206321.Util

import android.app.AlertDialog
import android.app.Dialog
import androidx.fragment.app.Fragment
import com.example.work_2_6430206321.MainActivity
import com.example.work_2_6430206321.R

fun Fragment.getLoading(): Dialog {
    val builder = AlertDialog.Builder((activity as MainActivity))
    builder.setView(R.layout.progress)
    builder.setCancelable(false)
    return builder.create()
}