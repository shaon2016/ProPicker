package com.shaon2016.propicker.util

import android.app.Dialog
import android.content.Context
import android.view.View
import com.shaon2016.propicker.R
import kotlinx.android.synthetic.main.dialog_progress.view.*

object D {
    fun showProgressDialog(
        context: Context,
        msg: String,
        isCancelable: Boolean = false
    ): Dialog {

        val v = View.inflate(context, R.layout.dialog_progress, null)

        val d = Dialog(context)
        d.setContentView(v)
        d.setCancelable(isCancelable)

        v.tvMsg.text = msg

        return d
    }
}