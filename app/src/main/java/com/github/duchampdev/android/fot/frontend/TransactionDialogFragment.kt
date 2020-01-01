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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.backend.SecondpartyAutocompleteService
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.bdo.TransactionItem
import com.github.duchampdev.android.fot.util.NoFilterArrayAdapter
import com.github.duchampdev.android.fot.util.Util
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_transaction.*
import kotlinx.android.synthetic.main.dialog_transaction.view.*

class TransactionDialogFragment: DialogFragment() {

    private val listeners: MutableList<TransactionDialogCallbacks> = ArrayList()

    private lateinit var editable: TransactionItem
    private lateinit var categories: List<Category>
    private lateinit var categorySpinnerAdapter: ArrayAdapter<Category>
    private lateinit var autocompleteService: SecondpartyAutocompleteService


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogTitle = if (arguments?.containsKey("titem") == true) resources.getString(R.string.tdialog_edit_transaction) else resources.getString(R.string.tdialog_add_transaction)
        
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_transaction, null)
        
        return MaterialAlertDialogBuilder(activity)
                .setTitle(dialogTitle)
                .setPositiveButton(resources.getString(R.string.save)) { _, _ -> saveTransaction() } // callback implemented later
                .setNeutralButton(resources.getString(R.string.abort)) { _, _ -> } // callback implemented later
                .setView(view)
                .create()
                .also { onViewCreated(view, savedInstanceState) }
    }


    override fun onResume() { // set specific save routine - do not exit on error
        super.onResume()
        (dialog as AlertDialog?)
                ?.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setOnClickListener { saveTransaction() }
    }
    
    private fun saveTransaction() {
        val dbInstance = FinanceOrgaToolDB.getInstance(context!!)
        val selectedCategory =  dbInstance.getCategoryForName(dialog!!.tdialog_category.text.toString())
        when {
            dialog!!.tdialog_secondparty.text.toString().isEmpty() -> {
                activity!!.runOnUiThread { Toast.makeText(context, resources.getString(R.string.tdialog_secondparty_missing), Toast.LENGTH_LONG).show() }
            }
            dialog!!.tdialog_amount.text.toString().isEmpty() -> {
                activity!!.runOnUiThread { Toast.makeText(context, resources.getString(R.string.tdialog_amount_missing), Toast.LENGTH_LONG).show() }
            }
            selectedCategory == null -> {
                activity!!.runOnUiThread { Toast.makeText(context, resources.getString(R.string.tdialog_category_missing), Toast.LENGTH_LONG).show() }
            }
            else -> {
                // check amount format
                try {
                    Util.formatMoney(dialog!!.tdialog_amount.text.toString())
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, resources.getString(R.string.tdialog_enter_valid_amount), Toast.LENGTH_SHORT).show()
                    return  // must break up, can't recover from this issue
                }

                // data is valid - persist
                if (! ::editable.isInitialized) { // new item
                    editable = TransactionItem(dialog!!.tdialog_secondparty.text.toString(), Util.formatMoney(dialog!!.tdialog_amount.text.toString()), dialog!!.tdialog_title.text.toString(), selectedCategory, dialog!!.tdialog_date.getDate())
                } else {
                    editable.secondParty = dialog!!.tdialog_secondparty.text.toString()
                    editable.amount = Util.formatMoney(dialog!!.tdialog_amount.text.toString())
                    editable.title = dialog!!.tdialog_title.text.toString()
                    editable.date = dialog!!.tdialog_date.getDate()
                    editable.category = selectedCategory
                }

                listeners.forEach { l -> l.dataSaved(editable) }
                dismiss() // only called if input was valid
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categorySpinnerAdapter = NoFilterArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, ArrayList())
        view.tdialog_category.setAdapter(categorySpinnerAdapter)

        view.tdialog_amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if(!hasFocus) {
                // format currency correctly after field becomes unfocused
                try {
                    val amount = Util.formatMoney(view.tdialog_amount.text.toString())
                    view.tdialog_amount.setText(Util.formatMoney(amount))
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, resources.getString(R.string.tdialog_enter_valid_amount), Toast.LENGTH_SHORT).show()
                }
            }
        }

        autocompleteService = SecondpartyAutocompleteService.getInstance(view.tdialog_secondparty.context!!)
        view.tdialog_secondparty.setAdapter(ArrayAdapter(view.tdialog_secondparty.context!!, android.R.layout.simple_spinner_dropdown_item, autocompleteService.getProposals()))

        if(arguments?.containsKey("categories") == true) {
            categories = arguments!!.get("categories")!! as List<Category>
            categorySpinnerAdapter.clear()
            categorySpinnerAdapter.addAll(categories)
            categorySpinnerAdapter.notifyDataSetChanged()

            if(arguments?.containsKey("titem") == true) {
                editable = arguments!!.get("titem")!! as TransactionItem

                view.tdialog_secondparty.setText(editable.secondParty)
                view.tdialog_amount.setText(Util.formatMoney(editable.amount))
                view.tdialog_title.setText(editable.title)
                view.tdialog_date.setDate(editable.date)
                view.tdialog_category.setText(editable.category.toString())
            }
        }
    }


    fun registerTransactionDialogCallbacks(c: TransactionDialogCallbacks) = listeners.add(c)

    interface TransactionDialogCallbacks {
        fun dataSaved(item: TransactionItem)
    }
}