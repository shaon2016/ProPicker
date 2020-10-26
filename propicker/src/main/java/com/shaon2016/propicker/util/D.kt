package com.shaon2016.propicker.util

import android.app.Dialog
import android.content.Context
import android.os.Message
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.app.DialogCompat
import com.shaon2016.propicker.databinding.DialogProgressBinding

object D {
    fun showProgressDialog(
        context: Context,
        msg: String,
        isCancelable: Boolean = false
    ): Dialog {

        val binding = DialogProgressBinding.inflate(LayoutInflater.from(context), null, false)

        val d = Dialog(context)
        d.setContentView(binding.root)
        d.setCancelable(isCancelable)
        d.show()

        binding.tvMsg.text = msg

        return d
    }
}