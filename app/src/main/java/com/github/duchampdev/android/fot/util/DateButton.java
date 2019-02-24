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

package com.github.duchampdev.android.fot.util;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@SuppressLint("AppCompatCustomView")
public class DateButton extends Button implements View.OnClickListener {

    private final Calendar cal = Calendar.getInstance();
    private final List<DateEventListener> listeners = new ArrayList<>();

    public DateButton(Context context) {
        super(context);
        this.setOnClickListener(this);
        this.setText(Util.formatDate(cal));
    }

    public DateButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(this);
        this.setText(Util.formatDate(cal));
    }

    public DateButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOnClickListener(this);
        this.setText(Util.formatDate(cal));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DateButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.setOnClickListener(this);
        this.setText(Util.formatDate(cal));
    }

    @Override
    public void onClick(View v) {
        DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth); // keep time
            setText(Util.formatDate(cal));
            for (DateEventListener l : listeners) {
                l.onDateChanged(cal.getTime());
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    public Date getDate() {
        return cal.getTime();
    }

    public void setDate(Date date) {
        cal.setTime(date);
        setText(Util.formatDate(cal));
    }

    // not used yet
    public interface DateEventListener {
        void onDateChanged(Date date);
    }

    public void addDateEventListener(DateEventListener l) {
        listeners.add(l);
    }
}
