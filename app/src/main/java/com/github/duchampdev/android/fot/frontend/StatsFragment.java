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


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.github.duchampdev.android.fot.bdo.CategorySumItem;
import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.TransactionItem;
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.frontend.adapters.CategorySumAdapter;
import com.github.duchampdev.android.fot.frontend.adapters.TransactionAdapter;
import com.github.duchampdev.android.fot.util.Util;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.text.DecimalFormat;
import java.util.*;


public class StatsFragment extends Fragment {

    private FinanceOrgaToolDB dbInstance;

    private CategorySumAdapter incomingAdapter;
    private CategorySumAdapter outgoingAdapter;

    private Button from;
    private Button until;
    private ListView incoming;
    private ListView outgoing;
    private EditText sumIn;
    private EditText sumOut;
    private EditText sumTotal;

    private Button openPieIn;
    private Button openPieOut;

    private Date fromDate;
    private Date untilDate;

    private final List<CategorySumItem> inValues = new ArrayList<>();
    private final List<CategorySumItem> outValues = new ArrayList<>();


    public static StatsFragment newInstance() {

        Bundle args = new Bundle();

        StatsFragment fragment = new StatsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadCategorySums();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        dbInstance = FinanceOrgaToolDB.getInstance(v.getContext());
        from = v.findViewById(R.id.stats_from);
        until = v.findViewById(R.id.stats_until);
        incoming = v.findViewById(R.id.stats_in_list);
        outgoing = v.findViewById(R.id.stats_out_list);
        sumIn = v.findViewById(R.id.stats_sum_in);
        sumOut = v.findViewById(R.id.stats_sum_out);
        sumTotal = v.findViewById(R.id.stats_sum_total);
        openPieIn = v.findViewById(R.id.stats_show_pie_in);
        openPieOut = v.findViewById(R.id.stats_show_pie_out);

        initGUI();

        return v;
    }

    private void initGUI() {
        from.setOnClickListener(this::changeTimeRange);
        until.setOnClickListener(this::changeTimeRange);
        openPieIn.setOnClickListener(v -> showPie(Category.INCOMING));
        openPieOut.setOnClickListener(v -> showPie(Category.OUTGOING));

        incomingAdapter = new CategorySumAdapter(getContext(), R.layout.categorysum_item, new ArrayList<>());
        outgoingAdapter = new CategorySumAdapter(getContext(), R.layout.categorysum_item, new ArrayList<>());
        incoming.setAdapter(incomingAdapter);
        outgoing.setAdapter(outgoingAdapter);

        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0);
        from.setText(Util.formatDate(cal));
        fromDate = cal.getTime();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        until.setText(Util.formatDate(cal));
        untilDate = cal.getTime();
        reloadCategorySums();

        incoming.setOnItemClickListener((parent, view, position, id) -> showCategoryTransactionsDialog(incomingAdapter.getItem(position).getCategory()));
        outgoing.setOnItemClickListener((parent, view, position, id) -> showCategoryTransactionsDialog(outgoingAdapter.getItem(position).getCategory()));
    }

    private void showCategoryTransactionsDialog(Category category) {
        List<TransactionItem> transactions = dbInstance.fetchTransactionsBetweenForCategory(fromDate, untilDate, category);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle(category.getName());
        dialogBuilder.setAdapter(new TransactionAdapter(getContext(), R.layout.transaction_item, transactions), null);
        dialogBuilder.setNegativeButton(getResources().getString(R.string.close), (dialog, which) -> { });
        dialogBuilder.create().show();
    }


    private void changeTimeRange(final View v) {
        Calendar cal = Calendar.getInstance();
        if (v == from) {
            cal.setTime(fromDate);
        } else if (v == until) {
            cal.setTime(untilDate);
        }
        DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            Calendar res = Calendar.getInstance();
            if (v == from) {
                res.set(year, month, dayOfMonth, 0, 0, 0);
                fromDate = res.getTime();
                from.setText(Util.formatDate(res));
                if(dayOfMonth == 1 && PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(getContext().getResources().getString(R.string.pref_key_statsrange), true)) {
                    // the user likely want's to show at least a whole month, so also adjust until, if allowed
                    Calendar end = Calendar.getInstance();
                    end.set(year, month, res.getActualMaximum(Calendar.DAY_OF_MONTH), 0, 0, 0);
                    untilDate = end.getTime();
                    until.setText(Util.formatDate(end));
                }
            } else if (v == until) {
                res.set(year, month, dayOfMonth, 23, 59, 59);
                untilDate = res.getTime();
                until.setText(Util.formatDate(res));
            }
            reloadCategorySums();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        if (v == from) datePicker.setMessage(getResources().getString(R.string.stats_startdate));
        else if (v == until) datePicker.setMessage(getResources().getString(R.string.stats_enddate));
        datePicker.show();
    }

    private void showPie(int direction) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_chart, null);
        PieChart pieChart = v.findViewById(R.id.dialog_chart_piechart);

        List<PieEntry> entries = new ArrayList<>();
        List<CategorySumItem> rawData = (direction == Category.INCOMING) ? inValues : outValues;
        double fullSum = 0;
        for (CategorySumItem csi : rawData) {
            fullSum += csi.getSum();
        }
        for (CategorySumItem csi : rawData) {
            if (csi.getSum() == 0) continue; // don't add unused categories
            entries.add(new PieEntry((float) (csi.getSum() / fullSum * 100), csi.getCategory().getName()));
        }

        PieDataSet piePreData = new PieDataSet(entries, "");
        piePreData.setColors(Util.getStatsDiagramColors(rawData.size()));

        PieData pieData = new PieData(piePreData);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(16);

        if (!entries.isEmpty()) pieChart.setData(pieData);
        pieChart.getDescription().setText("");
        pieChart.animateY(500);
        pieChart.getLegend().setTextSize(16);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.setNoDataText(getResources().getString(R.string.stats_no_data));
        pieChart.setNoDataTextColor(Color.BLACK);
        pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
        pieChart.setDrawEntryLabels(false);
        pieChart.setBackgroundColor(0xFFDDDDDD);
        pieChart.setHoleColor(0xFFDDDDDD);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle((direction == Category.INCOMING) ? getResources().getString(R.string.income_plural) : getResources().getString(R.string.expense_plural));
        dialogBuilder.setView(v);
        dialogBuilder.setPositiveButton(getResources().getString(R.string.close), (dialog, which) -> {
        });
        dialogBuilder.create().show();
    }


    private void reloadCategorySums() {
        Map<Category, Double> categorySums = dbInstance.fetchCategoriesSummed(fromDate, untilDate);
        List<Category> categories = dbInstance.getCategories();
        inValues.clear();
        outValues.clear();
        double sumIncoming = 0;
        double sumOutgoing = 0;
        for (Category cat : categories) {
            double sum = categorySums.containsKey(cat) ? categorySums.get(cat) : 0;
            CategorySumItem csi = new CategorySumItem(cat, sum);
            if (cat.getDirection() == Category.INCOMING) {
                sumIncoming += sum;
                inValues.add(csi);
            } else if (cat.getDirection() == Category.OUTGOING) {
                sumOutgoing += sum;
                outValues.add(csi);
            }
        }
        incomingAdapter.clear();
        outgoingAdapter.clear();
        incomingAdapter.addAll(inValues);
        outgoingAdapter.addAll(outValues);
        incomingAdapter.notifyDataSetChanged();
        outgoingAdapter.notifyDataSetChanged();

        sumIn.setText(Util.formatMoney(sumIncoming));
        sumOut.setText(Util.formatMoney(sumOutgoing));
        sumTotal.setText(Util.formatMoney(sumIncoming - sumOutgoing));
        if (sumIncoming - sumOutgoing < 0) {
            sumTotal.setTextColor(Util.getColorForThemeFromAttr(getContext(), R.attr.fot_red));
        } else {
            sumTotal.setTextColor(Util.getColorForThemeFromAttr(getContext(), R.attr.fot_green));
        }

    }
}
