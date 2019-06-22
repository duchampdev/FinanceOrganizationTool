/*
 * Copyright (c) 2019 duchampdev.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

fun String.nullForEmpty(): String? {
    return if (isEmpty()) null else this
}