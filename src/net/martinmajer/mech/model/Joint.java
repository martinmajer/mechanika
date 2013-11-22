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

import java.util.*;

/**
 * Styčník.
 * @author Martin
 */
public class Joint implements ModelEntity {

	/** Umístění nosníku. */
	public VectorXZ position;

	/** Seznam nosníků připojených ke kloubu. */
	public List <Beam> beams = new LinkedList <Beam>();

	/** Seznam sil, které působí na kloubu. */
	public List <Force> forces = new LinkedList <Force>();

	/** Seznam momentů, které působí na kloubu. */
	public List <Moment> moments = new LinkedList <Moment>();

	/** Seznam podpor, do kterých je kloub uchycen. */
	public List <Support> supports = new LinkedList <Support>();


	/** Jsou všechny nosníky táhla, je kloub hmotným bodem? */
	public boolean allRods = true;

	/** Jména reakcí. */
	public List <Reaction> reactions = new LinkedList <Reaction>();


	/** Je kloub pevný, vytváří i moment? */
	public boolean stiff = false;
	

	public Joint(VectorXZ p) {
		this.position = p.clone();
	}
	

	@Override
	public String toString() {
		return String.format(Locale.ENGLISH, "Kloub st. %d", this.beams.size());
	}

	public String toLongString() {
		String reactionsStr = null;
		if (!allRods) {
			StringBuilder sb = new StringBuilder(100);
			for (Reaction r: reactions) {
				sb.append(String.format(Locale.ENGLISH, "%s = %.3f ", r.getName(), r.getSize()));
			}
			if (reactions.size() == 0) sb.append("N/A");
			reactionsStr = sb.toString();
		}
		return String.format(Locale.ENGLISH, "Kloub [%.3f; %.3f] / stupeň %d%s", this.position.x, this.position.z, this.beams.size(), allRods ? " / hm. bod" : " / " + reactionsStr);
	}


	public String getInfo() {
		StringBuilder sb = new StringBuilder(500);
		sb.append(toLongString()); sb.append("\n\n");

		if (allRods) {
			sb.append("Zatížení hmotného bodu:\n");
			for (Force force: forces) {
				sb.append(force.toLongString()); sb.append("\n");
			}
		}
		else {
			Beam current = null, prev = null;

			Iterator <Reaction> reactionsIterator = reactions.iterator();

			for (Beam beam: beams) {
				if (beam.isRod) continue; // táhla přeskočíme
				prev = current;
				current = beam;
				if (prev != null) {
					sb.append(prev.toString()); sb.append(" -> ");
					sb.append(current.toString());
					sb.append(" / x: ");
					sb.append(reactionsIterator.next().getName());
					sb.append(", z: ");
					sb.append(reactionsIterator.next().getName());
					if (stiff) {
						sb.append(", m: ");
						sb.append(reactionsIterator.next().getName());
					}
					sb.append("\n");
				}
			}
		}

		return sb.toString();
	}


	/** Vrátí true, pokud je na kloubu vetknutí. */
	public boolean hasFixedSupport() {
		for (Support support: supports) {
			if (support instanceof Support.Fixed) return true;
		}
		return false;
	}

}
