package com.github.duchampdev.android.fot.backend

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.preference.PreferenceManager
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.bdo.Category
import com.github.duchampdev.android.fot.bdo.TransactionItem
import java.lang.Exception
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min

class FinanceOrgaToolDB private constructor(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 5
        private val DATABASE_NAME = "fot-transactions.db"

        private val TABLE_TRANSACTIONS = "transactions"
        private val TABLE_CATEGORIES = "categories"
        private val TABLE_SECONDPARTY_INDEX = "secondpartyindex"

        val INSERT_ERROR = -1
        val UPDATE_SUCCESS = 1

        private val CREATE_TABLE_TRANSACTIONS = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "secondparty TEXT, " +
                "amount DECIMAL(7,2), " +
                "title TEXT, " +
                "category INTEGER, " +
                "date INTEGER, FOREIGN KEY (category) REFERENCES categories(id)" +
                ");"

        private val CREATE_TABLE_CATEGORIES = "CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "direction INTEGER, " +
                "lastused INTEGER" +
                ");"

        private val CREATE_TABLE_SECONDPARTYINDEX = "CREATE TABLE IF NOT EXISTS secondpartyindex ( " +
                "name TEXT PRIMARY KEY," +
                "count INTEGER" +
                ");"

        private val CREATE_INDEX_SECONDPARTYINDEX = "CREATE INDEX spindex_occurances_index " +
                "ON " + TABLE_SECONDPARTY_INDEX + " " +
                "(count DESC, name ASC)" // use index for performance when sorting


        private lateinit var instance: FinanceOrgaToolDB

        fun getInstance(context: Context): FinanceOrgaToolDB {
            if (!::instance.isInitialized) instance = FinanceOrgaToolDB(context)
            return instance
        }
    }

    private var db: SQLiteDatabase? = null
    private val categoriesCache: MutableMap<Long, Category> = HashMap()
    private val listeners: MutableList<DbFinTransactionEventCallbacks> = ArrayList()
    private val logger = Logger.getLogger(javaClass.name)

    private var isOpeningDb = false

    /**
     * just to be used internally
     */
    private fun openDB() {
        if (db?.isOpen != true) {
            // only open db if necessary
            isOpeningDb = true
            db = writableDatabase
            refreshCategoriesCache()
            isOpeningDb = false
            logger.log(Level.INFO, "DB has been opened")
        }
    }

    /**
     * DB is opened automatically
     */
    fun closeDB() {
        db?.close()
        logger.log(Level.INFO, "DB has been closed (if it was open)")
    }

    fun insertOrUpdate(transactionItem: TransactionItem): Long {
        return _insertOrUpdate(transactionItem)
                .also { opSucess ->
                    if (opSucess > 0) {
                        val c = transactionItem.category
                        c.lastUsed = System.currentTimeMillis()
                        insertOrUpdate(c)
                    }
                }
    }

    private fun _insertOrUpdate(transactionItem: TransactionItem): Long {
        openDB() // ensure db != null
        val cv = ContentValues()
        cv.put("secondparty", transactionItem.secondParty)
        cv.put("amount", transactionItem.amount)
        cv.put("title", transactionItem.title)
        cv.put("category", transactionItem.category.id)
        cv.put("date", transactionItem.date.time)

        if (transactionItem.id == TransactionItem.NOID) {
            listeners.forEach { l -> l.transactionInserted(transactionItem) }
            return db!!.insertOrThrow(TABLE_TRANSACTIONS, null, cv)
        } else {
            val itemOld = fetchTransactionById(transactionItem.id)
            listeners.forEach { l -> l.transactionUpdated(transactionItem, itemOld) }
            return db!!.update(TABLE_TRANSACTIONS, cv, "id=?", arrayOf(transactionItem.id.toString())).toLong()
        }
    }

    fun insertOrUpdate(category: Category): Long {
        openDB() // ensure db != null
        val cv = ContentValues()
        cv.put("name", category.name)
        cv.put("direction", category.direction)
        cv.put("lastUsed", category.lastUsed)
        val result: Long
        if (category.id == Category.NOID) {
            // check if category already exists (if so, abort without side effects)
            if (categoriesCache.values.any { predicate ->
                        predicate.name == category.name &&
                                predicate.direction == category.direction
                    }) return INSERT_ERROR.toLong()
            // if not - insert
            result = db!!.insertOrThrow(TABLE_CATEGORIES, null, cv)
        } else {
            result = db!!.update(TABLE_CATEGORIES, cv, "id=?", arrayOf(category.id.toString())).toLong()
        }
        refreshCategoriesCache()
        return result
    }

    fun remove(transactionItem: TransactionItem): Boolean {
        openDB() // ensure db != null
        val result = db!!.delete(TABLE_TRANSACTIONS, "id=?", arrayOf(transactionItem.id.toString()))
        if (result == 1) listeners.forEach { l -> l.transactionRemoved(transactionItem) }
        return result == 1
    }

    fun remove(category: Category): Boolean {
        openDB() // ensure db != null
        db!!.delete(TABLE_TRANSACTIONS, "category=?", arrayOf(category.id.toString()))
        return db!!.delete(TABLE_CATEGORIES, "id=?", arrayOf(category.id.toString())) == 1
                .also { refreshCategoriesCache() }
    }

    fun getCategories(alwaysSortAlphabetically: Boolean = false): List<Category> {
        openDB() // ensure db != null
        val categories: MutableList<Category> = ArrayList(categoriesCache.values)
        val orderLRU = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_categoryorder), false) && !alwaysSortAlphabetically
        return if (orderLRU) {
            categories.sortedBy(Category::lastUsed).reversed()
        } else {
            // outgoing first
            categories.sortedWith(compareBy({ c -> -c.direction }, Category::name))
        }
    }

    /**
     * get category by plain name or name with appended direction (i.e. result of Category's toString method
     */
    fun getCategoryForName(name: String): Category? {
        return categoriesCache.values.find { c -> c.toString() == name } ?: categoriesCache.values.find { c -> c.name == name }
    }

    private fun refreshCategoriesCache() {
        if (!isOpeningDb) openDB() // might lead to double refreshCategoriesCache call
        categoriesCache.clear()
        val cursor: Cursor = db!!.rawQuery("SELECT * FROM $TABLE_CATEGORIES;", null)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val name = cursor.getString(1)
            val direction = cursor.getInt(2)
            val lastUsed = cursor.getLong(3)
            categoriesCache[id] = Category(id, name, direction, lastUsed)
        }
        cursor.close()
    }

    private fun fetchTransactionsBetween(from: Date, until: Date): List<TransactionItem> {
        // openDB not necessary, is called in public method(s)
        val cursor = db!!.rawQuery("SELECT * " +
                "FROM $TABLE_TRANSACTIONS " +
                "WHERE date>=? AND date <=? " +
                "ORDER BY date DESC",
                arrayOf(from.time.toString(), until.time.toString()))
        return fetchTransactionsFromCursor(cursor)
    }

    fun fetchTransactionsBetweenForCategory(from: Date, until: Date, category: Category): List<TransactionItem> {
        openDB() // ensure db != null
        val cursor = db!!.rawQuery("SELECT * " +
                "FROM $TABLE_TRANSACTIONS " +
                "WHERE date>=? AND date <=? AND category=? " +
                "ORDER BY date DESC",
                arrayOf(from.time.toString(), until.time.toString(), category.id.toString()))
        return fetchTransactionsFromCursor(cursor)
    }

    /**
     *
     * @param cursor the cursor to fetch the transactions from, is closed in the end
     * @return the queried transactions
     */
    private fun fetchTransactionsFromCursor(cursor: Cursor): List<TransactionItem> {
        // openDB not necessary, is called in public method(s)
        val transactions: MutableList<TransactionItem> = ArrayList()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val secondParty = cursor.getString(1)
            val amount = cursor.getDouble(2)
            val title = if (cursor.isNull(3)) null else cursor.getString(3)
            val category = categoriesCache[cursor.getLong(4)]!! // cannot be null
            val date = Date(cursor.getLong(5))

            transactions.add(TransactionItem(id, secondParty, amount, title, category, date))
        }
        cursor.close()
        return transactions
    }

    private fun fetchTransactionById(id: Long): TransactionItem {
        openDB() // ensure db != null
        val cursor = db!!.rawQuery("SELECT * " +
                "FROM $TABLE_TRANSACTIONS " +
                "WHERE id=?",
                arrayOf(id.toString()))
        return fetchTransactionsFromCursor(cursor)[0]
    }

    fun fetchMonth(month: Int, year: Int): List<TransactionItem> {
        openDB() // ensure db != null
        val from = Calendar.getInstance()
        val until = Calendar.getInstance()
        from.set(year, month, 1, 0, 0, 0)
        until.set(year, month, from.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        return fetchTransactionsBetween(from.time, until.time)
    }

    fun fetchCategoriesSummed(from: Date, until: Date): Map<Category, Double> {
        openDB() // ensure db != null
        val categorySums: MutableMap<Category, Double> = HashMap()
        val cursor = db!!.rawQuery("SELECT category, SUM(amount) " +
                "FROM $TABLE_TRANSACTIONS " +
                "WHERE date >=? AND date <=? " +
                "GROUP BY category " +
                "ORDER BY category ASC",
                arrayOf(from.time.toString(), until.time.toString()))
        while (cursor.moveToNext()) {
            val category = categoriesCache[cursor.getLong(0)]!! // cannot be null
            val sum = cursor.getDouble(1)
            categorySums[category] = sum
        }
        cursor.close()
        return categorySums
    }


    // SECONDPARTY AUTOCOMPLETE

    fun fetchSecondpartyAutocompleteItems(): List<Pair<String, Int>> {
        openDB() // ensure db != null
        val cursor = db!!.rawQuery("SELECT name, count " +
                "FROM $TABLE_SECONDPARTY_INDEX " +
                "ORDER BY count DESC, name ASC;", null)
        val autocompleteItems: MutableList<Pair<String, Int>> = ArrayList()
        while (cursor.moveToNext()) {
            autocompleteItems.add(cursor.getString(0) to cursor.getInt(1))
        }
        cursor.close()
        return autocompleteItems
    }

    fun insertOrUpdateSecondpartyAutocompleteItem(name: String, count: Int) {
        openDB() // ensure db != n
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("count", count)
        val updateResult = db!!.update(TABLE_SECONDPARTY_INDEX, cv, "name=?", arrayOf(name))
        if (updateResult == 0) db!!.insert(TABLE_SECONDPARTY_INDEX, null, cv) // nothing updated -> entry missing
    }

    fun removeSecondpartyAutocompleteItem(name: String) {
        db!!.delete(TABLE_SECONDPARTY_INDEX, "name=?", arrayOf(name))
    }

    // END AUTOCOMPLETE

    private fun createDefaultCategories(initDb: SQLiteDatabase) {
        val categories = listOf(
                Category("Geschenk", Category.INCOMING),
                Category("Einkommen", Category.INCOMING),
                Category("Unterhalt", Category.INCOMING),

                Category("Ernährung", Category.OUTGOING),
                Category("Geschenk", Category.OUTGOING),
                Category("Fixkosten", Category.OUTGOING),
                Category("Haushalt", Category.OUTGOING),
                Category("Freizeit", Category.OUTGOING))

        initDb.beginTransaction()
        var minId = Long.MAX_VALUE
        val stmt = initDb.compileStatement("INSERT INTO $TABLE_CATEGORIES " +
                "(name, direction) VALUES (?,?);")
        categories.forEach { c ->
            stmt.bindString(1, c.name)
            stmt.bindLong(2, c.direction.toLong())
            minId = min(stmt.executeInsert(), minId)
        }
        if (minId > -1) initDb.setTransactionSuccessful() // no error occured
        else logger.log(Level.SEVERE, "creation of default categories failed")

        initDb.endTransaction()
    }

    interface DbFinTransactionEventCallbacks {
        /**
         * @param transactionItem the inserted transaction *without* id == @see{TransactionItem.NOID}, not the assigned id in the database!
         */
        fun transactionInserted(transactionItem: TransactionItem)

        fun transactionUpdated(transactionItem: TransactionItem, transactionItemOld: TransactionItem)
        fun transactionRemoved(transactionItem: TransactionItem)
    }

    fun registerDbFinTransactionEventCallbacks(listener: DbFinTransactionEventCallbacks) {
        listeners.add(listener)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(CREATE_TABLE_CATEGORIES)
        db.execSQL(CREATE_TABLE_TRANSACTIONS)
        db.execSQL(CREATE_TABLE_SECONDPARTYINDEX)
        db.execSQL(CREATE_INDEX_SECONDPARTYINDEX)

        createDefaultCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db!!.beginTransaction()
            try {
                // move transactions to temp table and setup new tables
                db.execSQL("CREATE TABLE transacttmp AS SELECT * FROM $TABLE_TRANSACTIONS") // old schema
                db.execSQL("DROP TABLE $TABLE_TRANSACTIONS")
                db.execSQL(CREATE_TABLE_CATEGORIES)
                db.execSQL(CREATE_TABLE_TRANSACTIONS)

                // extract existing categories and migrate them
                val categoryLegacyToId: MutableMap<String, Long> = HashMap()
                var c = db.rawQuery("SELECT DISTINCT category FROM transacttmp", emptyArray())
                var cv = ContentValues()
                while (c.moveToNext()) {
                    val legacyName = c.getString(0)
                    val direction: Int
                    if (legacyName.contains("(A)") || legacyName.contains("(E)")) {
                        direction = if (legacyName.contains("(A)")) Category.OUTGOING else Category.INCOMING
                    } else {
                        c.close()
                        throw IllegalArgumentException("invalid category in DB!")
                    }
                    val name = legacyName.substring(0, legacyName.length - 1 - 3) // -1 to correct length/zero-based and -3 to remove (A)/(E) and preceeding whitespace (not -4 as end is handled exclusive)
                    cv.put("name", name)
                    cv.put("direction", direction)
                    val categoryId = db.insert(TABLE_CATEGORIES, null, cv)
                    cv.clear() // clear for next run
                    categoryLegacyToId[legacyName] = categoryId
                }
                c.close()

                // transfer legacy transactions
                c = db.rawQuery("SELECT * FROM transacttmp", emptyArray())
                cv = ContentValues() // create new object for new job
                while (c.moveToNext()) {
                    val secondParty = c.getString(1)
                    val amount = c.getDouble(2)
                    val title = if (c.isNull(3)) null else c.getString(3)
                    val categoryId = categoryLegacyToId[c.getString(4)]
                    val rawDate = c.getLong(5)

                    cv.put("secondparty", secondParty)
                    cv.put("amount", amount)
                    cv.put("title", title)
                    cv.put("category", categoryId)
                    cv.put("date", rawDate)
                    db.insert(TABLE_TRANSACTIONS, null, cv)
                    cv.clear()
                }

                // clean up
                db.execSQL("DROP TABLE transacttmp")

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while upgrading DB!");
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 4) {
            // extract values for secondparty autocomplete
            db!!.execSQL(CREATE_TABLE_SECONDPARTYINDEX)
            val c = db.rawQuery("SELECT secondparty, COUNT(secondparty) " +
                    "FROM $TABLE_TRANSACTIONS " +
                    "GROUP BY secondparty", null)
            val cv = ContentValues()
            while (c.moveToNext()) {
                cv.put("name", c.getString(0))
                cv.put("count", c.getInt(1))
                db.insert(TABLE_SECONDPARTY_INDEX, null, cv)
            }
            c.close()

            // create index after insertions (deferred for performance)
            db.execSQL(CREATE_INDEX_SECONDPARTYINDEX)
        }
        if (oldVersion < 5) {
            db!!.execSQL("ALTER TABLE $TABLE_CATEGORIES ADD COLUMN lastused INTEGER DEFAULT 0;") // init to 1970
        }
    }
}