package com.vpmedia.sdkvbot.example

import android.app.Activity
import android.app.ProgressDialog

private val progressDialogs = mutableMapOf<Activity, ProgressDialog>()

fun Activity.showProgress() {
    if (isFinishing || isDestroyed) return
    val existing = progressDialogs[this]
    if (existing != null && existing.isShowing) return
    val dialog = ProgressDialog(this).apply {
        setMessage("Đang xử lý...")
        setCancelable(false)
        show()
    }
    progressDialogs[this] = dialog
}

fun Activity.hideProgress() {
    progressDialogs.remove(this)?.let {
        if (it.isShowing) it.dismiss()
    }
}
