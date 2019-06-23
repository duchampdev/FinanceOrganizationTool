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

package com.github.duchampdev.android.fot.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("AppCompatCustomView")
class DateButton : Button, View.OnClickListener {

    private val cal = Calendar.getInstance()
    private val listeners: MutableList<DateEventListener> = ArrayList()

    constructor(context: Context) : super(context) {
        setOnClickListener(this)
        text = Util.formatDate(cal)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setOnClickListener(this)
        text = Util.formatDate(cal)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setOnClickListener(this)
        text = Util.formatDate(cal)
    }


    override fun onClick(v: View?) {
        DatePickerDialog(context, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(year, month, dayOfMonth) // keep time
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