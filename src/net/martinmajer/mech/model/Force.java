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
import java.util.Locale;

/**
 * Síla.
 * @author Martin
 */
public abstract class Force implements ModelEntity, Serializable {

	public static final long serialVersionUID = 2001L;

	/** Působiště síly. */
	public VectorXZ origin;

	/** Jednotkový vektor směru, ve kterém působí síla. */
	public VectorXZ direction;

	/** Velikost síly [kN]. */
	public float size;

	/** Jméno síly. */
	public String name;


	/** Vrátí směr síly jako úhel. */
	public int getDirectionAngle() {
		int angle = (int)Math.round((Math.atan2(direction.z, direction.x)*180/Math.PI));
		if (angle < 0) angle += 360;
		return angle;
	}



	@Override
	public String toString() {
		return "Síla " + name;
	}

	public String toLongString() {
		return String.format(Locale.ENGLISH, "Síla %s / %.0f kN / [%.3f; %.3f] / [%d°; (%.2f; %.2f)]", name, size, origin.x, origin.z, getDirectionAngle(), direction.x, direction.z);
	}

	public String getInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}


	/** Akce (osamělá síla). */
	public static class Action extends Force implements Serializable {

		public static final long serialVersionUID = 2101L;

		/** False, pokud síla nepůsobí na žádný nosník / kloub. */
		public transient boolean enabled = false;


		/** Resetuje sílu. */
		public void reset() {
			enabled = false;
		}

		@Override
		public String toLongString() {
			return String.format(Locale.ENGLISH, "Síla %s %s/ %.0f kN / [%.3f; %.3f] / [%d°; (%.2f; %.2f)]", name, enabled ? "" : "(neaktivní) ", size, origin.x, origin.z, getDirectionAngle(), direction.x, direction.z);
		}

	}

	/** Reakce (osamělá síla). */
	public static class Reaction extends Force implements net.martinmajer.mech.model.Reaction {

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setSize(float size) {
			this.size = size;
		}
		
		public float getSize() {
			return size;
		}

	}

}
