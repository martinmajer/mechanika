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

import jama.Matrix;
import java.io.Serializable;
import java.util.*;

/**
 * Model.
 * @author Martin
 *
 * @todo Ošetřit vícenásobné zapojení zalomeného nosníku do jednoho kloubu.
 * @todo Vyřešit uzavřené nosníky (odstranit?).
 */
public class Model implements Serializable {

	static final long serialVersionUID = 4001L;


	/** 
	 * Měřítko modelu, udává kolik pixelů na obrazovce odovídá jednomu
	 * metru, zároveň vyjadřuje přesnost, s jakou se počítá. */
	public int scale = 50;

	/** Měřítko vnitřních sil modelu (slouží pouze k zobrazování). */
	public int innerForcesScale = 100;


	/** Seznam všech nosníků. */
	public List <Beam> beams = new LinkedList <Beam>();

	/** Počítadlo nosníků, podle kterého jsou pojmenovány nové nosníky. */
	public int beamsCounter = 0;

	/** Seznam sil. */
	public List <Force.Action> forces = new LinkedList <Force.Action>();

	/** Seznam momentů. */
	public List <Moment.Action> moments = new LinkedList <Moment.Action>();

	/** Seznam spojitých zatížení. */
	public List <Load> loads = new LinkedList <Load>();

	/** Počítadlo sil. */
	public int forcesCounter = 0;

	/** Seznam podpor. */
	public List <Support> supports = new LinkedList <Support>();


	/** Množina všech kloubů (do některých však může být připojen jen jeden styčník). */
	public transient Map <VectorXZ, Joint> joints;

	/** Body, ve kterých jsou pevné klouby. */
	public Set <VectorXZ> stiffJoints = new HashSet <VectorXZ>();

	/** Jména zavedených reakcí. */
	public transient List <String> reactionNames;

	private transient int reactionsCounter = 0;

	public transient List <Reaction> allReactions;


	public transient String reactionsString = "[prázdný model]";

	public transient float maxForce = 0;
	public transient float maxMoment = 0;


	public transient boolean determinate = false;


	public Model() {
		reset();
	}

	/**
	 * Metoda pro zajištění kompatibility programu se staršími verzemi souborů.
	 * Nastavuje hodnoty proměnným, které nejsou ve starších serializovaných
	 * objektech.
	 */
	public void afterLoad() {
		if (innerForcesScale == 0) innerForcesScale = 100;
	}


	/** Přidá nosník do modelu. */
	public void addBeam(Beam beam) {
		beams.add(beam);
		beam.name = Integer.toString(++beamsCounter);
		recalculate();
	}

	/** Odstraní nosník z modelu. */
	public void removeBeam(Beam beam) {
		beams.remove(beam);
		recalculate();
	}

	/** Přidá sílu do modelu. */
	public void addForce(Force.Action force) {
		forces.add(force);
		force.name = "F" + Integer.toString(++forcesCounter);
		recalculate();
	}

	/** Odstraní sílu z modelu. */
	public void removeForce(Force.Action force) {
		forces.remove(force);
		recalculate();
	}

	/** Přidá moment do modelu. */
	public void addMoment(Moment.Action moment) {
		moments.add(moment);
		moment.name = "M" + Integer.toString(++forcesCounter);
		recalculate();
	}

	/** Odstraní moment z modelu. */
	public void removeMoment(Moment.Action moment) {
		moments.remove(moment);
		recalculate();
	}

	/** Přidá spojité zatížení do modelu. */
	public void addLoad(Load load) {
		loads.add(load);
		load.name = "f" + Integer.toString(++forcesCounter);
		recalculate();
	}

	/** Odstraní spojité zatížení z modelu. */
	public void removeLoad(Load load) {
		loads.remove(load);
		recalculate();
	}

	/** Přidá podporu do modelu. */
	public void addSupport(Support support) {
		supports.add(support);
		recalculate();
	}

	/** Odstraní podporu z modelu. */
	public void removeSupport(Support support) {
		supports.remove(support);
		recalculate();
	}

	/** Resetuje model. */
	private void reset() {
		reactionNames = new ArrayList <String>();
		reactionsCounter = 0;
		allReactions  = new LinkedList <Reaction>();
		reactionsString = null;
		maxForce = 0;
		maxMoment = 0;
		determinate = false;
		joints = new HashMap <VectorXZ, Joint>();
		for (Beam beam: beams) {
			beam.reset();
		}
		for (Force.Action force: forces) {
			force.reset();
		}
		for (Moment.Action moment: moments) {
			moment.reset();
		}
		for (Load load: loads) {
			load.reset();
		}
		for (Support support: supports) {
			support.reset();
		}
	}

	/** Vytvoří jméno pro novou reakci. */
	private String getReactionName() {
		String name = null;
		do {
			name = "R" + ++reactionsCounter;
			if (reactionNames.contains(name)) {
				name = null;
			}
		} while (name == null);
		return name;
	}

	/** Vrátí jméno pro reakci, nebo vytvoří nové, ještě neobsazené. */
	private String getReactionName(String attempt) {
		if (reactionNames.contains(attempt)) return getReactionName();
		else return attempt;
	}

	/** Přepočítá model. */
	public void recalculate() {
		reset();

		// seřadíme nosníky podle jména, ať vznikají stejné reakce
		// při různém pořadí přidání do modelu
		Collections.sort(beams, new Comparator<Beam>() {
			public int compare(Beam b1, Beam b2) {
				return b1.name.compareTo(b2.name);
			}
		});

		// Přidáme do modelu nosníky a vytvoříme klouby
		for (Beam beam: beams) {
			bindBeam(beam);
		}

		// Přidáme do modelu síly
		for (Force.Action force: forces) {
			bindForce(force);
		}

		// Přidáme momenty
		for (Moment.Action moment: moments) {
			bindMoment(moment);
		}

		// Přidáme spojité zatížení
		for (Load load: loads) {
			bindLoad(load);
		}

		// Přidáme podpory
		for (Support support: supports) {
			bindSupport(support);
		}

		Iterator <VectorXZ> stiffIt = stiffJoints.iterator();
		while (stiffIt.hasNext()) {
			VectorXZ p = stiffIt.next();
			if (!joints.containsKey(p)) stiffIt.remove();
		}

		// Nejdřív se podíváme, jestli nemůžeme některé nosníky převést na táhla,
		// z takových nosníku vytvoříme reakci ve směru normálové síly
		for (Beam beam: beams) {
			if (beam.canBeRod(this)) {
				VectorXZ a = beam.mainPoints.get(0);
				VectorXZ b = beam.mainPoints.get(1);
				Joint jointA = joints.get(a);
				Joint jointB = joints.get(b);

				// pokud je v jednom z kloubů vetknutí / moment, nebudeme vytvářet táhlo
				// (mohlo by se stát, že v takovém kloubu budou samá táhla a bude
				// na něj nahlíženo jako na hmotný bod)
				if (!jointA.stiff && !jointB.stiff) {
					beam.isRod = true;

					// zavedeme reakce - síly směřují dovnitř táhla, tzn při kladné reakci je táhlo
					// skutečně v tahu (ne v tlaku)
					VectorXZ dirA = new VectorXZ(b.x-a.x, b.z-a.z).normalize();
					VectorXZ dirB = new VectorXZ(-dirA.x, -dirA.z);

					String name = getReactionName("R" + beam.name);

					Force.Reaction forceA = new Force.Reaction();
					forceA.origin = a.clone();
					forceA.direction = dirA;
					forceA.setName(name);

					Force.Reaction forceB = new Force.Reaction();
					forceB.origin = b.clone();
					forceB.direction = dirB;
					forceB.setName(name);

					jointA.forces.add(forceA);
					jointB.forces.add(forceB);

					reactionNames.add(name);
					beam.rodReactionName = name;
					beam.rodReaction = forceA;
					allReactions.add(forceA);
					allReactions.add(forceB);
				}
			}
		}

		// Nahradíme všechny podpory silami resp. momenty
		// a) na kloubech
		for (VectorXZ p: joints.keySet()) {
			Joint joint = joints.get(p);
			for (Support support: joint.supports) {
				for (Reaction reaction: support.getReactions()) {
					String name = getReactionName();
					reaction.setName(name);
					reactionNames.add(name);
					allReactions.add(reaction);
					if (reaction instanceof Force) joint.forces.add((Force)reaction);
					else joint.moments.add((Moment)reaction);
				}
			}
		}
		// b) uprostřed nosníků
		for (Beam beam: beams) {
			List <Beam.InnerPoint> pointsWithSupport = new LinkedList <Beam.InnerPoint>();
			for (VectorXZ point: beam.allPoints) {
				if (point instanceof Beam.InnerPoint && ((Beam.InnerPoint)point).support != null) {
					pointsWithSupport.add((Beam.InnerPoint)point);
				}
			}
			for (Beam.InnerPoint toReplace: pointsWithSupport) {
				List <Beam.InnerPoint> newPoints = new LinkedList <Beam.InnerPoint>();
				for (Reaction reaction: toReplace.support.getReactions()) {
					String name = getReactionName();
					reaction.setName(name);
					reactionNames.add(name);
					allReactions.add(reaction);
					Beam.InnerPoint newPoint = new Beam.InnerPoint(toReplace);
					if (reaction instanceof Force) newPoint.force = (Force)reaction;
					else newPoint.moment = (Moment)reaction;
					newPoints.add(newPoint);
				}
				beam.replacePoint(toReplace, newPoints);
			}
		}

		// Nahradíme klouby silami
		for (VectorXZ p: joints.keySet()) {
			Joint joint = joints.get(p);

			Beam current = null, prev = null;

			Collections.sort(joint.beams, new Comparator<Beam>() {
				public int compare(Beam b1, Beam b2) {
					return b1.name.compareTo(b2.name);
				}
			});

			for (Beam beam: joint.beams) {
				if (beam.isRod) continue; // táhla přeskočíme
				joint.allRods = false;
				prev = current;
				current = beam;
				if (prev != null) {
					// spojíme nosníky dvojicí sil
					Force.Reaction xPlus = new Force.Reaction();
					xPlus.setName(getReactionName());
					xPlus.direction = new VectorXZ(1, 0);
					xPlus.origin = joint.position;

					Force.Reaction xMinus = new Force.Reaction();
					xMinus.setName(xPlus.getName());
					xMinus.direction = new VectorXZ(-1, 0);
					xMinus.origin = joint.position;

					Moment.Reaction mPlus = null;
					if (joint.stiff) {
						mPlus = new Moment.Reaction();
						mPlus.setName(getReactionName());
						mPlus.origin = joint.position;
					}

					Force.Reaction zPlus = new Force.Reaction();
					zPlus.setName(getReactionName());
					zPlus.direction = new VectorXZ(0, 1);
					zPlus.origin = joint.position;

					Force.Reaction zMinus = new Force.Reaction();
					zMinus.setName(zPlus.getName());
					zMinus.direction = new VectorXZ(0, -1);
					zMinus.origin = joint.position;

					Moment.Reaction mMinus = null;
					if (joint.stiff) {
						mMinus = new Moment.Reaction();
						mMinus.opposite = true;
						mMinus.setName(mPlus.getName());
						mMinus.origin = joint.position;
					}

					reactionNames.add(xPlus.name);
					reactionNames.add(zPlus.name);
					if (joint.stiff) reactionNames.add(mPlus.name);

					allReactions.add(xPlus);
					allReactions.add(xMinus);
					allReactions.add(zPlus);
					allReactions.add(zMinus);
					if (joint.stiff) {
						allReactions.add(mPlus);
						allReactions.add(mMinus);
					}

					Beam.InnerPoint xPlusIp = new Beam.InnerPoint(joint.position);
					xPlusIp.force = xPlus;
					Beam.InnerPoint zPlusIp = new Beam.InnerPoint(joint.position);
					zPlusIp.force = zPlus;

					Beam.InnerPoint xMinusIp = new Beam.InnerPoint(joint.position);
					xMinusIp.force = xMinus;
					Beam.InnerPoint zMinusIp = new Beam.InnerPoint(joint.position);
					zMinusIp.force = zMinus;

					prev.insertInnerPoint(joint.position, xPlusIp);
					prev.insertInnerPoint(joint.position, zPlusIp);
					current.insertInnerPoint(joint.position, xMinusIp);
					current.insertInnerPoint(joint.position, zMinusIp);

					joint.reactions.add(xPlus);
					joint.reactions.add(zPlus);

					if (joint.stiff) {
						Beam.InnerPoint mPlusIp = new Beam.InnerPoint(joint.position);
						mPlusIp.moment = mPlus;

						Beam.InnerPoint mMinusIp = new Beam.InnerPoint(joint.position);
						mMinusIp.moment = mMinus;

						prev.insertInnerPoint(joint.position, mPlusIp);
						current.insertInnerPoint(joint.position, mMinusIp);

						joint.reactions.add(mPlus);
					}
				}
			}


			// všechny síly působící na kloub necháme působit na první nosník
			// (pokud není kloub hmotný bod)
			if (!joint.allRods) {
				Beam firstBeam = null;
				for (Beam beam: joint.beams) {
					if (!beam.isRod) {
						firstBeam = beam;
						break;
					}
				}
				for (Force force: joint.forces) {
					Beam.InnerPoint ip = new Beam.InnerPoint(joint.position);
					ip.force = force;
					firstBeam.insertInnerPoint(joint.position, ip);
				}
				for (Moment moment: joint.moments) {
					Beam.InnerPoint ip = new Beam.InnerPoint(joint.position);
					ip.moment = moment;
					firstBeam.insertInnerPoint(joint.position, ip);
				}
			}
		}

		// Vytvoříme mapu jméno reakce -> index sloupce v matici
		Map <String, Integer> nameMap = new HashMap <String, Integer>();
		int reactions = 0;
		for (String name: reactionNames) {
			nameMap.put(name, reactions++);
		}

		// Sestavíme matici
		ArrayList <double[]> leftRows = new ArrayList<double[]>();
		ArrayList <Double> rightRows = new ArrayList<Double>();

		// Projdeme klouby (dva řádky za každý kloub)
		for (VectorXZ p: joints.keySet()) {
			Joint joint = joints.get(p);
			if (joint.allRods) {
				double[] xEq = new double[reactions];
				double[] zEq = new double[reactions];
				double xSum = 0, zSum = 0;

				for (Force f: joint.forces) {
					if (f instanceof Force.Action) {
						xSum -= f.size*f.direction.x;
						zSum -= f.size*f.direction.z;
					}
					else if (f instanceof Force.Reaction) {
						xEq[nameMap.get(f.name)] += f.direction.x;
						zEq[nameMap.get(f.name)] += f.direction.z;
					}
				}

				leftRows.add(xEq);
				leftRows.add(zEq);
				rightRows.add(xSum);
				rightRows.add(zSum);
			}
		}

		// Projdeme nosníky (tři řádky za každý nosník)
		for (Beam beam: beams) {
			if (!beam.isRod) {
				double[] xEq = new double[reactions];
				double[] zEq = new double[reactions];
				double[] mEq = new double[reactions];
				double xSum = 0, zSum = 0, mSum = 0;

				// osamělé síly / momenty
				for (VectorXZ p: beam.allPoints) {
					if (p instanceof Beam.InnerPoint) {
						Beam.InnerPoint ip = (Beam.InnerPoint)p;
						if (ip.force != null && ip.force instanceof Force.Action) {
							Force f = ip.force;
							xSum -= f.size*f.direction.x;
							zSum -= f.size*f.direction.z;
							mSum -= f.size*(f.direction.x*f.origin.z - f.direction.z*f.origin.x);
						}
						else if (ip.force != null && ip.force instanceof Force.Reaction) {
							Force f = ip.force;
							xEq[nameMap.get(f.name)] += f.direction.x;
							zEq[nameMap.get(f.name)] += f.direction.z;
							mEq[nameMap.get(f.name)] += (f.direction.x*f.origin.z - f.direction.z*f.origin.x);
						}
						else if (ip.moment != null && ip.moment instanceof Moment.Action) {
							mSum -= ip.moment.size;
						}
						else if (ip.moment != null && ip.moment instanceof Moment.Reaction) {
							mEq[nameMap.get(ip.moment.name)] += ((Moment.Reaction)ip.moment).opposite ? -1 : 1;
						}
					}
				}

				// spojitá zatížení
				for (Load load: beam.loads) {
					float size = load.getSize();
					VectorXZ center = load.getCenter();
					VectorXZ direction = load.direction;
					xSum -= size*direction.x;
					zSum -= size*direction.z;
					mSum -= size*(direction.x*center.z - direction.z*center.x);
				}

				leftRows.add(xEq);
				leftRows.add(zEq);
				leftRows.add(mEq);
				rightRows.add(xSum);
				rightRows.add(zSum);
				rightRows.add(mSum);
			}
		}

		double[][] leftRowsArray = new double[leftRows.size()][reactions];
		double[][] rightRowsArray = new double[leftRowsArray.length][1];
		//rightRowsArray[0] = new double[leftRowsArray.length];

		for (int row = 0; row < leftRows.size(); row++) {
			leftRowsArray[row] = new double[reactions];
			for (int col = 0; col < reactions; col++) {
				leftRowsArray[row][col] = leftRows.get(row)[col];
			}
			rightRowsArray[row] = new double[] {rightRows.get(row)};
		}

		if (leftRowsArray.length > 0) {
			Matrix equationsMatrix = new Matrix(leftRowsArray);
			Matrix sumMatrix = new Matrix(rightRowsArray);

			//System.out.printf(Locale.ENGLISH, ("Řádky: %d, neznámé: %d\n", leftRowsArray.length, reactions);

			if (reactions > leftRowsArray.length) {
				reactionsString = (reactions - leftRowsArray.length) + "x staticky neurčitá.";
			}
			else if (reactions < leftRowsArray.length) {
				reactionsString = (leftRowsArray.length - reactions) + "x staticky přeurčitá.";
			}
			else {
				try {
					double det = equationsMatrix.det();
					System.out.println(det);
					if (Math.abs(det) < 0.0000001) throw new Exception();

					//LUDecomposition lud = new LUDecomposition(equationsMatrix);
					Matrix solutionMatrix = equationsMatrix.solve(sumMatrix);
					double[][] solution = solutionMatrix.getArray();


					for (Reaction reaction: allReactions) {
						float size = (float)solution[nameMap.get(reaction.getName())][0];
						reaction.setSize(size);

						// zapamatujeme si největší sílu
						if (reaction instanceof Force.Reaction) {
							if (Math.abs(size) > maxForce) maxForce = Math.abs(size);
						}
					}

					Collections.sort(reactionNames, new Comparator<String>() {
						public int compare(String o1, String o2) {
							try {
								String s1 = o1.substring(1);
								String s2 = o2.substring(1);
								if (s1.length() > s2.length()) return 1;
								else if (s1.length() < s2.length()) return -1;
								else return s1.compareTo(s2);
							}
							catch (IndexOutOfBoundsException e) {
								return o1.compareTo(o2);
							}
						}
					});

					StringBuilder sb = new StringBuilder();

					for (String reactionName: reactionNames) {
						sb.append(String.format(Locale.ENGLISH, "%3s = % 9.3f\n", reactionName, solution[nameMap.get(reactionName)][0]));
					}

					reactionsString = sb.toString();
					
					determinate = true;
				}
				catch (Exception e) {
					reactionsString = "Výjimkový případ.";
				}
			}
		}
		else {
			reactionsString = "[prázdný model]";
		}
	}


	private static final float EPSILON = 0.03f;

	/** Rozhodne, zda bod C leží na úsečce AB. */
	private boolean isPointOnLine(VectorXZ a, VectorXZ b, VectorXZ c) {
		// vertikální čára
		if (a.x == b.x) {
			if (c.x == a.x && ((a.z <= c.z && c.z <= b.z) || (a.z >= c.z && c.z >= b.z))) return true;
			else return false;
		}
		// horizontální čára
		else if (a.z == b.z) {
			if (c.z == a.z && ((a.x <= c.x && c.x <= b.x) || (a.x >= c.x && c.x >= b.x))) return true;
			else return false;
		}
		// ostatní případy... nehrozí žádné dělení nulou
		else {
			float alpha1 = (c.x - a.x) / (b.x - a.x);
			float alpha2 = (c.z - a.z) / (b.z - a.z);

			// tolerance závislá na délce čáry
			float lengthFactor = (float)Math.sqrt((b.x-a.x)*(b.x-a.x)+(b.z-a.z)*(b.z-a.z))/200*scale;

			if (Math.abs(alpha1 - alpha2) < EPSILON / lengthFactor && (alpha1 >= 0 && alpha1 <= 1)) return true;
			else return false;
		}
	}


	/**
	 * Rozhodne, zda bod C leží na přímce AB.
	 * @return parametr k pro C = A + k(B-A), nebo NaN, pokud C na příme neleží.
	 */
	private float isPointOnLine2(VectorXZ a, VectorXZ b, VectorXZ c) {
		if (a.x == b.x) {
			if (c.x == a.x) {
				return (c.z - a.z) / (b.z - a.z);
			}
			else return Float.NaN;
		}
		else if (a.z == b.z) {
			if (c.z == a.z) {
				return (c.x - a.x) / (b.x - a.x);
			}
			else return Float.NaN;
		}
		else {
			float alpha1 = (c.x - a.x) / (b.x - a.x);
			float alpha2 = (c.z - a.z) / (b.z - a.z);

			// tolerance závislá na délce čáry
			float lengthFactor = (float)Math.sqrt((b.x-a.x)*(b.x-a.x)+(b.z-a.z)*(b.z-a.z))/200*scale;

			if (Math.abs(alpha1 - alpha2) < EPSILON / lengthFactor) {
				return (alpha1 + alpha2) / 2;
			}
			else return Float.NaN;
		}
	}


	/**
	 * Zjistí, zda zadaný bod leží na zadaném nosníku a pokud ano, připojí nosník ke
	 * kloubu v daném bodě, nebo na něj naváže zadaný objekt.
	 */
	private void testPointOnBeam(Beam testedBeam, VectorXZ testedPoint, Object addObject) {
		// projdeme všechny segmenty testovaného nosníku
		for (int i = 0; i < testedBeam.mainPoints.size(); i++) {
			// testujeme současný a předchozí bod
			VectorXZ lineA = null, lineB = null;
			if (i == 0) {
				if (testedBeam.closed) {
					lineA = testedBeam.mainPoints.get(testedBeam.mainPoints.size()-1);
					lineB = testedBeam.mainPoints.get(i);
				}
			}
			else {
				lineA = testedBeam.mainPoints.get(i-1);
				lineB = testedBeam.mainPoints.get(i);
			}

			if (lineA != null && lineB != null) {
				if (lineA.equals(testedPoint) || lineB.equals(testedPoint)) continue;
				if (isPointOnLine(lineA, lineB, testedPoint) == true) {
					if (addObject == null) {
						Joint joint = joints.get(testedPoint);
						if (!joint.beams.contains(testedBeam)) {
							joint.beams.add(testedBeam);
							Beam.InnerPoint innerPoint = new Beam.InnerPoint(testedPoint);
							innerPoint.joint = joint;
							testedBeam.insertInnerPoint(lineA, innerPoint);
							testedBeam.insertInnerJoint(lineA, innerPoint);
						}
					}
					else {
						Beam.InnerPoint innerPoint = new Beam.InnerPoint(testedPoint);
						if (addObject instanceof Force.Action) {
							innerPoint.force = (Force.Action)addObject;
							((Force.Action)addObject).enabled = true;
						}
						else if (addObject instanceof Moment.Action) {
							innerPoint.moment = (Moment.Action)addObject;
							((Moment.Action)addObject).enabled = true;
						}
						else if (addObject instanceof Support) {
							innerPoint.support = (Support)addObject;
							innerPoint.support.enabled = true;
						}
						testedBeam.insertInnerPoint(lineA, innerPoint);
					}
					return;
				}
			}
		}
	}

	/** Přidá do modelu nový nosník a připojí ho do kloubů. */
	private void bindBeam(Beam beam) {
		// Můžeme narazit na následující situace při spojování nosníků:
		// 1) Nosník se jedním ze svých konců/zlomů dotýká konce/zlomu jiného nosníku
		//		-> z bodu vytvoříme kloub
		// 2) Nosník se jedním ze svých konců/zlomů dotýká jiného nosníku, ne na konci/zlomu
		//		-> na druhý nosník vložíme bod navíc
		// 3) Jiný nosník se jedním ze svých konců/zlomů dotýká tohoto nosníku, ne na konci/zlomu
		//		-> dtto
		// Pokud se nosníky kříží, žádný spoj nebude vytvořen.

		// připojíme nosník do kloubů v hlavních bodech
		for (VectorXZ point: beam.mainPoints) {
			// podíváme se, jestli je na zadané pozici nějaký kloub
			Joint joint = joints.get(point);
			if (joint == null) { // kloub neexistuje, vytvoříme ho
				joint = new Joint(point);
				if (stiffJoints.contains(point)) joint.stiff = true;
				joints.put(point.clone(), joint);
			}

			// přidáme do kloubu nosník
			joint.beams.add(beam);
		}

		// otestujeme všechny nosníky pro všechny body přidávaného nosníku
		for (Beam testedBeam: beams) {
			if (testedBeam.mainPoints.size() < 2) continue;
			if (testedBeam == beam) continue;
			for (VectorXZ testedPoint: beam.mainPoints) {
				// pokud je na místě bodu kloub s alespoň dvěma nosníky,
				// testovaný nosník už musel být připojen
				// Joint j = joints.get(testedPoint);
				testPointOnBeam(testedBeam, testedPoint, null);
			}
		}

		// otestujeme nosník na všechny existující klouby
		outerLoop: for (VectorXZ testedPoint: joints.keySet()) {
			// nejdřív se ujistíme, že kloub není součástí nosníku
			for (VectorXZ beamPoint: beam.mainPoints) {
				if (testedPoint.equals(beamPoint)) continue outerLoop;
			}
			testPointOnBeam(beam, testedPoint, null);
		}

	}

	/** Připojí do modelu zadanou sílu. */
	private void bindForce(Force.Action force) {
		// Mohou nastav tyto situace:
		// 1) Síla bude působit v místě kloubu
		//		-> navážeme sílu na kloub
		// 2) Síla bude působit uprostřed nosníku
		//		-> navážeme sílu na nosník

		Joint joint = joints.get(force.origin);
		if (joint != null) {
			joint.forces.add(force);
			force.enabled = true;
		}
		else {
			// projdeme všechny nosníky a zjistíme, jestli síla neleží
			// na některém z nich
			for (Beam beam: beams) {
				testPointOnBeam(beam, force.origin, force);
			}
		}
	}

	/** Připojí do modelu zadaný moment. */
	private void bindMoment(Moment.Action moment) {
		Joint joint = joints.get(moment.origin);
		if (joint != null) {
			joint.moments.add(moment);
			joint.stiff = true; // pevný kloub!
			moment.enabled = true;
		}
		else {
			for (Beam beam: beams) {
				testPointOnBeam(beam, moment.origin, moment);
			}
		}
	}

	/** Připojí do modelu zadanou podporu. */
	private void bindSupport(Support support) {
		// viz kloub
		Joint joint = joints.get(support.origin);
		if (joint != null) {
			joint.supports.add(support);
			if (support instanceof Support.Fixed) joint.stiff = true; // pevný kloub!
			support.enabled = true;
		}
		else {
			for (Beam beam: beams) {
				testPointOnBeam(beam, support.origin, support);
			}
		}
	}


	/** Struktura pro kloub na přímce. */
	private class JointOnLine {
		public float param;
		public Joint joint;
		public JointOnLine(Joint joint, float param) {
			this.joint = joint; this.param = param;
		}
	}

	/** Vrátí všechny klouby, které leží na zadané čáře. */
	private List <JointOnLine> getAllJointsOnLine(VectorXZ a, VectorXZ b) {
		List <JointOnLine> list = new ArrayList <JointOnLine>();
		for (VectorXZ point: joints.keySet()) {
			float param = isPointOnLine2(a, b, point);
			if (!Float.isNaN(param)) {
				list.add(new JointOnLine(joints.get(point), param));
			}
		}
		Collections.sort(list, new Comparator<JointOnLine>() {
			public int compare(JointOnLine o1, JointOnLine o2) {
				if (o1.param > o2.param) return 1;
				else if (o1.param < o2.param) return -1;
				else return 0;
			}
		});
		return list;
	}


	/** Připojí do modelu spojité zatížení. */
	private void bindLoad(Load load) {
		List <JointOnLine> jointsOnLine = getAllJointsOnLine(load.start, load.end);
		
		JointOnLine prev = null, current = null;
		for (JointOnLine jol: jointsOnLine) {
			prev = current;
			current = jol;
			if (prev == null) continue;

			// pokud jsou oba body před začátkem zatížení, přeskočíme je
			if (prev.param <= 0 && current.param <= 0) continue;
			// pokud jsou oba body za začátkem zatížení, opustíme smyčku
			if (prev.param >= 1 && current.param >= 1) break;

			// pokud je alespoň jeden bod v <0; 1>, je možné, že síla působí
			// na nosník nebo jeho část - ověříme, zda body leží na stejném nosníku
			for (Beam beam: prev.joint.beams) {
				int partOfBeam = beam.isPartOfBeam(prev.joint.position, current.joint.position);
				if (partOfBeam != 0) {
					VectorXZ start = prev.joint.position;
					VectorXZ end = current.joint.position;

					// zatížení nemusí působit na celém úseku nosníku
					if (prev.param < 0) start = load.start;
					if (current.param > 1) end = load.end;

					Load partOfLoad = new Load();
					partOfLoad.name = load.name;
					partOfLoad.start = start;
					partOfLoad.end = end;
					partOfLoad.direction = load.direction;
					partOfLoad.sizePerPixel = load.sizePerPixel;

					// přidáme patřičnou část zatížení na nosník
					beam.loads.add(partOfLoad);
					Beam.InnerPoint ipStart = new Beam.InnerPoint(start);
					ipStart.load = partOfLoad;
					Beam.InnerPoint ipEnd = new Beam.InnerPoint(end);
					ipEnd.load = partOfLoad;
					Beam.InnerPoint ipCenter = new Beam.InnerPoint(new VectorXZ((start.x+end.x)/2, (start.z+end.z)/2));
					ipCenter.centerOfLoad = true;

					beam.insertInnerPoint(partOfBeam == 1 ? prev.joint.position : current.joint.position, ipEnd);
					//beam.insertInnerPoint(partOfBeam == 1 ? prev.joint.position : current.joint.position, ipCenter);
					beam.insertInnerPoint(partOfBeam == 1 ? prev.joint.position : current.joint.position, ipStart);

					// přidáme aktuální interval (případně rozšíříme předchozí)
					float lastActivePartEnd = load.activeParts.size() > 0 ? load.activeParts.get(load.activeParts.size()-1) : Float.NaN;
					if (prev.param == lastActivePartEnd) {
						load.activeParts.set(load.activeParts.size()-1, current.param);
					}
					else {
						load.activeParts.add(prev.param);
						load.activeParts.add(current.param);
					}
				}
			}
		}
	}

	public String getModelInfo() {
		StringBuilder sb = new StringBuilder(10000);

		// počet kloubů
		int nJoints = 0;
		int nBeamsInJoints = 0;
		for (VectorXZ p: joints.keySet()) {
			Joint j = joints.get(p);
			if (j.beams.size() > 1) {
				nJoints++;
				nBeamsInJoints += j.beams.size();
			}
		}


		sb.append("INFORMACE O MODELU\n");
		sb.append("------------------\n\n");
		sb.append("Počet nosníků:      "); sb.append(beams.size()); sb.append("\n");
		sb.append("Počet kloubů:       "); sb.append(nJoints); sb.append("\n");
		sb.append("Stupně kloubů:      "); sb.append(nBeamsInJoints); sb.append("\n");
		sb.append("Počet sil:          "); sb.append(forces.size()); sb.append("\n");
		sb.append("Počet momentů:      "); sb.append(forces.size()); sb.append("\n");
		sb.append("Počet sp. zatížení: "); sb.append(loads.size()); sb.append("\n");
		sb.append("Počet podpor:       "); sb.append(supports.size()); sb.append("\n");

		sb.append("\n");
		sb.append("\n");

		sb.append(reactionsString);

		return sb.toString();
	}

	/** Vrátí nejzazší souřadnice kloubů v pořadí levá, horní, pravá, dolní. */
	public float[] getModelBounds() {
		float xMin = 0, xMax = 0;
		float zMin = 0, zMax = 0;

		for (VectorXZ point: joints.keySet()) {
			if (point.x > xMax) xMax = point.x;
			if (point.x < xMin) xMin = point.x;
			if (point.z > zMax) zMax = point.z;
			if (point.z < zMin) zMin = point.z;
		}

		return new float[] { xMin, zMin, xMax, zMax };
	}


	public static Model getTestModel() {
		Model model = new Model();
		Beam a = new Beam(0, 0, 100, 0, 200, -100);
		Beam b = new Beam(0, 0, 0, 100);
		Beam c = new Beam(100, 0, 100, 100, 0, 100);
		Beam d = new Beam(150, -50, 50, -100, 50, 0);
		model.addBeam(a);
		model.addBeam(b);
		model.addBeam(c);
		model.addBeam(d);
		
		//model.removeBeam(b);
		return model;
	}


}
