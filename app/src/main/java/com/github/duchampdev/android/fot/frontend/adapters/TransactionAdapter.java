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

import java.util.List;

import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.TransactionItem;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.util.Util;


public class TransactionAdapter extends ArrayAdapter<TransactionItem> {

    private final Context context;
    private final List<TransactionItem> items;

    public TransactionAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<TransactionItem> objects) {
        super(context, resource, objects);
        this.context = context;
        this.items = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View itemView, @NonNull ViewGroup parent) {
        if (itemView == null) {
            itemView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.transaction_item, parent, false);
        }
        TextView secondParty = itemView.findViewById(R.id.transaction_otherparty);
        TextView amount = itemView.findViewById(R.id.transaction_amount);
        TextView title = itemView.findViewById(R.id.transaction_title);
        TextView category = itemView.findViewById(R.id.transaction_category);
        TextView date = itemView.findViewById(R.id.transaction_date);

        TransactionItem curItem = items.get(position);

        secondParty.setText(curItem.getSecondParty());
        amount.setText(Util.formatMoney(curItem.getAmount()));
        if (curItem.getCategory().getDirection() == Category.OUTGOING) {
            amount.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_red));
        } else if (curItem.getCategory().getDirection() == Category.INCOMING) {
            amount.setTextColor(Util.getColorForThemeFromAttr(context, R.attr.fot_green));
        }
        title.setText(curItem.getTitle());
        category.setText(curItem.getCategory().getName());
        date.setText(Util.formatDate(curItem.getDate()));


        return itemView;
    }
}
