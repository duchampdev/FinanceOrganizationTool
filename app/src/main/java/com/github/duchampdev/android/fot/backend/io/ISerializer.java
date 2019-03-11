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

import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.bdo.Category;
import com.github.duchampdev.android.fot.bdo.TransactionItem;

import java.io.File;
import java.util.List;


public interface ISerializer {

    boolean export(File target, List<TransactionItem> transactions, List<Category> categories);
    boolean importFile(File source, FinanceOrgaToolDB dbInstance);
}
