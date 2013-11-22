/*
	Mechanika
    Copyright (C) 2011 Martin Majer

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.martinmajer.mech.model;

import java.io.Serializable;

/**
 * Vektor / bod.
 * @author Martin
 */
public class VectorXZ implements Cloneable, Serializable {

	/** X-ová souřadnice. */
	public final float x;

	/** Z-ová souřadnice. */
	public final float z;
	


	public VectorXZ(VectorXZ v) {
		this.x = v.x;
		this.z = v.z;
	}

	public VectorXZ(float x, float z) {
		this.x = x; this.z = z;
	}

	public VectorXZ() {
		this.x = 0; this.z = 0;
	}

	/** Normalizuje vektor. */
	public VectorXZ normalize() {
		float size = size();
		return new VectorXZ(x / size, z / size);
	}

	/** Skalární součin. */
	public float dotProduct(VectorXZ v) {
		return x*v.x + z*v.z;
	}

	/** Skalární součin. */
	public float dotProduct(float x, float z) {
		return this.x*x + this.z*z;
	}

	/** Vrátí velikost vektoru. */
	public float size() {
		return (float)Math.sqrt(x*x+z*z);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o instanceof VectorXZ) {
			VectorXZ p = (VectorXZ)o;
			if (this.x == p.x && this.z == p.z) return true;
			else return false;
		}
		else return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 11 * hash + Float.floatToIntBits(this.x);
		hash = 11 * hash + Float.floatToIntBits(this.z);
		return hash;
	}

	@Override
	public VectorXZ clone() {
		return new VectorXZ(this.x, this.z);
	}


	@Override
	public String toString() {
		return "[" + x + "; " + z + "]";
	}


	public java.awt.geom.Point2D.Float getAwtPoint() {
		return new java.awt.geom.Point2D.Float(x, z);
	}

}
