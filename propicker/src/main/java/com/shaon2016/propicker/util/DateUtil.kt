/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.util


import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

fun Date.formattedDate(toFormat: String = "yyyy-mm-dd"): String {
    return try {
        SimpleDateFormat(toFormat, Locale.ENGLISH).format(this)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}