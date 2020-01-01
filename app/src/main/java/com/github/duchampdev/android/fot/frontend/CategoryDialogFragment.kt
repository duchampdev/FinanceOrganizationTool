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

package com.github.duchampdev.android.fot.frontend

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.bdo.Category
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_category.*

class CategoryDialogFragment: DialogFragment() {

    private val listeners: MutableList<CategoryDialogCallbacks> = ArrayList()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogTitle = resources.getString(R.string.catmgmt_add_category)
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_category, null)

        return MaterialAlertDialogBuilder(activity)
                .setTitle(dialogTitle)
                .setPositiveButton(resources.getString(R.string.save)) { _, _ -> saveCategory() }
                .setNeutralButton(resources.getString(R.string.abort)) { _, _ -> }
                .setView(view)
                .create()
    }

    override fun onResume() { // set specific save routine - do not exit on error
        super.onResume()
        (dialog as AlertDialog?)
                ?.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setOnClickListener { saveCategory() }
    }

    private fun saveCategory() {
        val dbInstance = FinanceOrgaToolDB.getInstance(context!!)
        if (dialog!!.category_new_name.text.toString().isEmpty()) {
            Toast.makeText(context!!, resources.getString(R.string.catmgmt_enter_name), Toast.LENGTH_LONG).show()
        } else if (!dialog!!.category_new_direction_in.isChecked && !dialog!!.category_new_direction_out.isChecked) {
            Toast.makeText(context!!, resources.getString(R.string.catmgmt_choose_direction), Toast.LENGTH_LONG).show()
        } else {
            // all valid, persist
            val name = dialog!!.category_new_name.text.toString()
            val direction = if (dialog!!.category_new_direction_in.isChecked) Category.INCOMING else Category.OUTGOING
            if (dbInstance.insertOrUpdate(Category(name, direction)) != FinanceOrgaToolDB.INSERT_ERROR.toLong()) { // only insert possible here
                Toast.makeText(context!!, resources.getString(R.string.catmgmt_category_added), Toast.LENGTH_LONG).show()

                listeners.forEach { l -> l.dataSaved(dbInstance.getCategoryForName(name)!!)}
                dismiss()
            } else {
                Toast.makeText(context!!, resources.getString(R.string.catmgmt_add_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun registerCategoryDialogCallbacks(c: CategoryDialogCallbacks) = listeners.add(c)

    interface CategoryDialogCallbacks {
        fun dataSaved(category: Category)
    }
}