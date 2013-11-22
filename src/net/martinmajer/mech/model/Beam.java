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
 * Nosník.
 *
 * @author Martin
 */
public class Beam implements ModelEntity, Serializable {

	public static final long serialVersionUID = 1001L;

	/** Seznam hlavnícho bodů nosníku, které definují jeho tvar. */
	public List <VectorXZ> mainPoints = new ArrayList <VectorXZ>();

	/** Seznam hlavních bodů a kloubů nosníku. */
	public transient List <VectorXZ> allJoints;

	/** Seznam všech bodů nosníku, i těch, kde není nosník zalomený. */
	public transient List <VectorXZ> allPoints;

	/** Tvoří body uzavřený obrazec? */
	public transient boolean closed = false; // @todo nepoužívá se, odstranit!
											 // (nejspíš by vytvářelo staticky neurčité soustavy)


	public String name = null;

	public transient List <Load> loads;
	public transient boolean isRod = false;

	public transient String rodReactionName = null;
	public transient Force.Reaction rodReaction = null;

	public transient List <ForcesDistribution> internalForces = null;


	public Beam() {
		reset();
	}

	public Beam(VectorXZ ... points) {
		for (VectorXZ p: points) {
			this.mainPoints.add(p);
		}
		copyMainPointsToAll();
	}

	public Beam(float ... coords) {
		assert (coords.length % 2 == 0);
		for (int i = 0; i < coords.length; i += 2) {
			this.mainPoints.add(new VectorXZ(coords[i], coords[i+1]));
		}
		copyMainPointsToAll();
	}

	public void copyMainPointsToAll() {
		for (VectorXZ p: mainPoints) {
			allPoints.add(p);
			allJoints.add(p);
		}
	}

	/** Vrátí true, pokud je možné považovat nosník za táhlo. */
	public boolean canBeRod(Model model) {
		// nosník vede pouze mezi dvěma body a mimo hlavní body na něj nic nepůsobí
		if (mainPoints.size() == 2 && allPoints.size() == 2 && !closed) {
			if (model.joints.get(mainPoints.get(0)).stiff) return false;
			if (model.joints.get(mainPoints.get(1)).stiff) return false;
			return true;
		}
		return false;
	}

	public void reset() {
		loads = new LinkedList <Load>();
		isRod = false;
		rodReactionName = null;
		rodReaction = null;
		internalForces = null;
		this.allPoints = new LinkedList <VectorXZ>();
		this.allJoints = new LinkedList <VectorXZ>();
		copyMainPointsToAll();
	}

	/**
	 * Rozhodne, zda je zadaná úsečka částí nosníku.
	 * @return 1, pokud jde o část nosníku v zadaném pořadí,
	 *		   -1, pokud je pořadí opačné a 0, pokud úsečka na nosníku neleží
	 */
	public int isPartOfBeam(VectorXZ a, VectorXZ b) {
		VectorXZ prev = null;
		for (VectorXZ current: allJoints) {
			if (prev != null) {
				if (a.equals(prev) && b.equals(current)) return 1;
				if (a.equals(current) && b.equals(prev)) return -1;
			}
			prev = current;
		}
		return 0;
	}

	public void insertInnerJoint(VectorXZ after, VectorXZ point) {
		ListIterator <VectorXZ> it = allJoints.listIterator();

		// Nejdřív najdeme bod, za který budeme aktuální vnitřní bod vkládat
		while (it.hasNext()) {
			VectorXZ p = it.next();
			if (p.equals(after)) break;
		}

		// iterátor je nyní připravený na vložení bodu, případně došel ke konci
		// je však možné, že v segmentu za zadaným bodem jsou ještě jiné vnitřní
		// body, které je třeba přeskočit

		float distance2 = (point.x-after.x)*(point.x-after.x) + (point.z-after.z)*(point.z-after.z);
			// vzdálenost na druhou

		while (it.hasNext()) {
			VectorXZ p = it.next();
			float distance2_p = (p.x-after.x)*(p.x-after.x) + (p.z-after.z)*(p.z-after.z);
			if (distance2 < distance2_p) {
				// přeskočili jsme bod, vrátíme se a ukončíme smyčku
				it.previous();
				break;
			}
		}

		it.add(point);
	}

	public void insertInnerPoint(VectorXZ after, VectorXZ point) {
		ListIterator <VectorXZ> it = allPoints.listIterator();

		// Nejdřív najdeme bod, za který budeme aktuální vnitřní bod vkládat
		while (it.hasNext()) {
			VectorXZ p = it.next();
			if (p.equals(after)) break;
		}

		// iterátor je nyní připravený na vložení bodu, případně došel ke konci
		// je však možné, že v segmentu za zadaným bodem jsou ještě jiné vnitřní
		// body, které je třeba přeskočit

		float distance2 = (point.x-after.x)*(point.x-after.x) + (point.z-after.z)*(point.z-after.z);
			// vzdálenost na druhou

		while (it.hasNext()) {
			VectorXZ p = it.next();
			float distance2_p = (p.x-after.x)*(p.x-after.x) + (p.z-after.z)*(p.z-after.z);
			if (distance2 < distance2_p) {
				// přeskočili jsme bod, vrátíme se a ukončíme smyčku
				it.previous();
				break;
			}
		}

		it.add(point);
	}

	/** Nahradí zadaný bod seznamem nových bodů. */
	public void replacePoint(VectorXZ replaced, List <InnerPoint> points) {
		// najdeme bod
		ListIterator <VectorXZ> it = allPoints.listIterator();
		while (it.hasNext()) {
			VectorXZ p = it.next();
			if (replaced == p) {
				it.remove();
				for (InnerPoint ip: points) it.add(ip);
				break;
			}
		}
	}


	@Override
	public String toString() {
		return String.format(Locale.ENGLISH, "Nosník %s", this.name);
	}

	public String toLongString() {
		return String.format(Locale.ENGLISH, "Nosník %s%s", this.name, isRod ? (String.format(Locale.ENGLISH, " (táhlo %s = %.3f)", rodReactionName, rodReaction.size)) : "");
	}


	public String getInfo() {
		StringBuilder sb = new StringBuilder(1000);
		sb.append(toLongString()); sb.append("\n\nBody: ");

		for (VectorXZ point: mainPoints) {
			sb.append(point.toString()); sb.append(", ");
		}

		sb.append("\n\n");

		if (!isRod) {
			/*sb.append("Zatížení nosníku:\n");
			for (VectorXZ point: allPoints) {
				if (point instanceof InnerPoint) {
					InnerPoint ip = (InnerPoint)point;
					if (ip.force != null) {
						sb.append(ip.force.toLongString()); sb.append("\n");
					}
					else if (ip.moment != null) {
						sb.append(ip.moment.toLongString()); sb.append("\n");
					}
					else if (ip.load != null) {
						sb.append(ip.load.toLongString()); sb.append("\n");
					}
				}
			}*/

			sb.append("Průběhy vnitřních sil:\n");
			computeInternalForces();

			float currentS = 0;
			for (ForcesDistribution dist: internalForces) {
				if (dist.dirN == null) break; // konec?
				
				float nextS = currentS + dist.length;
				sb.append(String.format(Locale.ENGLISH, "s: (%.3f; %.3f), n = (%.3f; %.3f)\n", currentS, nextS, dist.dirN.x, dist.dirN.z));
				sb.append(String.format(Locale.ENGLISH, "    Nx = %.3f*(s-%.3f) + %.3f\n",
						dist.dirN.dotProduct(dist.dfx, dist.dfz), currentS,
						dist.dirN.dotProduct(dist.fx, dist.fz)
						));
				sb.append(String.format(Locale.ENGLISH, "    Vz = %.3f*(s-%.3f) + %.3f\n",
						dist.dirV.dotProduct(dist.dfx, dist.dfz), currentS,
						dist.dirV.dotProduct(dist.fx, dist.fz)
						));
				sb.append(String.format(Locale.ENGLISH, "    My = %.3f*(s-%.3f)^2 + %.3f*(s-%.3f) + %.3f\n",
						dist.ddm, currentS, dist.dm, currentS, dist.m
						));
				sb.append(String.format(Locale.ENGLISH, "    Nx(%.3f) = %.3f kN; Nx(%.3f) = %.3f kN\n",
						currentS, dist.nStart, nextS, dist.nEnd));
				sb.append(String.format(Locale.ENGLISH, "    Vz(%.3f) = %.3f kN; Vz(%.3f) = %.3f kN\n",
						currentS, dist.vStart, nextS, dist.vEnd));
				sb.append(String.format(Locale.ENGLISH, "    Mx(%.3f) = %.3f kNm; Mx(%.3f) = %.3f kNm\n",
						currentS, dist.m, nextS, dist.mEnd));
				if (!Float.isNaN(dist.msExtreme)) {
					sb.append(String.format(Locale.ENGLISH, "    Extrém Mx(%.3f) = %.3f kNm\n",
						dist.msExtreme + currentS, dist.mExtreme));
				}


				currentS = nextS;
			}
		}

		return sb.toString();
	}


	/** Funkce popisující průběh veličiny. */
	public static class ForcesDistribution {
		
		public float dfx, fx;		// Fx = dfx*s + fx
		public float dfz, fz;		// Fz = dfz*s + fz
		public float ddm, dm, m;	// My = ddm*s^2 + dm*s + m

		// bod nosníku, od kterého je průběh platný
		public float x, z;

		// vektor směru normálové síly a posouvající síly
		public VectorXZ dirN, dirV;

		// délka části nosníku
		public float length;

		// počáteční a koncové hodnoty sil
		public float nStart, nEnd;
		public float vStart, vEnd;

		// koncový a extrémní moment (počáteční moment = m)
		public float mEnd;
		public float mExtreme;
		public float msExtreme = Float.NaN; // hodnota s, pro kterou je moment extrémní

		public VectorXZ getStartingPoint() {
			return new VectorXZ(x, z);
		}
	}

	/** Vypočítá průběhy vnitřních sil. */
	public void computeInternalForces() {
		if (internalForces != null) return;

		internalForces = new ArrayList <ForcesDistribution>();

		float fx = 0, fz = 0;
		float dfx = 0, dfz = 0;
		float mJump = 0;

		ListIterator <VectorXZ> it = allPoints.listIterator();

		VectorXZ last = null;

		while (it.hasNext()) {
			boolean samePoint = true;
			VectorXZ current = it.next(), next = null;

			// k síle přičteme přírůstek za vzdálenost uraženou od minulého bodu
			if (last != null) {
				float length = (float)Math.sqrt((current.x-last.x)*(current.x-last.x) + (current.z-last.z)*(current.z-last.z));
				fx += length * dfx;
				fz += length * dfz;
			}

			mJump = 0; // řešíme jen skokové snížení / zvýšení, ne celý průběh

			while (samePoint) {
				if (current instanceof InnerPoint) {
					InnerPoint inner = (InnerPoint)current;
					if (inner.force != null) {
						fx -= inner.force.size * inner.force.direction.x;
						fz -= inner.force.size * inner.force.direction.z;
					}
					if (inner.load != null) {
						if (!inner.load.started) {
							inner.load.started = true;
							dfx -= inner.load.sizePerPixel * inner.load.direction.x;
							dfz -= inner.load.sizePerPixel * inner.load.direction.z;
						}
						else {
							inner.load.started = false;
							dfx += inner.load.sizePerPixel * inner.load.direction.x;
							dfz += inner.load.sizePerPixel * inner.load.direction.z;
						}
					}
					// započítáváme pouze skokové změny momentu, zbytek dopočítáme
					// při vykreslování pomocí Schwedlerovy věty
					if (inner.moment != null) {
						if (inner.moment instanceof Moment.Reaction && ((Moment.Reaction)inner.moment).opposite) {
							mJump += inner.moment.size;
						}
						else {
							mJump -= inner.moment.size;
						}
					}
				}

				if (it.hasNext()) {
					next = it.next();
					if (current.equals(next)) current = next;
					else {
						samePoint = false;
						it.previous();
					}
				}
				else break;
			}

			ForcesDistribution dist = new ForcesDistribution();
			dist.x = current.x;
			dist.z = current.z;
			dist.fx = fx;
			dist.fz = fz;
			dist.dfx = dfx;
			dist.dfz = dfz;
			dist.m = mJump; // zatím pouze skokové zvýšení momentu na začátku úseku,
							// celá počáteční podmínka je dopočítána až v dalším kroku

			internalForces.add(dist);

			last = current;
		}
		

		Iterator <Beam.ForcesDistribution> it2 = internalForces.listIterator();
		Beam.ForcesDistribution current = null, next = null;

		float endMoment = 0;

		while (it2.hasNext()) {
			current = next;
			next = it2.next();
			if (current == null) continue;

			VectorXZ line = new VectorXZ(next.x - current.x, next.z - current.z);
			float length = line.size();
			current.length = length;

			// směrový vektor vykreslované části nosníku (směr normálové síly)
			VectorXZ dirN = line.normalize();
			// normálový vektor vykreslované části nosníku (směr posouvající síly)
			VectorXZ dirV = new VectorXZ(-dirN.z, dirN.x);

			current.dirN = dirN;
			current.dirV = dirV;


			// průběh vnitřních sil jako vektor
			VectorXZ f = new VectorXZ(current.fx, current.fz);
			VectorXZ df = new VectorXZ(current.dfx, current.dfz);

			// průběh normálové síly
			float fn = f.dotProduct(dirN);
			float dfn = df.dotProduct(dirN);

			current.nStart = fn;
			current.nEnd = fn + dfn*length;

			// průběh posouvající síly
			float fv = f.dotProduct(dirV);
			float dfv = df.dotProduct(dirV);

			current.vStart = fv;
			current.vEnd = fv + dfv*length;


			// zintegrujeme
			current.m = endMoment + current.m;
			current.dm = fv;
			current.ddm = dfv/2;

			// moment na konci, výchozí moment pro další segment
			current.mEnd = current.ddm*length*length + current.dm*length + current.m;

			// zkusíme najít extrém
			if (current.ddm != 0) {
				float exS = -0.5f*current.dm/current.ddm; // parametr s, kde je vrchol
				if (exS > 0 && exS < length) {
					current.mExtreme = current.ddm*exS*exS + current.dm*exS + current.m;
					current.msExtreme = exS;
				}
				// pokud exS neleží uprostřed nosníků, zůstává NaN
			}

			endMoment = current.mEnd; // bude sloužit jako počáteční podmínka pro další úsek
		}
	}


	/** Vnitřní bod nosníku. */
	public static class InnerPoint extends VectorXZ {

		/** Kloub působící v daném vnitřním bodě. */
		public Joint joint = null;

		/** Síla v daném vnitřním bodě. */
		public Force force = null;

		/** Moment v daném vnitřním bodě. */
		public Moment moment = null;

		/** Podpora v daném vnitřním bodě. */
		public Support support = null;

		/** Začátek / konec zatížení v daném vnitřním bodě. */
		public Load load = null;

		/** Bod leží v těžišti nějakého spojitého zatížení - užitečné pro vykreslování. */
		public boolean centerOfLoad = false;

		/** Vytvoří vnitřní bod podle zadaného bodu. */
		public InnerPoint(VectorXZ point) {
			super(point);
		}

	}

}
