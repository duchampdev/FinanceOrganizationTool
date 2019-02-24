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

package com.github.duchampdev.android.fot.backend;

import android.content.Context;
import android.util.Pair;

import com.github.duchampdev.android.fot.bdo.TransactionItem;

import java.util.*;


public class SecondpartyAutocompleteService {

    private static SecondpartyAutocompleteService instance;

    private FinanceOrgaToolDB dbInstance;

    private final Map<String, Integer> indexItems = new HashMap<>();
    private String[] orderedItems;


    public static SecondpartyAutocompleteService getInstance(Context context) {
        if (instance == null) {
            instance = new SecondpartyAutocompleteService(context);
        }
        return instance;
    }

    private SecondpartyAutocompleteService(Context context) {
        dbInstance = FinanceOrgaToolDB.getInstance(context);
        setupDbCallbacks();
        reloadIndex();
    }

    private void setupDbCallbacks() {
        dbInstance.registerDbFinTransactionEventCallbacks(new FinanceOrgaToolDB.DbFinTransactionEventCallbacks() {
            @Override
            public void transactionInserted(TransactionItem transaction) {
                addUsage(transaction.getSecondParty());
                reloadIndex();
            }

            @Override
            public void transactionUpdated(TransactionItem transaction, TransactionItem transactionOld) {
                if (! transaction.getSecondParty().equals(transactionOld.getSecondParty())) {
                    removeUsage(transactionOld.getSecondParty());
                    addUsage(transaction.getSecondParty());
                    reloadIndex();
                }
            }

            @Override
            public void transactionRemoved(TransactionItem transaction) {
                removeUsage(transaction.getSecondParty());
                reloadIndex();
            }
        });
    }

    private void addUsage(String name) {
        int newCount = indexItems.containsKey(name) ? indexItems.get(name) + 1 : 1;
        dbInstance.insertOrUpdateSecondpartyAutocompleteItem(name, newCount);
    }

    private void removeUsage(String name) {
        if (!indexItems.containsKey(name)) return;
        int newCount = indexItems.get(name) - 1;
        if (newCount == 0) {
            dbInstance.removeSecondpartyAutocompleteItem(name);
        } else {
            dbInstance.insertOrUpdateSecondpartyAutocompleteItem(name, newCount);
        }
    }

    public String[] getProposals() {
        return orderedItems;
    }

    private void reloadIndex() {
        indexItems.clear();
        Pair<String[], int[]> itemArrays = dbInstance.fetchSecondpartyAutocompleteItems();
        int length = itemArrays.first.length;
        for(int i=0; i < length; i++) {
            indexItems.put(itemArrays.first[i], itemArrays.second[i]);
        }
        orderedItems = itemArrays.first;
    }
}
