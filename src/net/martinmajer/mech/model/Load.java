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
import java.util.*;

/**
 * Spojité zatížení.
 * @author Martin
 */
public class Load implements ModelEntity, Serializable {

	public static final long serialVersionUID = 3001L;


	/** Počátek působení. */
	public VectorXZ start;

	/** Konec působení. */
	public VectorXZ end;

	/** Velikost na jeden pixel. */
	public float sizePerPixel;

	/** Směr působení. */
	public VectorXZ direction;

	
	public String name;


	/** Aktivní úseky zatížení. */
	public transient List <Float> activeParts;

	/** Pomocná proměnná pro určení vnitřních sil. */
	public transient boolean started = false;


	public Load() {
		reset();
	}

	public void reset() {
		activeParts = new ArrayList <Float>();
		started = false;
	}

	/** Vrátí směr síly jako úhel. */
	public int getDirectionAngle() {
		int angle = (int)Math.round((Math.atan2(direction.z, direction.x)*180/Math.PI));
		if (angle < 0) angle += 360;
		return angle;
	}

	/** Vrátí velikost zatížení. */
	public float getSize() {
		float length = (float)Math.sqrt((start.x-end.x)*(start.x-end.x) + (start.z-end.z)*(start.z-end.z));
		return sizePerPixel * length;
	}

	/** Vrátí střed zatížení - těžiště. */
	public VectorXZ getCenter() {
		return new VectorXZ((start.x+end.x)/2, (start.z+end.z)/2);
	}

	@Override
	public String toString() {
		return "Zatížení " + name;
	}

	public String toLongString() {
		return String.format(Locale.ENGLISH, "Zatížení %s / %.3f kN / %.3f kN/m / [%.3f; %.3f] -> [%.3f; %.3f] / [%d°; (%.2f; %.2f)]", name, getSize(), sizePerPixel, start.x, start.z, end.x, end.z, getDirectionAngle(), direction.x, direction.z);
	}

	public String getInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
