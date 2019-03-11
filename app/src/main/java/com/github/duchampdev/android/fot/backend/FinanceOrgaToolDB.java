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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.util.Pair;
import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.bdo.TransactionItem;
import com.github.duchampdev.android.fot.bdo.Category;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FinanceOrgaToolDB extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "fot-transactions.db";

    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_SECONDPARTY_INDEX = "secondpartyindex";

    public static final int INSERT_ERROR = -1;
    public static final int UPDATE_SUCCESS = 1;

    private static final String CREATE_TABLE_TRANSACTIONS = "CREATE TABLE IF NOT EXISTS transactions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "secondparty TEXT, " +
            "amount DECIMAL(7,2), " +
            "title TEXT, " +
            "category INTEGER, " +
            "date INTEGER, FOREIGN KEY (category) REFERENCES categories(id)" +
            ");";

    private static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE IF NOT EXISTS categories (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT, " +
            "direction INTEGER, " +
            "lastused INTEGER" +
            ");";

    private static final String CREATE_TABLE_SECONDPARTYINDEX = "CREATE TABLE IF NOT EXISTS secondpartyindex ( " +
            "name TEXT PRIMARY KEY," +
            "count INTEGER" +
            ");";

    private static final String CREATE_INDEX_SECONDPARTYINDEX = "CREATE INDEX spindex_occurances_index " +
            "ON " + TABLE_SECONDPARTY_INDEX + " " +
            "(count DESC, name ASC)"; // use index for performance when sorting


    private static FinanceOrgaToolDB instance;
    private SQLiteDatabase db;
    private final Map<Long, Category> categoriesCache = new HashMap<>();
    private final List<DbFinTransactionEventCallbacks> listeners = new ArrayList<>();

    private Context context;

    private boolean isOpeningDb = false;


    public static FinanceOrgaToolDB getInstance(Context context) {
        if (instance == null) {
            instance = new FinanceOrgaToolDB(context);
        }
        return instance;
    }

    private FinanceOrgaToolDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * just to be used internally
     */
    private void openDB() {
        if(db == null || !db.isOpen()) {
            // only open db if necessary
            isOpeningDb = true;
            db = getWritableDatabase();
            refreshCategoriesCache();
            isOpeningDb = false;
            Logger.getLogger("FOT-" + getClass().getSimpleName()).log(Level.INFO, "DB has been opened");
        }
    }

    /**
     * DB is opened automatically
     */
    public void closeDB() {
        db.close();
        Logger.getLogger("FOT-" + getClass().getSimpleName()).log(Level.INFO, "DB has been closed");

    }

    public long insertOrUpdate(TransactionItem item) {
        long opSuccess = _insertOrUpdate(item);
        if(opSuccess > 0) {
            Category c = item.getCategory();
            c.setLastUsed(System.currentTimeMillis());
            insertOrUpdate(c);
        }
        return opSuccess;
    }

    private long _insertOrUpdate(TransactionItem item) {
        openDB();
        ContentValues cv = new ContentValues();
        cv.put("secondparty", item.getSecondParty());
        cv.put("amount", item.getAmount());
        cv.put("title", item.getTitle());
        cv.put("category", item.getCategory().getId());
        cv.put("date", item.getDate().getTime());
        if(item.getId() == TransactionItem.NOID) {
            for(DbFinTransactionEventCallbacks listener : listeners) listener.transactionInserted(item);
            return db.insertOrThrow(TABLE_TRANSACTIONS, null, cv);
        } else {
            TransactionItem itemOld = fetchTransactionById(item.getId());
            for(DbFinTransactionEventCallbacks listener : listeners) listener.transactionUpdated(item, itemOld);
            return db.update(TABLE_TRANSACTIONS, cv, "id=?", new String[]{item.getId() + ""});
        }
    }

    public long insertOrUpdate(Category category) {
        openDB();
        ContentValues cv = new ContentValues();
        cv.put("name", category.getName());
        cv.put("direction", category.getDirection());
        cv.put("lastUsed", category.getLastUsed());
        long result;
        if(category.getId() == Category.NOID) {
            // check if category already exists (if so, abort without side effects)
            for(Category predicate : categoriesCache.values()) {
                if(predicate.getName().equals(category.getName()) && predicate.getDirection() == category.getDirection()) return INSERT_ERROR;
            }
            // insert
            result = db.insertOrThrow(TABLE_CATEGORIES, null, cv);
        } else {
            result = db.update(TABLE_CATEGORIES, cv, "id=?", new String[]{category.getId() + ""});
        }
        refreshCategoriesCache();
        return result;
    }

    public boolean remove(TransactionItem item) {
        openDB();
        int result =  db.delete(TABLE_TRANSACTIONS, "id=?", new String[]{item.getId() + ""});
        if(result == 1) {
            //success
            for(DbFinTransactionEventCallbacks listener : listeners) listener.transactionRemoved(item);
        }
        return result == 1;
    }

    public boolean remove(Category category) {
        openDB();
        db.delete(TABLE_TRANSACTIONS, "category=?", new String[]{category.getId() + ""});
        boolean success =  db.delete(TABLE_CATEGORIES, "id=?", new String[]{category.getId() + ""}) == 1;
        refreshCategoriesCache();
        return success;
    }

    /**
     * sort categories by in/out and then by name
     * @return
     */
    public List<Category> getCategories() {
        openDB();
        List<Category> categories = new ArrayList<>(categoriesCache.values());
        boolean orderLRU = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_categoryorder), false);
        if (orderLRU) {
            categories.sort((o1, o2) -> -Long.compare(o1.getLastUsed(), o2.getLastUsed())); // sort LRU
        } else {
            Collections.sort(categories, (o1, o2) -> {
                int toplevel = - Integer.compare(o1.getDirection(), o2.getDirection()); // outgoing first
                return toplevel != 0 ? toplevel : o1.getName().compareTo(o2.getName());
            });
        }
        return categories;
    }

    private void refreshCategoriesCache() {
        if(!isOpeningDb) {
            openDB(); // might lead to double refreshCategoriesCache call
        }
        categoriesCache.clear();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + ";", null);
        while(c.moveToNext()) {
            long id = c.getLong(0);
            String name = c.getString(1);
            int direction = c.getInt(2);
            long lastUsed = c.getLong(3);
            categoriesCache.put(id, new Category(id, name, direction, lastUsed));
        }
        c.close();
    }


    private List<TransactionItem> fetchTransactionsBetween(Date from, Date until) {
        // openDB not necessary, is private method
        Cursor c = db.rawQuery("SELECT * " +
                "FROM " + TABLE_TRANSACTIONS + " " +
                "WHERE date>=? AND date <=? " +
                "ORDER BY date DESC",
                new String[] {from.getTime() + "", until.getTime() + ""});
        return fetchTransactionsFromCursor(c);
    }

    public List<TransactionItem> fetchTransactionsBetweenForCategory(Date from, Date until, Category category) {
        openDB();
        Cursor c = db.rawQuery("SELECT * " +
                "FROM " + TABLE_TRANSACTIONS + " " +
                "WHERE date>=? AND date <=? AND category=? " +
                "ORDER BY date DESC",
                new String[] {from.getTime() + "", until.getTime() + "", category.getId() + ""});
        return fetchTransactionsFromCursor(c);
    }

    /**
     *
     * @param c the cursor to fetch the transactions from, is closed in the end
     * @return the queried transactions
     */
    private List<TransactionItem> fetchTransactionsFromCursor(final Cursor c) {
        // openDB not necessary, is private method
        List<TransactionItem> transactions = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(0);
            String secondParty = c.getString(1);
            double amount = c.getDouble(2);
            String title = c.isNull(3) ? null : c.getString(3);
            Category category = categoriesCache.get(c.getLong(4));
            Date date = new Date(c.getLong(5));

            TransactionItem t = new TransactionItem(id, secondParty, amount, title, category, date);
            transactions.add(t);
        }
        c.close();
        return transactions;
    }

    private TransactionItem fetchTransactionById(long id) {
        openDB();
        Cursor c = db.rawQuery("SELECT * " +
                        "FROM " + TABLE_TRANSACTIONS + " " +
                        "WHERE id=?",
                new String[] {id + ""});
        return fetchTransactionsFromCursor(c).get(0);
    }

    public List<TransactionItem> fetchMonth(int month, int year) {
        openDB();
        Calendar from = Calendar.getInstance();
        Calendar until = Calendar.getInstance();
        from.set(year, month, 1, 0, 0, 0);
        until.set(year, month, from.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return fetchTransactionsBetween(from.getTime(), until.getTime());
    }

    public Map<Category, Double> fetchCategoriesSummed(Date from, Date until) {
        openDB();
        Map<Category, Double> categorySums = new HashMap<>();
        Cursor c = db.rawQuery("SELECT category, SUM(amount) " +
                "FROM " + TABLE_TRANSACTIONS + " " +
                "WHERE date>=? AND date<=? " +
                "GROUP BY category " +
                "ORDER BY category ASC", new String[]{from.getTime() + "", until.getTime() + ""});

        while (c.moveToNext()) {
            Category category = categoriesCache.get(c.getLong(0));
            double sum = c.getDouble(1);
            categorySums.put(category, sum);
        }
        c.close();
        return categorySums;
    }

    // PACKAGE-PRIVATE METHODS FOR SECONDPARTY AUTOCOMPLETE
     Pair<String[],int[]> fetchSecondpartyAutocompleteItems() {
        openDB();
        Cursor c = db.rawQuery("SELECT name, count " +
                "FROM " + TABLE_SECONDPARTY_INDEX + " " +
                "ORDER BY count DESC, name ASC;", null);

        int cursorSize = c.getCount();
        String[] indexItemsNames = new String[cursorSize];
        int[] indexitemsCount = new int[cursorSize];
        int i=0;
        while (c.moveToNext()) {
            indexItemsNames[i] = c.getString(0);
            indexitemsCount[i] = c.getInt(1);
            i++;
        }
        c.close();
        return new Pair<>(indexItemsNames, indexitemsCount);
    }

     void insertOrUpdateSecondpartyAutocompleteItem(String name, int count) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("count", count);
        int updateResult = db.update(TABLE_SECONDPARTY_INDEX, cv, "name=?", new String[]{name});
        if(updateResult == 0) { // nothing updated -> entry missing
            db.insert(TABLE_SECONDPARTY_INDEX, null, cv);
        }
    }

    void removeSecondpartyAutocompleteItem(String name) {
        db.delete(TABLE_SECONDPARTY_INDEX, "name=?", new String[]{name});
    }
    // END AUTOCOMPLETE


    private void createDefaultCategories(SQLiteDatabase initDb) {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Geschenk", Category.INCOMING));
        categories.add(new Category("Einkommen", Category.INCOMING));
        categories.add(new Category("Unterhalt", Category.INCOMING));

        categories.add(new Category("Ernährung", Category.OUTGOING));
        categories.add(new Category("Geschenk", Category.OUTGOING));
        categories.add(new Category("Fixkosten", Category.OUTGOING));
        categories.add(new Category("Haushalt", Category.OUTGOING));
        categories.add(new Category("Freizeit", Category.OUTGOING));

        initDb.beginTransaction();
        long minId = Long.MAX_VALUE;
        SQLiteStatement stmt = initDb.compileStatement("INSERT INTO categories (name, direction) VALUES (?,?);");
        for(Category c : categories) {
            stmt.bindString(1, c.getName());
            stmt.bindLong(2, c.getDirection());
            minId = Math.min(stmt.executeInsert(), minId);
        }
        if (minId > -1 ) {
            // no error occured
            initDb.setTransactionSuccessful();
        } else {
            System.err.println("creation of default categories failed");
        }
        initDb.endTransaction();
    }

    interface DbFinTransactionEventCallbacks {
        void transactionInserted(TransactionItem transaction);
        void transactionUpdated(TransactionItem transaction, TransactionItem transactionOld);
        void transactionRemoved(TransactionItem transaction);
    }

    void registerDbFinTransactionEventCallbacks(DbFinTransactionEventCallbacks listener) {
        listeners.add(listener);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_SECONDPARTYINDEX);
        db.execSQL(CREATE_INDEX_SECONDPARTYINDEX);
        createDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 3) {
            db.beginTransaction();
            try {
                // move transactions to temp table and setup new tables
                db.execSQL("CREATE TABLE transacttmp AS SELECT * FROM " + TABLE_TRANSACTIONS); // old schema
                db.execSQL("DROP TABLE " + TABLE_TRANSACTIONS);
                db.execSQL(CREATE_TABLE_CATEGORIES);
                db.execSQL(CREATE_TABLE_TRANSACTIONS);

                // extract existing categories and migrate them
                Map<String,Long> categoryLegacyToId = new HashMap<>();
                Cursor c = db.rawQuery("SELECT DISTINCT category FROM transacttmp", new String[]{});
                ContentValues cv = new ContentValues();
                while(c.moveToNext()) {
                    String legacyName = c.getString(0);
                    int direction;
                    if(legacyName.contains("(A)") || legacyName.contains("(E)")) {
                        direction = legacyName.contains("(A)") ? Category.OUTGOING : Category.INCOMING;
                    } else {
                        c.close();
                        throw new IllegalArgumentException("invalid category in DB!");
                    }
                    String name = legacyName.substring(0, legacyName.length()-1-3); // -1 to correct length/zero-based and -3 to remove (A)/(E) and preceeding whitespace (not -4 as end is handled exclusive)
                    cv.put("name", name);
                    cv.put("direction", direction);
                    long categoryId = db.insert(TABLE_CATEGORIES, null, cv);
                    cv.clear(); // clear for next run
                    categoryLegacyToId.put(legacyName, categoryId);
                }
                c.close();

                // transfer legacy transactions
                c = db.rawQuery("SELECT * FROM transacttmp", new String[]{});
                cv = new ContentValues(); // create new object for new job
                while(c.moveToNext()) {
                    String secondParty = c.getString(1);
                    double amount = c.getDouble(2);
                    String title = c.isNull(3) ? null : c.getString(3);
                    long categoryId = categoryLegacyToId.get(c.getString(4));
                    long rawDate = c.getLong(5);

                    cv.put("secondparty", secondParty);
                    cv.put("amount", amount);
                    cv.put("title", title);
                    cv.put("category", categoryId);
                    cv.put("date", rawDate);
                    db.insert(TABLE_TRANSACTIONS, null, cv);
                    cv.clear();
                }

                // clean up
                db.execSQL("DROP TABLE transacttmp");

                db.setTransactionSuccessful();
            } catch (Exception se) {
                System.err.println("Error while upgrading DB!");
                se.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        if(oldVersion < 4) {
            // extract values for secondparty autocomplete
            db.execSQL(CREATE_TABLE_SECONDPARTYINDEX);
            Cursor c = db.rawQuery("SELECT secondparty, COUNT(secondparty) " +
                    "FROM " + TABLE_TRANSACTIONS + " " +
                    "GROUP BY secondparty", null);
            ContentValues cv = new ContentValues();
            while(c.moveToNext()) {
                cv.put("name", c.getString(0));
                cv.put("count", c.getInt(1));
                db.insert(TABLE_SECONDPARTY_INDEX, null, cv);
            }
            c.close();

            // create index after insertions (deferred for performance)
            db.execSQL(CREATE_INDEX_SECONDPARTYINDEX);
        }
        if(oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_CATEGORIES + " ADD COLUMN lastused INTEGER DEFAULT 0;"); // init to 1970
        }
    }
}
