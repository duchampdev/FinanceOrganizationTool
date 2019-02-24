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

import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.TransactionItem;
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.frontend.adapters.TransactionAdapter;


public class DashboardFragment extends Fragment {

    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private ArrayAdapter<String> yearAdapter;

    private ListView financePositions;
    private TransactionAdapter transactionAdapter;
    private FinanceOrgaToolDB dbInstance;

    private static final String[] YEARS;

    private int currentMonthZeroBased;
    private int currentYear;

    static {
        ArrayList<String> years = new ArrayList<>();
        for (int i = Calendar.getInstance().get(Calendar.YEAR); i >= 2018; i--) {
            years.add(i + "");
        }
        YEARS = years.toArray(new String[years.size()]);
    }

    public static DashboardFragment newInstance() {
        Bundle args = new Bundle();
        DashboardFragment fragment = new DashboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadTransactions();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        dbInstance = FinanceOrgaToolDB.getInstance(v.getContext());

        monthSpinner =  v.findViewById(R.id.dashboard_month);
        yearSpinner =  v.findViewById(R.id.dashboard_year);
        financePositions = v.findViewById(R.id.dashboard_positions);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(v.getContext(), R.array.months, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, YEARS);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        yearSpinner.setAdapter(yearAdapter);

        currentMonthZeroBased = Calendar.getInstance().get(Calendar.MONTH);
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        monthSpinner.setSelection(currentMonthZeroBased); // january == 0 -> so no problem with zero-based indices
        yearSpinner.setSelection(0); // present year

        transactionAdapter = new TransactionAdapter(v.getContext(), R.layout.transaction_item, new ArrayList<>());
        financePositions.setAdapter(transactionAdapter);


        initGUI();
        reloadTransactions();

        return v;
    }

    void addNewTransaction() {
            Bundle transactionBundle = new Bundle();
            transactionBundle.putSerializable("categories", (Serializable) dbInstance.getCategories());

            TransactionDialogFragment df = new TransactionDialogFragment();
            df.setArguments(transactionBundle);
            df.registerTransactionDialogCallbacks(item -> {
                dbInstance.insertOrUpdate(item);
                reloadTransactions();
            });
            df.show(getFragmentManager(), "atdf");
        }

    private void initGUI() {

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMonthZeroBased = position;
                reloadTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                monthSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH)); // prevent nothing being selected
                currentMonthZeroBased = Calendar.getInstance().get(Calendar.MONTH);
                reloadTransactions();
            }
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentYear = Calendar.getInstance().get(Calendar.YEAR) - position; // current year is first, others following
                reloadTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                yearSpinner.setSelection(0); // prevent nothing being selected
                currentYear = Calendar.getInstance().get(Calendar.YEAR);
                reloadTransactions();
            }
        });

        financePositions.setOnItemLongClickListener((parent, view, position, id) -> {
            final TransactionItem t = transactionAdapter.getItem(position);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setTitle(getResources().getString(R.string.dashboard_transaction_menu));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.edit), (dialog, which) -> {
                TransactionDialogFragment tdf = new TransactionDialogFragment();
                Bundle transactionBundle = new Bundle();
                transactionBundle.putSerializable("titem", t);
                transactionBundle.putSerializable("categories", (Serializable) dbInstance.getCategories());
                tdf.setArguments(transactionBundle);
                tdf.registerTransactionDialogCallbacks(item -> {
                    dbInstance.insertOrUpdate(item);
                    reloadTransactions();
                });
                tdf.show(getFragmentManager(), "etdf");

            });
            dialogBuilder.setNegativeButton(getResources().getString(R.string.delete), (dialog, which) -> Snackbar.make(getView(), getResources().getString(R.string.dashboard_transaction_really_delete), Snackbar.LENGTH_LONG).setAction(getResources().getString(R.string.delete), v -> {
                dbInstance.remove(t);
                reloadTransactions();
                Snackbar.make(getView(), getResources().getString(R.string.dashboard_transaction_deleted), Snackbar.LENGTH_LONG).setActionTextColor(getResources().getColor(R.color.colorCreme)).show();
            }).setActionTextColor(getResources().getColor(R.color.colorCreme)).show());
            dialogBuilder.setNeutralButton(getResources().getString(R.string.abort), (dialog, which) -> {
                // do nothing
            });
            dialogBuilder.create().show();
            return true;
        });
    }


    private void reloadTransactions() {
        List<TransactionItem> transactions = dbInstance.fetchMonth(currentMonthZeroBased, currentYear);
        transactionAdapter.clear();
        transactionAdapter.addAll(transactions);
        transactionAdapter.notifyDataSetChanged();

    }
}
