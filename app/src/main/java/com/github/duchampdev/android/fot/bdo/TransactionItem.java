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

package com.github.duchampdev.android.fot.bdo;


import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;


public class TransactionItem implements Serializable {
    private final long id;
    private String secondParty;
    private double amount;
    private String title;
    private Category category;
    private Date date;

    public static final int NOID = -1;

    public TransactionItem(long id, String secondParty, double amount, String title, Category category, Date date) {
        this.id = id;
        this.secondParty = secondParty;
        this.amount = amount;
        this.title = title;
        this.category = category;
        this.date = date;
    }

    public TransactionItem(String secondParty, double amount, String title, Category category, Date date) {
        this(NOID, secondParty, amount, title.isEmpty() ? null : title, category, date);
    }

    public TransactionItem(String secondParty, double amount, String title, Category category) {
        this(NOID, secondParty, amount, title.isEmpty() ? null : title, category, Calendar.getInstance().getTime());
    }

    public long getId() {
        return id;
    }

    public String getSecondParty() {
        return secondParty;
    }

    public void setSecondParty(String secondParty) {
        this.secondParty = secondParty;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
