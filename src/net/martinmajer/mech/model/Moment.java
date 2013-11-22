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
 * Moment.
 * @author Martin
 */
public abstract class Moment implements ModelEntity, Serializable {

	static final long serialVersionUID = 5001L;

	/** Velikost momentu [kN*m]. */
	public float size;

	/** Působiště momentu. */
	public VectorXZ origin;

	/** Jméno neznámé. */
	public String name;
	
	
	//public boolean positive = true;


	@Override
	public String toString() {
		return "Moment " + name;
	}

	public String toLongString() {
		return String.format(Locale.ENGLISH, "Moment %s / %.0f kNm / [%.3f; %.3f]", name, size, origin.x, origin.z);
	}

	public String getInfo() {
		return null;
	}

	public static class Action extends Moment implements Serializable {

		public static final long serialVersionUID = 5101L;

		public transient boolean enabled;

		/** Resetuje sílu. */
		public void reset() {
			enabled = false;
		}

	}

	/** Reakce. */
	public static class Reaction extends Moment implements net.martinmajer.mech.model.Reaction {

		public boolean opposite = false;

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
