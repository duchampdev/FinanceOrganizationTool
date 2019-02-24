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


public class Category implements Serializable {

    private final long id;
    private String name;
    private int direction;
    private long lastUsed;

    public static final int INCOMING = 0;
    public static final int OUTGOING = 1;
    public static final int NOID = -1;


    public Category(long id, String name, int direction, long lastUsed) {
        this.id = id;
        this.name = name;
        this.direction = direction;
        this.lastUsed = lastUsed;
    }

    public Category(String name, int direction) {
        this(NOID, name, direction, 0);
    }


    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public String toString() {
        return name + (direction == INCOMING ? " (E)" : " (A)");
    }
}
