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
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.extensions.getColorForThemeForAttr


class CategoryAdapter(@NonNull context: Context, @LayoutRes resource: Int, @NonNull objects: List<Category>) : ArrayAdapter<Category>(context, resource, objects) {

    private val items: List<Category> = objects

    @NonNull
    override fun getView(position: Int, @Nullable convertView: View?, @NonNull parent: ViewGroup): View {
        val itemView = convertView ?: (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.category_item, parent, false)

        val currentItem = items[position]
        val isIncoming = currentItem.direction == Category.INCOMING

        val name: TextView = itemView.findViewById(R.id.category_name)
        val direction: TextView = itemView.findViewById(R.id.category_direction)

        name.text = currentItem.name
        when(isIncoming) {
            true -> {
                name.setTextColor(context.getColorForThemeForAttr(R.attr.fot_green))
                direction.text = context.resources.getString(R.string.income)
            }
            false -> {
                name.setTextColor(context.getColorForThemeForAttr(R.attr.fot_red))
                direction.text = context.resources.getString(R.string.expense)
            }
        }

        return itemView
    }
}