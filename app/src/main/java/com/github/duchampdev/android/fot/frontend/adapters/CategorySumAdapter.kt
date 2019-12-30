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

package com.github.duchampdev.android.fot.frontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.bdo.CategorySumItem
import com.github.duchampdev.android.fot.util.Util

class CategorySumAdapter(context: Context, @LayoutRes resource: Int, items: List<CategorySumItem>): ArrayAdapter<CategorySumItem>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView
                ?: (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.categorysum_item, parent, false)
        val categoryView: TextView = itemView.findViewById(R.id.categorysum_category)
        val sumView: TextView = itemView.findViewById(R.id.categorysum_sum)

        val currentItem = getItem(position)!!
        categoryView.text = currentItem.category.name
        sumView.text = Util.formatMoney(currentItem.sum, withSymbol = true)

        val color = when(currentItem.sum) {
            0f.toDouble() -> context.resources.getColor(android.R.color.secondary_text_light_nodisable, null)
            else -> when(currentItem.category.direction) {
                Category.INCOMING -> Util.getColorForThemeFromAttr(context, R.attr.fot_green)
                Category.OUTGOING -> Util.getColorForThemeFromAttr(context, R.attr.fot_red)
                else -> -1 // does never occur
            }
        }
        sumView.setTextColor(color)

        return itemView
    }
}