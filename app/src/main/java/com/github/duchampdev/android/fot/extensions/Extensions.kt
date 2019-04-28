package com.github.duchampdev.android.fot.extensions

import android.content.Context
import android.util.TypedValue

fun Context.getColorForThemeForAttr(attr: Int): Int {
    val resolvedVal = TypedValue()
    val wasResolved = theme.resolveAttribute(attr, resolvedVal, true)
    return if (wasResolved && resolvedVal.resourceId != 0) {
        getColor(resolvedVal.resourceId)
    } else {
        android.R.color.darker_gray // fallback
    }
}