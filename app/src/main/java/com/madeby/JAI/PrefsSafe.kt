package com.madeby.JAI

import android.content.SharedPreferences

fun SharedPreferences.safeInt(key: String, defValue: Int): Int {
    return (all[key] as? Number)?.toInt() ?: (all[key] as? String)?.toIntOrNull() ?: defValue
}

fun SharedPreferences.safeLong(key: String, defValue: Long): Long {
    return (all[key] as? Number)?.toLong() ?: (all[key] as? String)?.toLongOrNull() ?: defValue
}

fun SharedPreferences.safeBoolean(key: String, defValue: Boolean): Boolean {
    val v = all[key]
    return when (v) {
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull() ?: defValue
        is Number -> v.toLong() != 0L
        else -> defValue
    }
}

fun SharedPreferences.safeFloat(key: String, defValue: Float): Float {
    return (all[key] as? Number)?.toFloat() ?: (all[key] as? String)?.toFloatOrNull() ?: defValue
}

fun SharedPreferences.safeString(key: String, defValue: String? = null): String? {
    val v = all[key]
    return v?.toString() ?: defValue
}

