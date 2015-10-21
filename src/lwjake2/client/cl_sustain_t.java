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

package lwjake2.client;

/**
 * cl_sustain_t
 */
public class cl_sustain_t {
    int id;
    int type;
    int endtime;
    int nextthink;
    int thinkinterval;
    float[] org = new float[3];
    float[] dir = new float[3];
    int color;
    int count;
    int magnitude;
    ThinkAdapter think;

    void clear() {
        org[0] = org[1] = org[2] =
                dir[0] = dir[1] = dir[2] =
                        id = type = endtime = nextthink = thinkinterval = color = count = magnitude = 0;
        think = null;
    }

    static abstract class ThinkAdapter {
        abstract void think(cl_sustain_t self);
    }
}
