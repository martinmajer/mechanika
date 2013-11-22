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
 * Podpora / vnější reakce.
 * @author Martin
 */
public abstract class Support implements ModelEntity, Serializable {

	static final long serialVersionUID = 6001L;


	/** Působiště sil podpory. */
	public VectorXZ origin;

	/** Směr podpory. */
	public VectorXZ direction;

	/** Aktivní? */
	public transient boolean enabled = false;


	public void reset() {
		enabled = false;
	}


	/** Vrátí reakce, které tato podpora tvoří. */
	public abstract List <Reaction> getReactions();


	public abstract String toLongString();


	public String getInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}


	/** Vrátí směr síly jako úhel. */
	public int getDirectionAngle() {
		int angle = (int)Math.round((Math.atan2(direction.z, direction.x)*180/Math.PI));
		if (angle < 0) angle += 360;
		return angle;
	}


	/** Vetknutí. */
	public static class Fixed extends Support {

		static final long serialVersionUID = 6101L;

		transient List <Reaction> reactions = null;

		transient Force.Reaction x, z;
		transient Moment.Reaction m;

		@Override
		public List <Reaction> getReactions() {
			if (reactions == null) {
				reactions = new LinkedList <Reaction>();
				x = new Force.Reaction();
				x.direction = new VectorXZ(1, 0);
				x.origin = origin;
				z = new Force.Reaction();
				z.direction = new VectorXZ(0, 1);
				z.origin = origin;
				m = new Moment.Reaction();
				m.origin = origin;
				reactions.add(x);
				reactions.add(z);
				reactions.add(m);
			}
			return reactions;
		}

		@Override
		public String toString() {
			return "Vetknutí";
		}

		@Override
		public String toLongString() {
			if (x == null) return "Vetknutí / neaktivní";
			return String.format(Locale.ENGLISH, "Vektnutí / → %s = %.3f; ↓ %s = %.3f; m: %s = %.3f", x.name, x.size, z.name, z.size, m.name, m.size);
		}

		@Override
		public void reset() {
			super.reset();
			reactions = null;
		}

	}

	/** Kloubová podpora. */
	public static class Pinned extends Support {

		static final long serialVersionUID = 6201L;

		transient List <Reaction> reactions = null;

		transient Force.Reaction a, b;

		@Override
		public List <Reaction> getReactions() {
			if (reactions == null) {
				reactions = new LinkedList <Reaction>();
				a = new Force.Reaction();
				a.direction = new VectorXZ(direction.x, direction.z);
				a.origin = origin;
				b = new Force.Reaction();
				b.direction = new VectorXZ(-direction.z, direction.x);
				b.origin = origin;
				reactions.add(a);
				reactions.add(b);
			}
			return reactions;
		}

		@Override
		public String toString() {
			return "Pevný kloub";
		}

		@Override
		public String toLongString() {
			if (a == null) return "Pevný kloub / neaktivní";
			return String.format(Locale.ENGLISH, "Pevný kloub / ↑ %s = %.3f; → %s = %.3f", a.name, a.size, b.name, b.size);
		}

		@Override
		public void reset() {
			super.reset();
			reactions = null;
		}

	}

	/** Posuvná podpora. */
	public static class Roller extends Support {

		static final long serialVersionUID = 6301L;

		transient List <Reaction> reactions = null;

		transient Force.Reaction r;

		@Override
		public List <Reaction> getReactions() {
			if (reactions == null) {
				reactions = new LinkedList <Reaction>();
				r = new Force.Reaction();
				r.direction = new VectorXZ(direction.x, direction.z);
				r.origin = origin;
				reactions.add(r);
			}
			return reactions;
		}

		@Override
		public String toString() {
			return "Posuvný kloub";
		}

		@Override
		public String toLongString() {
			if (r == null) return "Posuvný kloub / neaktivní";
			return String.format(Locale.ENGLISH, "Posuvný kloub / ↑ %s = %.3f", r.name, r.size);
		}

		@Override
		public void reset() {
			super.reset();
			reactions = null;
		}

	}

	public static class Rod extends Support {

		static final long serialVersionUID = 6401L;

		public float length;

		transient List <Reaction> reactions = null;

		transient Force.Reaction r;

		@Override
		public List<Reaction> getReactions() {
			if (reactions == null) {
				reactions = new LinkedList <Reaction>();
				r = new Force.Reaction();
				r.direction = new VectorXZ(direction.x, direction.z);
				r.origin = origin;
				reactions.add(r);
			}
			return reactions;
		}

		public VectorXZ getEnd() {
			return new VectorXZ(origin.x + direction.x*length, origin.z + direction.z*length);
		}

		@Override
		public String toString() {
			return "Vnější táhlo";
		}

		@Override
		public String toLongString() {
			if (r == null) return "Vnější táhlo / neaktivní";
			return String.format(Locale.ENGLISH, "Táhlo / → ← %s = %.3f", r.name, r.size);
		}


		@Override
		public void reset() {
			super.reset();
			reactions = null;
		}

	}

}
