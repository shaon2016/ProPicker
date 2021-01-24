package com.shaon2016.propicker.util

import android.app.Dialog
import android.content.Context
import android.view.View
import com.shaon2016.propicker.R
import com.shaon2016.propicker.databinding.DialogProgressBinding

object D {
    fun showProgressDialog(
        context: Context,
        msg: String,
        isCancelable: Boolean = false
    ): Dialog {
        val v = View.inflate(context, R.layout.dialog_progress, null)
        val binding = DialogProgressBinding.bind(v)
        val view = binding.root

        val d = Dialog(context)
        d.setContentView(view)
        d.setCancelable(isCancelable)

        binding.tvMsg.text = msg

        return d
    }
}