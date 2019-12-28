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

package com.github.duchampdev.android.fot.frontend

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.frontend.adapters.CategoryAdapter
import kotlinx.android.synthetic.main.activity_category.*

class CategoryActivity : AppCompatActivity() {

    private lateinit var dbInstance: FinanceOrgaToolDB
    private lateinit var existingCategoriesAdapter: CategoryAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        title = resources.getString(R.string.catmgmt_activity_title)
        dbInstance = FinanceOrgaToolDB.getInstance(this)

        existingCategoriesAdapter = CategoryAdapter(this, R.layout.category_item, ArrayList())
        category_list.adapter = existingCategoriesAdapter
        reloadCategories()
        category_list.setOnItemLongClickListener(this::showCategoryMenu)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun reloadCategories() {
        existingCategoriesAdapter.clear()
        existingCategoriesAdapter.addAll(dbInstance.getCategories())
        existingCategoriesAdapter.notifyDataSetChanged()
    }

    fun addNewCategory(view: View) {
        if (view == category_new_add) {
            if (category_new_name.text.toString().isEmpty()) {
                Toast.makeText(this, resources.getString(R.string.catmgmt_enter_name), Toast.LENGTH_LONG).show()
            } else if (!category_new_direction_in.isChecked && !category_new_direction_out.isChecked) {
                Toast.makeText(this, resources.getString(R.string.catmgmt_choose_direction), Toast.LENGTH_LONG).show()
            } else {
                // all valid, persist
                val name = category_new_name.text.toString()
                val direction = if (category_new_direction_in.isChecked) Category.INCOMING else Category.OUTGOING
                if (dbInstance.insertOrUpdate(Category(name, direction)) != FinanceOrgaToolDB.INSERT_ERROR.toLong()) { // only insert possible here
                    // restore old input state
                    category_new_name.setText("")
                    category_direction_radiogroup.clearCheck()

                    // reload categories and notify user
                    reloadCategories()
                    Toast.makeText(this, resources.getString(R.string.catmgmt_category_added), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, resources.getString(R.string.catmgmt_add_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    /**
     * method to handle list item long click (options menu for existing categories)
     */
    private fun showCategoryMenu(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val selectedCategory = existingCategoriesAdapter.getItem(position)!!
        AlertDialog.Builder(this)
                .setTitle(selectedCategory.name)
                .setNeutralButton(resources.getString(R.string.abort), null)
                .setPositiveButton(resources.getString(R.string.rename)) { _, _ -> renameCategory(selectedCategory) }
                .setNegativeButton(resources.getString(R.string.delete)) { _, _ -> removeCategory(selectedCategory) }
                .create().show()
        return true
    }


    private fun renameCategory(category: Category) {
        val renameDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_category, null)
        val categoryName = renameDialogView.findViewById<EditText>(R.id.category_edit_name)
        categoryName.setText(category.name)

        AlertDialog.Builder(this)
                .setView(renameDialogView)
                .setTitle(resources.getString(R.string.catmgmt_rename_category))
                .setPositiveButton(resources.getString(R.string.save)) { _, _ ->
                    if (categoryName.text.toString().isEmpty()) {
                        Toast.makeText(this, resources.getString(R.string.catmgmt_enter_name), Toast.LENGTH_LONG).show()
                    } else {
                        category.name = categoryName.text.toString()
                        dbInstance.insertOrUpdate(category)
                        reloadCategories()
                        Toast.makeText(this, resources.getString(R.string.catmgmt_category_renamed), Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton(resources.getString(R.string.abort), null)
                .create().show()
    }

    private fun removeCategory(category: Category) {
        AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.catmgmt_really_delete))
                .setMessage(resources.getString(R.string.catmgmt_really_delete_explanation))
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    dbInstance.remove(category)
                    reloadCategories()
                    Toast.makeText(this, resources.getString(R.string.catmgmt_category_deleted), Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(resources.getString(R.string.no), null)
                .create().show()
    }

    override fun onSupportNavigateUp(): Boolean {
        // show back arrow
        onBackPressed()
        return true
    }

}