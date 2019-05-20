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

package com.github.duchampdev.android.fot.bdo

import java.io.Serializable

data class Category(val id: Long, var name: String, var direction: Int, var lastUsed: Long) : Serializable {

    companion object {
        const val INCOMING = 0
        const val OUTGOING = 1
        const val NOID = -1L
    }

    constructor(name: String, direction: Int) : this(NOID, name, direction, 0)

    override fun toString(): String {
        return name + (if (direction == INCOMING) " (E)" else " (A)")
    }
}