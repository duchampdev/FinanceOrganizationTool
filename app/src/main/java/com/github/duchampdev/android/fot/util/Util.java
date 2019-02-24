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

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.graphics.ColorUtils;
import android.util.TypedValue;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.stream.IntStream;


public class Util {

    public static String formatMoney(double val) {
        val = ((double) Math.round(val * 100)) / 100;
        return new DecimalFormat("#0.00" + Currency.getInstance(Locale.GERMANY).getSymbol()).format(val);
    }

    public static double formatMoney(String val) throws NumberFormatException {
        val = val.replace(Currency.getInstance(Locale.GERMANY).getSymbol(), "");
        val = val.replace(",", ".");
        return Double.parseDouble(val);
    }

    private static String formatDate(int year, int monthZeroBased, int dayOfMonth) {
        return String.format("%02d.%02d.%04d", dayOfMonth, monthZeroBased + 1, year);
    }

    public static String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return formatDate(cal);
    }

    public static String formatDate(Calendar cal) {
        return formatDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }


    public static int getColorForThemeFromAttr(Context context, int attr) {
        Resources.Theme theme = context.getTheme();
        TypedValue resVal = new TypedValue();
        boolean wasResolved = theme.resolveAttribute(attr, resVal, true);
        if (wasResolved && resVal.resourceId != 0) {
            return context.getColor(resVal.resourceId);
        } else {
            return android.R.color.darker_gray; // fallback
        }
    }

    public static int[] getStatsDiagramColors(int count) {
        int step = 360 / count;
        int[] colors = new int[count];
        IntStream.range(0, count).forEach(i -> colors[i] = ColorUtils.HSLToColor(new float[] {step*i, .5f, .55f }));
        return colors;
    }
}
