/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package kuake2.sys;

import kuake2.Globals;
import kuake2.qcommon.Com;

public abstract class Timer {

    static Timer t;

    static {
        try {
            t = new NanoTimer();
        } catch (Throwable e) {
            t = new StandardTimer();
        }
        Com.Println("using " + t.getClass().getName());
    }

    public static int Milliseconds() {
        return Globals.curtime = (int) (t.currentTimeMillis());
    }

    abstract public long currentTimeMillis();
}
