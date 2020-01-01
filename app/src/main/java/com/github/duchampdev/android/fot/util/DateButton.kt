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

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.button.MaterialButton
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("AppCompatCustomView")
class DateButton : MaterialButton, View.OnClickListener {

    private val cal = Calendar.getInstance()
    private val listeners: MutableList<DateEventListener> = ArrayList()
    private var toD: TimeOfDay = TimeOfDay.KEEP

    enum class TimeOfDay { START, END, KEEP }

    constructor(context: Context) : super(context) {
        setOnClickListener(this)
        text = Util.formatDate(cal)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setOnClickListener(this)
        text = Util.formatDate(cal)
    }

    fun setTimeOfDay(toD: TimeOfDay) {
        this.toD = toD
    }


    override fun onClick(v: View?) {
        DatePickerDialog(context, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            when(toD) {
                TimeOfDay.START -> cal.set(year, month, dayOfMonth, 0, 0, 0)
                TimeOfDay.END -> cal.set(year, month, dayOfMonth, 23, 59, 59)
                TimeOfDay.KEEP -> cal.set(year, month, dayOfMonth)
            }
            text = Util.formatDate(cal)
            listeners.forEach { l -> l.onDateChanged(cal.time) }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show()
    }

    fun getDate(): Date {
        return cal.time
    }

    fun setDate(date: Date) {
        cal.time = date
        text = Util.formatDate(cal)
    }


    // not used yet
    interface DateEventListener {
        fun onDateChanged(date: Date)
    }

    fun addDateEventListener(l: DateEventListener) {
        listeners.add(l)
    }
}