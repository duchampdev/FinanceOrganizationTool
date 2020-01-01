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

package com.github.duchampdev.android.fot.backend

import android.content.Context
import com.github.duchampdev.android.fot.bdo.TransactionItem

class SecondpartyAutocompleteService private constructor(context: Context) {

    companion object {
        private lateinit var instance: SecondpartyAutocompleteService

        fun getInstance(context: Context): SecondpartyAutocompleteService {
            if (!::instance.isInitialized) instance = SecondpartyAutocompleteService(context)
            return instance
        }
    }

    private val dbInstance = FinanceOrgaToolDB.getInstance(context)
    private val indexItems: MutableMap<String, Int> = HashMap()
    private lateinit var orderedItems: Array<String>


    init {
        setupDbCallbacks()
        reloadIndex()
    }


    fun setupDbCallbacks() {
        dbInstance.registerDbFinTransactionEventCallbacks(object : FinanceOrgaToolDB.DbFinTransactionEventCallbacks {
            override fun transactionInserted(transactionItem: TransactionItem) {
                addUsage(transactionItem.secondParty)
                reloadIndex()
            }

            override fun transactionUpdated(transactionItem: TransactionItem, transactionItemOld: TransactionItem) {
                if (transactionItem.secondParty != transactionItemOld.secondParty) {
                    removeUsage(transactionItemOld.secondParty)
                    addUsage(transactionItem.secondParty)
                    reloadIndex()
                }
            }

            override fun transactionRemoved(transactionItem: TransactionItem) {
                removeUsage(transactionItem.secondParty)
                reloadIndex()
            }
        })
    }

    private fun addUsage(name: String) {
        val newCount = indexItems[name]?.plus(1) ?: 1
        dbInstance.insertOrUpdateSecondpartyAutocompleteItem(name, newCount)
    }

    private fun removeUsage(name: String) {
        val newCount = indexItems[name]?.minus(1) ?: return
        if (newCount == 0) dbInstance.removeSecondpartyAutocompleteItem(name)
        else dbInstance.insertOrUpdateSecondpartyAutocompleteItem(name, newCount)
    }

    fun getProposals() = orderedItems


    private fun reloadIndex() {
        indexItems.clear()
        indexItems.putAll(dbInstance.fetchSecondpartyAutocompleteItems())
        orderedItems = indexItems.keys.toTypedArray()
    }

}