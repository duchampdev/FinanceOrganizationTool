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

package com.github.duchampdev.android.fot.frontend.adapters;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.bdo.CategorySumItem;
import com.github.duchampdev.android.fot.util.Util;


public class CategorySumAdapter extends ArrayAdapter<CategorySumItem> {

    private final Context context;
    private final List<CategorySumItem> items;

    public CategorySumAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<CategorySumItem> objects) {
        super(context, resource, objects);
        this.context = context;
        this.items = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View itemView, @NonNull ViewGroup parent) {
        if (itemView == null) {
            itemView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.categorysum_item, parent, false);
        }
        TextView category = itemView.findViewById(R.id.categorysum_category);
        TextView sum = itemView.findViewById(R.id.categorysum_sum);

        CategorySumItem curItem = items.get(position);

        category.setText(curItem.getCategory().getName());
        sum.setText(Util.formatMoney(curItem.getSum()));

        if (curItem.getCategory().getDirection() == Category.INCOMING && curItem.getSum() != 0) {
            sum.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_green));
        } else if (curItem.getSum() != 0) {
            sum.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_red));
        } else {
            sum.setTextColor(context.getResources().getColor(android.R.color.secondary_text_light_nodisable, null));
        }


        return itemView;
    }
}
