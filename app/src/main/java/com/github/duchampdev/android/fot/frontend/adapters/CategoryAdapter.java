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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.util.Util;

import java.util.List;


public class CategoryAdapter extends ArrayAdapter<Category> {

    private final Context context;
    private final List<Category> items;

    public CategoryAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Category> objects) {
        super(context, resource, objects);
        this.context = context;
        this.items = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View itemView, @NonNull ViewGroup parent) {
        if (itemView == null) {
            itemView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.category_item, parent, false);
        }

        Category curItem = items.get(position);
        boolean isIncoming = curItem.getDirection() == Category.INCOMING;

        TextView name = itemView.findViewById(R.id.category_name);
        TextView direction = itemView.findViewById(R.id.category_direction);

        name.setText(curItem.getName());
        if(isIncoming) {
            name.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_green));
            direction.setText(context.getResources().getString(R.string.income));
        } else {
            name.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_red));
            direction.setText(context.getResources().getString(R.string.expense));
        }

        return itemView;
    }
}
