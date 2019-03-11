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

package com.github.duchampdev.android.fot.util.io;

import android.util.Pair;
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.bdo.TransactionItem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class SimpleSerializer implements ISerializer {

    private static final String HEADER_CATEGORIES = "---categories---";
    private static final String HEADER_TRANSACTIONS = "---transactions---";
    private static final String FIELD_SEPARATOR = ",";
    private static final String RECORD_SEPARATOR = ";";

    private static SimpleSerializer instance;



    private SimpleSerializer() {

    }

    public static SimpleSerializer getInstance() {
        if(instance == null) {
            instance = new SimpleSerializer();
        }
        return instance;
    }

    public boolean export(File target, List<TransactionItem> transactions, List<Category> categories) {
        try {
            PrintWriter pw = new PrintWriter(target);
            pw.println(HEADER_CATEGORIES);
            categories.forEach(c -> pw.println(writeCategory(c).concat(RECORD_SEPARATOR)));
            pw.println(HEADER_TRANSACTIONS);
            transactions.forEach(t -> pw.println(writeTransaction(t).concat(RECORD_SEPARATOR)));
            pw.flush();
            pw.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean importFile(File source, FinanceOrgaToolDB dbInstance) {
        HashMap<Long, Long> categoryIdMapping = new HashMap<>();
        // db cleanup - if anything was present before
        dbInstance.clear();

        // import
        try {
            Scanner sc = new Scanner(source);
            sc.next(HEADER_CATEGORIES);
            String line;
            Pair<Long, Category> parsedCategory;
            long oldId;
            long newId;
            while(sc.hasNextLine() && !(line = sc.nextLine()).equals(HEADER_TRANSACTIONS)) {
                parsedCategory = readCategory(line);
                oldId = parsedCategory.first;
                newId = dbInstance.insertOrUpdate(parsedCategory.second);
                categoryIdMapping.put(oldId, newId);
            }
            while(sc.hasNextLine()) {
                dbInstance.insertOrUpdate(readTransaction(sc.nextLine(), categoryIdMapping, dbInstance));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private String writeCategory(Category category) {
        // preserve id as we use it to keep the FK relation from the transaction entries
        return (category.getId()+"").concat(category.getName()).concat(FIELD_SEPARATOR).concat(category.getDirection()+"").concat(FIELD_SEPARATOR).concat(category.getLastUsed() + "");
    }

    private Pair<Long, Category> readCategory(String categoryString) {
        String[] propertyStrings = categoryString.split(FIELD_SEPARATOR);
        return new Pair<>(Long.parseLong(propertyStrings[0]), new Category(propertyStrings[1], Integer.parseInt(propertyStrings[2]), Long.parseLong(propertyStrings[3])));
    }

    private String writeTransaction(TransactionItem transaction) {
        String transactionTitle = transaction.getTitle() != null ? transaction.getTitle() : ""; // is optional
        return transaction.getSecondParty().concat(FIELD_SEPARATOR).concat(transaction.getAmount()+"").concat(FIELD_SEPARATOR).concat(transactionTitle).concat(FIELD_SEPARATOR).concat(transaction.getCategory().getId()+"").concat(FIELD_SEPARATOR).concat(transaction.getDate().getTime()+"");
    }

    private TransactionItem readTransaction(String transactionString, Map<Long, Long> categoryIdMapping, FinanceOrgaToolDB dbInstance) {
        String[] propertyStrings = transactionString.split(FIELD_SEPARATOR, -1);
        Category category = dbInstance.getCategoryById(categoryIdMapping.get(Long.parseLong(propertyStrings[3])));
        return new TransactionItem(propertyStrings[0], Double.parseDouble(propertyStrings[1]), propertyStrings[2], category, new Date(Long.parseLong(propertyStrings[4])));
    }
}
