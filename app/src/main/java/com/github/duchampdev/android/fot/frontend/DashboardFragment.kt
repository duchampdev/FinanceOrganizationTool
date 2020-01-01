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

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.bdo.TransactionItem
import com.github.duchampdev.android.fot.frontend.adapters.TransactionAdapter
import com.github.duchampdev.android.fot.util.NoFilterArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        val monthAdapter = NoFilterArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item, arrayListOf(*context!!.resources.getStringArray(R.array.months)))
                .also { mA -> mA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val yearAdapter = NoFilterArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item, arrayListOf(*years))
                .also { yA -> yA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        view.dashboard_month.setAdapter(monthAdapter)
        view.dashboard_year.setAdapter(yearAdapter)

        currentMonthZeroBased = Calendar.getInstance().get(Calendar.MONTH)
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        view.dashboard_month.setText(monthAdapter.getItem(currentMonthZeroBased)) // january == 0 -> no problem with zero-based indices
        view.dashboard_year.setText(currentYear.toString()) // current year

        transactionAdapter = TransactionAdapter(view.context, R.layout.transaction_item, ArrayList())
        view.dashboard_positions.adapter = transactionAdapter

        setUpCallbacks(view)
        reloadTransactions()

        return view
    }

    fun addNewTransaction() {
        val transactionBundle = Bundle()
        transactionBundle.putSerializable("categories", dbInstance.getCategories() as Serializable)

        val tdf = TransactionDialogFragment()
        tdf.arguments = transactionBundle
        tdf.registerTransactionDialogCallbacks(object: TransactionDialogFragment.TransactionDialogCallbacks {
            override fun dataSaved(item: TransactionItem) {
                dbInstance.insertOrUpdate(item)
                reloadTransactions()
            }
        })

        tdf.show(fragmentManager!!, "atdf")
    }

    private fun setUpCallbacks(view: View) {
        view.dashboard_month.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            currentMonthZeroBased = position
            reloadTransactions()
        }

        view.dashboard_year.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            currentYear = Calendar.getInstance().get(Calendar.YEAR) - position // current year is first, others follow
            reloadTransactions()
        }

        view.dashboard_positions.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            val transaction = transactionAdapter.getItem(position)!!
            MaterialAlertDialogBuilder(context)
                    .setTitle(resources.getString(R.string.dashboard_transaction_menu))
                    .setPositiveButton(resources.getString(R.string.edit)) { _, _ ->
                        val tdf = TransactionDialogFragment()
                        val transactionBundle = Bundle()
                        transactionBundle.putSerializable("titem", transaction)
                        transactionBundle.putSerializable("categories", dbInstance.getCategories() as Serializable)
                        tdf.arguments = transactionBundle
                        tdf.registerTransactionDialogCallbacks(object: TransactionDialogFragment.TransactionDialogCallbacks {
                            override fun dataSaved(item: TransactionItem) {
                                dbInstance.insertOrUpdate(item)
                                reloadTransactions()
                            }
                        })
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
                    .setNeutralButton(resources.getString(android.R.string.cancel)) { _, _ ->  }
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