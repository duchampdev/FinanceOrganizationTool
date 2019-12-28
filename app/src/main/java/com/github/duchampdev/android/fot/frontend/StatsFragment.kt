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
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.bdo.CategorySumItem
import com.github.duchampdev.android.fot.bdo.TransactionItem
import com.github.duchampdev.android.fot.frontend.adapters.CategorySumAdapter
import com.github.duchampdev.android.fot.frontend.adapters.TransactionAdapter
import com.github.duchampdev.android.fot.util.DateButton
import com.github.duchampdev.android.fot.util.Util
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlinx.android.synthetic.main.fragment_stats.view.*
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

class StatsFragment: Fragment() {

    private lateinit var dbInstance: FinanceOrgaToolDB
    private lateinit var incomingAdapter: CategorySumAdapter
    private lateinit var outgoingAdapter: CategorySumAdapter

    private val inValues: MutableList<CategorySumItem> = ArrayList()
    private val outValues: MutableList<CategorySumItem> = ArrayList()

    private lateinit var fromDate: Date
    private lateinit var untilDate: Date

    companion object {
        fun newInstance(): StatsFragment = StatsFragment().also { f -> f.arguments = Bundle() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        dbInstance = FinanceOrgaToolDB.getInstance(context!!)

        initGUI(view)
        return view
    }

    private fun initGUI(v: View) {
        (v.stats_from as DateButton).addDateEventListener(dateEventListenerForView(v.stats_from))
        v.stats_from.setTimeOfDay(DateButton.TimeOfDay.START)
        (v.stats_until as DateButton).addDateEventListener(dateEventListenerForView(v.stats_until))
        v.stats_until.setTimeOfDay(DateButton.TimeOfDay.END)

        v.stats_show_pie_in.setOnClickListener { showPie(Category.INCOMING) }
        v.stats_show_pie_out.setOnClickListener { showPie(Category.OUTGOING) }

        incomingAdapter = CategorySumAdapter(context!!, R.layout.categorysum_item, ArrayList())
        outgoingAdapter = CategorySumAdapter(context!!, R.layout.categorysum_item, ArrayList())

        v.stats_in_list.adapter = incomingAdapter
        v.stats_out_list.adapter = outgoingAdapter

        val cal = Calendar.getInstance()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0)
        v.stats_from.text = Util.Companion.formatDate(cal)
        fromDate = cal.time
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        v.stats_until.text = Util.formatDate(cal)
        untilDate = cal.time

        v.stats_in_list.setOnItemClickListener { parent, view, position, id -> showCategoryTransactionsDialog(incomingAdapter.getItem(position)!!.category) }
        v.stats_out_list.setOnItemClickListener { parent, view, position, id -> showCategoryTransactionsDialog(outgoingAdapter.getItem(position)!!.category) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadCategorySums()
    }

    private fun showCategoryTransactionsDialog(category: Category) {
        val transactions = dbInstance.fetchTransactionsBetweenForCategory(fromDate, untilDate, category)

        AlertDialog.Builder(context!!)
                .setTitle(category.name)
                .setAdapter(TransactionAdapter(context!!, R.layout.transaction_item, transactions), null)
                .setNegativeButton(resources.getString(R.string.close)) { _, _ -> }
                .create()
                .show()
    }

    private fun dateEventListenerForView(v: View): DateButton.DateEventListener {
        return object : DateButton.DateEventListener {
            override fun onDateChanged(date: Date) {
                when(v) {
                    stats_until -> {
                        untilDate = date
                    }
                    stats_from -> {
                        fromDate = date
                        val cal = Calendar.getInstance()
                        cal.time = date
                        if(cal.get(Calendar.DAY_OF_MONTH) == 1 && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context!!.resources.getString(R.string.pref_key_statsrange), true)) {
                            // the user likely wants to show at least a whole month, so also adjust until, if allowed
                            val end = Calendar.getInstance()
                            end.time = date
                            end.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            untilDate = end.time
                            stats_until.setDate(end.time)
                        }
                    }
                }
                reloadCategorySums()
            }
        }
    }

    private fun showPie(direction: Int) {
        val chartView = activity!!.layoutInflater.inflate(R.layout.dialog_chart, null)
        val pieChart: PieChart = chartView.findViewById(R.id.dialog_chart_piechart)

        val entries: MutableList<PieEntry> = ArrayList()
        val rawData: List<CategorySumItem> = when(direction) {
            Category.INCOMING -> inValues
            Category.OUTGOING -> outValues
            else -> throw IllegalArgumentException()
        }
        var fullSum = rawData.sumByDouble { csi -> csi.sum }

        rawData.filter { csi -> csi.sum > 0 }
                .forEach { csi -> entries.add(PieEntry((csi.sum / fullSum * 100.0).toFloat(), csi.category.name)) }

        val piePreData = PieDataSet(entries, "")
        piePreData.setColors(*Util.getStatsDiagramColors(rawData.size))

        val pieData = PieData(piePreData)
        pieData.setValueTextSize(16f)
        pieChart.setUsePercentValues(true)
        pieData.setValueFormatter(PercentFormatter(pieChart, false))

        if(entries.isNotEmpty()) pieChart.data = pieData

        pieChart.description.text = ""
        pieChart.animateY(500)
        pieChart.legend.textSize = 16f
        pieChart.legend.isWordWrapEnabled = true
        pieChart.setNoDataText(resources.getString(R.string.stats_no_data))
        pieChart.setNoDataTextColor(Color.BLACK)
        pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
        pieChart.setDrawEntryLabels(false)
        pieChart.setBackgroundColor(0xFFDDDDDD.toInt())
        pieChart.setHoleColor(0xFFDDDDDD.toInt())

        AlertDialog.Builder(context!!)
                .setTitle(when(direction) {
                    Category.INCOMING -> resources.getString(R.string.income_plural)
                    Category.OUTGOING -> resources.getString(R.string.expense_plural)
                    else -> throw IllegalStateException()
                })
                .setView(chartView)
                .setPositiveButton(resources.getString(R.string.close)) {_, _ -> }
                .create()
                .show()
    }

    private fun reloadCategorySums() {
        val categorySums = dbInstance.fetchCategoriesSummed(fromDate, untilDate)
        val categories = dbInstance.getCategories()

        inValues.clear()
        outValues.clear()

        var sumIncoming = .0
        var sumOutgoing = .0
        categories.forEach { c ->
            val sum = categorySums[c] ?: .0
            val csi = CategorySumItem(c, sum)
            when(c.direction) {
                Category.INCOMING -> {
                    sumIncoming += sum
                    inValues.add(csi)
                }
                Category.OUTGOING -> {
                    sumOutgoing += sum
                    outValues.add(csi)
                }
            }
        }

        incomingAdapter.clear()
        incomingAdapter.addAll(inValues)
        incomingAdapter.notifyDataSetChanged()
        outgoingAdapter.clear()
        outgoingAdapter.addAll(outValues)
        outgoingAdapter.notifyDataSetChanged()

        stats_sum_in.setText(Util.formatMoney(sumIncoming))
        stats_sum_out.setText(Util.formatMoney(sumOutgoing))
        stats_sum_total.setText(Util.formatMoney(sumIncoming - sumOutgoing))

        val colorAttr = if(sumIncoming - sumOutgoing < 0) R.attr.fot_red else R.attr.fot_green
        stats_sum_total.setTextColor(Util.getColorForThemeFromAttr(context!!, colorAttr))
    }

}