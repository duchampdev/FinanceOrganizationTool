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

package com.github.duchampdev.android.fot.frontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.bdo.TransactionItem
import com.github.duchampdev.android.fot.util.Util

class TransactionAdapter(@NonNull context: Context, @LayoutRes resource: Int, @NonNull objects: List<TransactionItem>) : ArrayAdapter<TransactionItem>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.transaction_item, parent, false)

        val secondParty: TextView = itemView.findViewById(R.id.transaction_otherparty)
        val amount: TextView = itemView.findViewById(R.id.transaction_amount)
        val title: TextView = itemView.findViewById(R.id.transaction_title)
        val category: TextView = itemView.findViewById(R.id.transaction_category)
        val date: TextView = itemView.findViewById(R.id.transaction_date)

        val currentItem = getItem(position)!!

        secondParty.text = currentItem.secondParty
        amount.text =  Util.formatMoney(currentItem.amount, withSymbol = true)
        title.text = currentItem.title
        category.text = currentItem.category.name
        date.text = Util.formatDate(currentItem.date)

        val amountColor = when(currentItem.category.direction) {
            Category.OUTGOING -> Util.getColorForThemeFromAttr(context, R.attr.fot_red)
            Category.INCOMING -> Util.getColorForThemeFromAttr(context, R.attr.fot_green)
            else -> -1 // error
        }
        amount.setTextColor(amountColor)

        return itemView
    }

}