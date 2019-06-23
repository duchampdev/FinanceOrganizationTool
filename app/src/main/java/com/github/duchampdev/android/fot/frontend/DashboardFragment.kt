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

import android.app.AlertDialog
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.frontend.adapters.TransactionAdapter
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import java.io.Serializable
import java.util.*

class DashboardFragment: Fragment() {
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var dbInstance: FinanceOrgaToolDB

    private val years: Array<String> = (2018..Calendar.getInstance().get(Calendar.YEAR)).reversed().map { year -> year.toString() }.toTypedArray()

    private var currentMonthZeroBased = 0
    private var currentYear = 0


    companion object {
        @JvmStatic fun newInstance(): DashboardFragment {
            val args = Bundle()
            val fragment = DashboardFragment()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onResume() {
        super.onResume()
        reloadTransactions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        dbInstance = FinanceOrgaToolDB.getInstance(view.context)

        val monthAdapter = ArrayAdapter.createFromResource(view.context, R.array.months, android.R.layout.simple_spinner_item)
                .also { mA -> mA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val yearAdapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, years)
                .also { yA -> yA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        view.dashboard_month.adapter = monthAdapter
        view.dashboard_year.adapter = yearAdapter

        currentMonthZeroBased = Calendar.getInstance().get(Calendar.MONTH)
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        view.dashboard_month.setSelection(currentMonthZeroBased) // january == 0 -> no problem with zero-based indices
        view.dashboard_year.setSelection(0) // current year

        transactionAdapter = TransactionAdapter(view.context, R.layout.transaction_item, ArrayList())
        view.dashboard_positions.adapter = transactionAdapter

        setUpCallbacks(view)
        reloadTransactions()

        return view
    }

    fun addNewTransaction() {
        val transactionBundle = Bundle()
        transactionBundle.putSerializable("categories", dbInstance.categories as Serializable)

        val tdf = TransactionDialogFragment()
        tdf.arguments = transactionBundle
        tdf.registerTransactionDialogCallbacks { item ->
            dbInstance.insertOrUpdate(item)
            reloadTransactions()
        }

        tdf.show(fragmentManager!!, "atdf")
    }

    private fun setUpCallbacks(view: View) {
        view.dashboard_month.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentMonthZeroBased = position
                reloadTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentMonthZeroBased = Calendar.getInstance().get(Calendar.MONTH)
                view.dashboard_month.setSelection(currentMonthZeroBased) // prevent nothing being selected
                reloadTransactions()
            }
        }

        view.dashboard_year.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentYear = Calendar.getInstance().get(Calendar.YEAR) - position // current year is first, others follow
                reloadTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                view.dashboard_year.setSelection(0) // prevent nothing being selected
                currentYear = Calendar.getInstance().get(Calendar.YEAR)
                reloadTransactions()
            }
        }

        view.dashboard_positions.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            val transaction = transactionAdapter.getItem(position)
            AlertDialog.Builder(context)
                    .setTitle(resources.getString(R.string.dashboard_transaction_menu))
                    .setPositiveButton(resources.getString(R.string.edit)) { _, _ ->
                        val tdf = TransactionDialogFragment()
                        val transactionBundle = Bundle()
                        transactionBundle.putSerializable("titem", transaction)
                        transactionBundle.putSerializable("categories", dbInstance.categories as Serializable)
                        tdf.arguments = transactionBundle
                        tdf.registerTransactionDialogCallbacks { item ->
                            run {
                                dbInstance.insertOrUpdate(item)
                                reloadTransactions()
                            }
                        }
                        tdf.show(fragmentManager!!, "etdf")
                    }
                    .setNegativeButton(resources.getString(R.string.delete)) { _, _ ->
                        Snackbar.make(view, resources.getString(R.string.dashboard_transaction_really_delete), Snackbar.LENGTH_LONG)
                                .setAction(resources.getString(R.string.delete)) {
                                    dbInstance.remove(transaction)
                                    reloadTransactions()
                                    Snackbar.make(view, resources.getString(R.string.dashboard_transaction_deleted), Snackbar.LENGTH_LONG)
                                            .show()
                                }
                                .setActionTextColor(resources.getColor(R.color.colorCreme, context!!.theme))
                                .show()
                    }
                    .setNeutralButton(resources.getString(R.string.abort)) { _, _ ->  }
                    .create().show()
            return@OnItemLongClickListener true
        }
    }

    private fun reloadTransactions() {
        val transactions = dbInstance.fetchMonth(currentMonthZeroBased, currentYear)
        transactionAdapter.clear()
        transactionAdapter.addAll(transactions)
        transactionAdapter.notifyDataSetChanged()
    }
}