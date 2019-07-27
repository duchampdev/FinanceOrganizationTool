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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.backend.SecondpartyAutocompleteService;
import com.github.duchampdev.android.fot.bdo.TransactionItem;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.util.DateButton;
import com.github.duchampdev.android.fot.util.Util;


public class TransactionDialogFragment extends DialogFragment {

    private final List<TransactionDialogCallbacks> listeners = new ArrayList<>();

    private AutoCompleteTextView secondParty;
    private EditText amount;
    private EditText title;
    private DateButton date;
    private Spinner categorySpinner;

    private TransactionItem editable;
    private List<Category> categories;
    private ArrayAdapter<Category> categorySpinnerAdapter;
    private SecondpartyAutocompleteService autocompleteService;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String dialogTitle = (args != null && args.containsKey("titem")) ? getResources().getString(R.string.tdialog_edit_transaction) : getResources().getString(R.string.tdialog_add_transaction);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(dialogTitle);
        dialogBuilder.setPositiveButton(getResources().getString(R.string.save), (dialog, which) -> {
            // implemented later
        });
        dialogBuilder.setNeutralButton(getResources().getString(R.string.abort), (dialog, which) -> {
            // nothing to do
        });


        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_transaction, null);

        secondParty = v.findViewById(R.id.tdialog_secondparty);
        amount = v.findViewById(R.id.tdialog_amount);
        title = v.findViewById(R.id.tdialog_title);
        date = v.findViewById(R.id.tdialog_date);
        categorySpinner = v.findViewById(R.id.tdialog_category);

        categorySpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        categorySpinner.setAdapter(categorySpinnerAdapter);

        amount.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                // format currency correctly
                try {
                    double amount = Util.formatMoney(TransactionDialogFragment.this.amount.getText().toString());
                    TransactionDialogFragment.this.amount.setText(Util.formatMoney(amount));
                } catch (NumberFormatException nfe) {
                    Toast.makeText(getContext(), getResources().getString(R.string.tdialog_enter_valid_amount), Toast.LENGTH_SHORT).show();
                }
            }
        });
        autocompleteService = SecondpartyAutocompleteService.Companion.getInstance(secondParty.getContext());
        secondParty.setAdapter(new ArrayAdapter<>(secondParty.getContext(), android.R.layout.simple_spinner_dropdown_item,autocompleteService.getProposals()));
        dialogBuilder.setView(v);
        return dialogBuilder.create();
    }

    @Override
    public void onResume() { // set specific save routine - do not exit on error
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> saveTransaction());
        }
    }

    private void saveTransaction() {
        if (secondParty.getText().toString().isEmpty()) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getResources().getString(R.string.tdialog_secondparty_missing), Toast.LENGTH_LONG).show());
        } else if (amount.getText().toString().isEmpty()) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getResources().getString(R.string.tdialog_amount_missing), Toast.LENGTH_LONG).show());
        } else if (categorySpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), getResources().getString(R.string.tdialog_category_missing), Toast.LENGTH_LONG).show());
        } else {
            // check amount format
            try {
                Util.formatMoney(amount.getText().toString());
            } catch (NumberFormatException nfe) {
                Toast.makeText(getContext(), getResources().getString(R.string.tdialog_enter_valid_amount), Toast.LENGTH_SHORT).show();
                return; // must break up, can't recover from this issue
            }

            // data is valid - persist
            if (editable == null) { // new item
                editable = new TransactionItem(secondParty.getText().toString(), Util.formatMoney(amount.getText().toString()), title.getText().toString(), (Category) categorySpinner.getSelectedItem(), date.getDate());
            } else {
                editable.setSecondParty(secondParty.getText().toString());
                editable.setAmount(Util.formatMoney(amount.getText().toString()));
                editable.setTitle(title.getText().toString());
                editable.setDate(date.getDate());
                editable.setCategory((Category) categorySpinner.getSelectedItem());
            }
            for (TransactionDialogCallbacks l : listeners) {
                l.dataSaved(editable);
            }
            dismiss(); // only called if valid input
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null && args.containsKey("categories")) {
            categories = (List<Category>) args.get("categories");
            categorySpinnerAdapter.clear();
            categorySpinnerAdapter.addAll(categories);
            categorySpinnerAdapter.notifyDataSetChanged();
            if (args.containsKey("titem")) {
                editable = (TransactionItem) args.getSerializable("titem");
                secondParty.setText(editable.getSecondParty());
                //amount.setText(editable.getAmount() + ""); // do not show euro sign
                amount.setText(Util.formatMoney(editable.getAmount()));
                title.setText(editable.getTitle());
                date.setDate(editable.getDate());
                categorySpinner.setSelection(categorySpinnerAdapter.getPosition(editable.getCategory()));
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    public void registerTransactionDialogCallbacks(TransactionDialogCallbacks c) {
        listeners.add(c);
    }

    public interface TransactionDialogCallbacks {
        void dataSaved(TransactionItem item);
    }
}
