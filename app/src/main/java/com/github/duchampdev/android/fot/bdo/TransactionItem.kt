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

package com.github.duchampdev.android.fot.bdo

import com.github.duchampdev.android.fot.extensions.nullForEmpty
import java.io.Serializable
import java.util.*

data class TransactionItem(val id: Long, var secondParty: String, var amount: Double, var title: String?, var category: Category, var date: Date) : Serializable {

    companion object {
        const val NOID = -1L
    }

    constructor(secondParty: String, amount: Double, title: String?, category: Category, date: Date): this(NOID, secondParty, amount, title?.nullForEmpty(), category, date)

    constructor(secondParty: String, amount: Double, title: String?, category: Category): this(NOID, secondParty, amount, title, category, Calendar.getInstance().time)
}