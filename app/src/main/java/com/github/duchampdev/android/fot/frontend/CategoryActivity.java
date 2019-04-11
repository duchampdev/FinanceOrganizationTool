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

package com.github.duchampdev.android.fot.frontend;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.frontend.adapters.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;


public class CategoryActivity extends AppCompatActivity {

    private FinanceOrgaToolDB dbInstance;

    private ListView existingCategoriesList;
    private CategoryAdapter existingCategoriesAdapter;

    private EditText newCategoryName;
    private RadioButton newCategoryIncoming;
    private RadioButton newCategoryOutgoing;
    private Button addNewCategory;

    private RadioGroup newCategoryDirectionRadioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        setTitle(getResources().getString(R.string.catmgmt_activity_title));
        dbInstance = FinanceOrgaToolDB.getInstance(this);

        existingCategoriesList = findViewById(R.id.category_list);
        newCategoryName = findViewById(R.id.category_new_name);
        newCategoryIncoming = findViewById(R.id.category_new_direction_in);
        newCategoryOutgoing = findViewById(R.id.category_new_direction_out);
        addNewCategory = findViewById(R.id.category_new_add);
        newCategoryDirectionRadioGroup = findViewById(R.id.category_direction_radiogroup);

        existingCategoriesAdapter = new CategoryAdapter(this, R.layout.category_item, new ArrayList<>());
        existingCategoriesList.setAdapter(existingCategoriesAdapter);
        reloadCategories();

        existingCategoriesList.setOnItemLongClickListener(this::showCategoryMenu);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void reloadCategories() {
        List<Category> categories = dbInstance.getCategories();
        existingCategoriesAdapter.clear();
        existingCategoriesAdapter.addAll(categories);
        existingCategoriesAdapter.notifyDataSetChanged();
    }

    /**
     * onClick method for add button
     * @param view
     */
    public void addNewCategory(View view) {
        if (view == addNewCategory) {
            if (newCategoryName.getText().toString().isEmpty()) {
                Toast.makeText(this, getResources().getString(R.string.catmgmt_enter_name), Toast.LENGTH_LONG).show();
            } else if (!newCategoryIncoming.isChecked() && !newCategoryOutgoing.isChecked()) {
                Toast.makeText(this, getResources().getString(R.string.catmgmt_choose_direction), Toast.LENGTH_LONG).show();
            } else {
                // all valid, persist
                String name = newCategoryName.getText().toString();
                int direction = newCategoryIncoming.isChecked() ? Category.INCOMING : Category.OUTGOING;
                if (dbInstance.insertOrUpdate(new Category(name, direction)) != FinanceOrgaToolDB.INSERT_ERROR) { // only insertion possible here
                    // restore old input state
                    newCategoryName.setText("");
                    newCategoryDirectionRadioGroup.clearCheck();

                    // reload categories and notify user
                    reloadCategories();
                    Toast.makeText(this, getResources().getString(R.string.catmgmt_category_added), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.catmgmt_add_failed), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * method to handle list item long click (options menu for existing categories)
     * @param parent
     * @param view
     * @param position
     * @param id
     * @return
     */
    private boolean showCategoryMenu(AdapterView<?> parent, View view, int position, long id) {
        final Category selected = existingCategoriesAdapter.getItem(position);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(selected.getName());
        dialogBuilder.setNeutralButton(getResources().getString(R.string.abort), (dialog, which) -> {});
        dialogBuilder.setPositiveButton(getResources().getString(R.string.rename), (dialog, which) -> renameCategory(selected));
        dialogBuilder.setNegativeButton(getResources().getString(R.string.delete), (dialog, which) -> removeCategory(selected));
        dialogBuilder.create().show();
        return true;
    }

    private void renameCategory(final Category category) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View renameDialogView = layoutInflater.inflate(R.layout.dialog_edit_category, null);
        EditText categoryName = renameDialogView.findViewById(R.id.category_edit_name);
        categoryName.setText(category.getName());

        AlertDialog.Builder renameDialogBuilder = new AlertDialog.Builder(this);
        renameDialogBuilder.setView(renameDialogView);
        renameDialogBuilder.setTitle(getResources().getString(R.string.catmgmt_rename_category));
        renameDialogBuilder.setPositiveButton(getResources().getString(R.string.save), (dialog, which) -> {
            if(categoryName.getText().toString().isEmpty()) {
                Toast.makeText(this, getResources().getString(R.string.catmgmt_enter_name), Toast.LENGTH_LONG).show();
            } else {
                category.setName(categoryName.getText().toString());
                dbInstance.insertOrUpdate(category);
                reloadCategories();
                Toast.makeText(this, getResources().getString(R.string.catmgmt_category_renamed), Toast.LENGTH_LONG).show();
            }
        });
        renameDialogBuilder.setNegativeButton(getResources().getString(R.string.abort), (dialog, which) -> {});
        renameDialogBuilder.create().show();
    }

    private void removeCategory(final Category category) {
        AlertDialog.Builder removeDialogBuilder = new AlertDialog.Builder(this);
        removeDialogBuilder.setTitle(getResources().getString(R.string.catmgmt_really_delete));
        removeDialogBuilder.setMessage(getResources().getString(R.string.catmgmt_really_delete_explanation));
        removeDialogBuilder.setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
            dbInstance.remove(category);
            reloadCategories();
            Toast.makeText(this, getResources().getString(R.string.catmgmt_category_deleted), Toast.LENGTH_LONG).show();
        });
        removeDialogBuilder.setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> {});
        removeDialogBuilder.create().show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // show back arrow
        onBackPressed();
        return true;
    }
}
