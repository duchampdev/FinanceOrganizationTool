/*
 * Copyright (c) 2019-2020 duchampdev.
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

package com.github.duchampdev.android.fot.util

import android.content.Context
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import java.text.DecimalFormat
import java.util.*
import java.util.stream.IntStream
import kotlin.math.roundToInt

class Util {
    companion object {
        private val currencySymbol = Currency.getInstance(Locale.GERMANY).symbol

        fun formatMoney(value: Double, withSymbol: Boolean = false): String {
            val rounded = (value * 100).roundToInt().toDouble() / 100
            return DecimalFormat("#0.00 ${if(withSymbol) currencySymbol else ""}").format(rounded)
        }

        fun formatMoney(value: String): Double {
            val parsable = value.removeSuffix(currencySymbol).replace(",", ".")
            return parsable.toDouble()
        }

        fun formatDate(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
            return String.format("%02d.%02d.%04d", dayOfMonth, monthZeroBased + 1, year)
        }

        fun formatDate(date: Date): String {
            val cal = Calendar.getInstance()
            cal.time = date
            return formatDate(cal)
        }

        fun formatDate(cal: Calendar): String {
            return formatDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }

        fun getStatsDiagramColors(count: Int): IntArray {
            if(count == 0) return IntArray(0)
            val step: Int = 360 / count
            val colors = IntArray(count)
            IntStream.range(0, count).forEach { i -> colors[i] = ColorUtils.HSLToColor(floatArrayOf((step*i).toFloat(), .5f, .55f))}
            return colors
        }
    }
}