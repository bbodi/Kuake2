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

package lwjake2.util;

import java.util.Vector;

/**
 * Vargs is a helper class to encapsulate printf arguments.
 * 
 * @author cwei
 */
public class Vargs {

	// initial capacity
	static final int SIZE = 5;

	Vector<Object> v;

	public Vargs() {
		this(SIZE);
	}

	public Vargs(int initialSize) {
		if (v != null)
			v.clear(); // clear previous list for GC
		v = new Vector<Object>(initialSize);
	}

	public Vargs add(boolean value) {
		v.add(new Boolean(value));
		return this;
	}

	public Vargs add(byte value) {
		v.add(new Byte(value));
		return this;
	}

	public Vargs add(char value) {
		v.add(new Character(value));
		return this;
	}

	public Vargs add(short value) {
		v.add(new Short(value));
		return this;
	}

	public Vargs add(int value) {
		v.add(new Integer(value));
		return this;
	}

	public Vargs add(long value) {
		v.add(new Long(value));
		return this;
	}

	public Vargs add(float value) {
		v.add(new Float(value));
		return this;
	}

	public Vargs add(double value) {
		v.add(new Double(value));
		return this;
	}

	public Vargs add(String value) {
		v.add(value);
		return this;
	}

	public Vargs add(Object value) {
		v.add(value);
		return this;
	}

	public Vargs clear() {
		v.clear();
		return this;
	}

	/* This apparently isn't even used? - flibit
	public Vector toVector() {
		//		Vector tmp = v;
		//		v = null;
		//		return tmp;
		return (Vector) v.clone();
	}
	*/

	public Object[] toArray() {
		return v.toArray();
	}

	public int size() {
		return v.size();
	}
}
