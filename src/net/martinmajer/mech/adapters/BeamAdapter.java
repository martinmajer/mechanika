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

package net.martinmajer.mech.adapters;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import net.martinmajer.mech.MechCanvas.ViewMode;
import net.martinmajer.mech.ModelEntityAdapter;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class BeamAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Beam beam = (Beam)e;
		Model model = canvas.model;

		if (beam.mainPoints.size() < 2) return;
		if (active) g.setColor(CL_ACTIVE_BEAM);
		else {
			if (beam.isRod && model.maxForce != 0) {
				float red = 0, blue = 0;
				float force = beam.rodReaction.size;
				if (force > 0) {
					blue = force / model.maxForce;
				}
				else if (force < 0) {
					red = -force / model.maxForce;
				}
				g.setColor(new Color(red, 0, blue));
			}
			else {
				g.setColor(CL_BEAM);
			}
		}

		for (int i = 0; i < beam.mainPoints.size(); i++) {
			float currentX = beam.mainPoints.get(i).x;
			float currentZ = beam.mainPoints.get(i).z;

			if (i > 0) { // pokud jsme minimálně u druhého bodu, nakreslíme čáru
				if (beam.isRod) g.setStroke(ST_ROD);
				else g.setStroke(ST_BEAM);
				g.drawLine(canvas.m2cx(beam.mainPoints.get(i-1).x), canvas.m2cz(beam.mainPoints.get(i-1).z), canvas.m2cx(currentX), canvas.m2cz(currentZ));
				g.setStroke(ST_DEFAULT);
			}

			// podíváme se, jestli není v bodu kloub, který bychom mohli vykreslit
			Joint j = model.joints.get(beam.mainPoints.get(i));
			if (j != null && j.beams.size() > 1 && !j.allRods && !j.stiff) { // kloub!
				float dirX = 0, dirZ = 0; // směr, ve kterém posuneme graficku
										  // značku kloubu od jeho skutečné pozice

				float offset = 5f / canvas.model.scale;

				// 1) Koncový kloub v počátečním bodě
				if (i == 0 && !beam.closed) {
					float nextX = beam.mainPoints.get(1).x;
					float nextZ = beam.mainPoints.get(1).z;

					// vektor je ve směru od počátečního bodu do následujícího
					dirX = nextX - currentX;
					dirZ = nextZ - currentZ;
				}
				// 2) Koncový kloub v koncovém bodě
				else if (i == beam.mainPoints.size()-1 && !beam.closed) {
					float prevX = beam.mainPoints.get(i-1).x;
					float prevZ = beam.mainPoints.get(i-1).z;

					// vektor je ve směru od koncového bodu do předchozího
					dirX = prevX - currentX;
					dirZ = prevZ - currentZ;
				}
				// 3) Kloub ve zlomu
				else {
					float prevX, prevZ, nextX, nextZ;
					int prevIndex = i-1;
					if (i == 0) prevIndex = beam.mainPoints.size()-1;
					int nextIndex = i+1;
					if (i == beam.mainPoints.size()-1) nextIndex = 0;

					prevX = beam.mainPoints.get(prevIndex).x;
					prevZ = beam.mainPoints.get(prevIndex).z;
					nextX = beam.mainPoints.get(nextIndex).x;
					nextZ = beam.mainPoints.get(nextIndex).z;

					// vytvoříme dva vektory z aktuálního bodu do předchozího
					// a následujícího bodu, normalizujeme a sečteme
					float v1x = prevX - currentX;
					float v2x = nextX - currentX;
					float v1z = prevZ - currentZ;
					float v2z = nextZ - currentZ;

					float v1size = (float)Math.sqrt(v1x*v1x + v1z*v1z);
					v1x /= v1size; v1z /= v1size;
					float v2size = (float)Math.sqrt(v2x*v2x + v2z*v2z);
					v2x /= v2size; v2z /= v2size;

					dirX = v1x + v2x;
					dirZ = v1z + v2z;
				}

				// vytvoříme ze směru jednotkový vektor
				float dirSize = (float)Math.sqrt(dirX*dirX + dirZ*dirZ);
				dirX /= dirSize;
				dirZ /= dirSize;

				if (dirSize < 0.1) {
					dirX = 0; dirZ = 0;
				}

				// vykreslíme kloub
				canvas.drawJointCircle(g, canvas.m2cx(j.position.x + dirX*offset), canvas.m2cz(j.position.z + dirZ*offset), 9);
			}
		}
		if (beam.closed) {
			g.setStroke(ST_BEAM);
			g.drawLine(canvas.m2cx(beam.mainPoints.get(0).x), canvas.m2cz(beam.mainPoints.get(0).z), canvas.m2cx(beam.mainPoints.get(beam.mainPoints.size()-1).x), canvas.m2cz(beam.mainPoints.get(beam.mainPoints.size()-1).z));
			g.setStroke(ST_DEFAULT);
		}


		if (canvas.model.determinate) {
			if (canvas.viewMode == ViewMode.NORMAL_FORCE) {
				drawNormalForce(g, beam);
			}
			else if (canvas.viewMode == ViewMode.SHEAR_FORCE) {
				drawShearForce(g, beam);
			}
			else if (canvas.viewMode == ViewMode.BENDING_MOMENT) {
				drawBendingMoment(g, beam);
			}
		}
	}


	/**
	 * Vykreslí vnitřní síly na nosníku.
	 * @param g grafický kontext
	 * @param beam nosník
	 * @param normalOrShear true - normálové, false - posouvající
	 */
	private void drawInnerForces(Graphics2D g, Beam beam, boolean normalOrShear) {
		if (beam.internalForces == null) beam.computeInternalForces();

		Iterator <Beam.ForcesDistribution> it = beam.internalForces.listIterator();
		Beam.ForcesDistribution current = null, next = null;
		while (it.hasNext()) {
			current = next;
			next = it.next();
			if (current == null) continue;

			/*VectorXZ line = new VectorXZ(next.x - current.x, next.z - current.z);
			float length = line.size();

			// směrový vektor vykreslované části nosníku (směr normálové síly)
			VectorXZ iX = line.normalize();
			// normálový vektor vykreslované části nosníku (směr posouvající síly)
			VectorXZ iZ = new VectorXZ(-iX.z, iX.x);

			// síla v prvním bodě
			VectorXZ forceA = new VectorXZ(current.fx, current.fz);
			float fa = forceA.dotProduct(normalOrShear ? iX : iZ);

			// síla v druhém bodě
			VectorXZ forceB = new VectorXZ(current.fx + length * current.dfx, current.fz + length * current.dfz);
			float fb = forceB.dotProduct(normalOrShear ? iX : iZ);*/

			float fa = 0, fb = 0;
			if (normalOrShear == true) { // normálová síla
				fa = current.nStart;
				fb = current.nEnd;
			}
			else {
				fa = current.vStart;
				fb = current.vEnd;
			}

			float factor = 1;
			if (normalOrShear == true) factor = .2f; // normálové síly bývají hrozně velké :-)

			drawLinearDistribution(g, 0.1f*factor*canvas.model.innerForcesScale/100f, current.getStartingPoint(), next.getStartingPoint(), current.dirV, fa, fb);

			//drawLinearDistribution(g, 0.1f/**(50f/canvas.model.scale)*/*canvas.innerForcesScale/100f, current.getStartingPoint(), next.getStartingPoint(), current.dirV, fa, fb);
			//drawLinearDistribution(g, 0.01f*(50f/canvas.model.scale)*canvas.innerForcesScale/100f, current.getStartingPoint(), next.getStartingPoint(), iZ, fa, fb);
		}
	}

	/** Vynese lineární průběh sil nad úsečku AB, zadaným směrem. */
	private void drawLinearDistribution(Graphics2D g, float factor, VectorXZ a, VectorXZ b, VectorXZ normal, float fa, float fb) {
		int a1x = canvas.m2cx(a.x);
		int a1z = canvas.m2cz(a.z);
		int a2x = canvas.m2cx(a.x - fa*normal.x * factor);
		int a2z = canvas.m2cz(a.z - fa*normal.z * factor);
		int b2x = canvas.m2cx(b.x - fb*normal.x * factor);
		int b2z = canvas.m2cz(b.z - fb*normal.z * factor);
		int b1x = canvas.m2cx(b.x);
		int b1z = canvas.m2cz(b.z);

		// kladnou posouvající sílu vyneseme kolmo na nosník
		if (Math.abs(fa*factor*canvas.model.scale) < 1 && Math.abs(fb*factor*canvas.model.scale) < 1);
		// oba body na stejné straně / stačí jeden tvar
		else if((fa >= 0 && fb >= 0) || (fa <= 0 && fb <= 0)) {

			if (fa >= 0 && fb >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE);

			g.drawLine(a1x, a1z, a2x, a2z);
			g.drawLine(a2x, a2z, b2x, b2z);
			g.drawLine(b2x, b2z, b1x, b1z);

			if (fa >= 0 && fb >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE_FILL);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE_FILL);

			int[] xpoints = new int[] {a1x, a2x, b2x, b1x};
			int[] zpoints = new int[] {a1z, a2z, b2z, b1z};

			g.fillPolygon(xpoints, zpoints, 4);
		}
		// body na různé straně nosníku
		else {
			// průsečík čar
			Line2D.Float line1 = new Line2D.Float(a1x, a1z, b1x, b1z);
			Line2D.Float line2 = new Line2D.Float(a2x, a2z, b2x, b2z);
			Point2D.Float intersection = getIntersection(line1, line2);

			if (Float.isNaN(intersection.x)) {
				intersection.x = (line2.x1 + line2.x2) / 2;
			}
			if (Float.isNaN(intersection.y)) {
				intersection.y = (line2.y1 + line2.y2) / 2;
			}

			int sx = (int)Math.round(intersection.x);
			int sz = (int)Math.round(intersection.y);

			// první část
			if (fa >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE);

			g.drawLine(a1x, a1z, a2x, a2z);
			g.drawLine(a2x, a2z, sx, sz);

			if (fa >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE_FILL);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE_FILL);

			int[] xpointsA = new int[] {a1x, a2x, sx};
			int[] zpointsA = new int[] {a1z, a2z, sz};

			g.fillPolygon(xpointsA, zpointsA, 3);

			// druhá část
			if (fb >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE);

			g.drawLine(b1x, b1z, b2x, b2z);
			g.drawLine(b2x, b2z, sx, sz);

			if (fb >= 0) g.setColor(CL_INTERNAL_FORCE_POSITIVE_FILL);
			else g.setColor(CL_INTERNAL_FORCE_NEGATIVE_FILL);

			int[] xpointsB = new int[] {b1x, b2x, sx};
			int[] zpointsB = new int[] {b1z, b2z, sz};

			g.fillPolygon(xpointsB, zpointsB, 3);
		}
	}

	/** Vynese nad úsečku AB kvadratický průběh sil. */
	private void drawQuadraticDistribution(Graphics2D g, float factor, VectorXZ a, VectorXZ b, VectorXZ normal, float ddm, float dm, float m) {
		// spočítáme délku čáry a počet a délky úseků
		int SEGMENT_LENGTH = 10;
		float length = (float)Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.z-b.z)*(a.z-b.z));
		int segments = (int)(canvas.model.scale * length / 10);
		if (segments == 0) segments = 1;
		float segmentLength = length / segments;

		VectorXZ dir = new VectorXZ(normal.z, -normal.x);

		// znaménko - 0 - zatím neurčeno, 1 - plus, -1 - minus
		int sign = 0;

		int[] xPoints = new int[segments+3];
		int[] zPoints = new int[segments+3];
		int nPoints = 0;

		// přidáme první bod
		xPoints[0] = canvas.m2cx(a.x);
		zPoints[0] = canvas.m2cz(a.z);

		// první hodnota
		float prevMoment = m;
		if (prevMoment > 0) sign = 1;
		else if (prevMoment < 0) sign = -1;

		// vyneseme první hodnotu
		xPoints[1] = canvas.m2cx(a.x - prevMoment*normal.x*factor);
		zPoints[1] = canvas.m2cz(a.z - prevMoment*normal.z*factor);
		nPoints = 2;

		float s = 0; // vzdálenost na nosníku

		//System.out.print(prevMoment + " ");

		for (int i = 0; i < segments; i++) {
			s += segmentLength;

			float moment = ddm*s*s + dm*s + m;
			//System.out.print(moment + " ");

			// pokud byl první moment nulový, nemusíme mít ještě znaménko
			if (prevMoment == 0) {
				if (moment > 0) sign = 1;
				else if (moment < 0) sign = -1;
			}

			if ((prevMoment >= 0 && moment >= 0) || (prevMoment <= 0 && moment <= 0)) {
				// pokud má tento i předchozí moment stejné znaméno, přidáme bod
				xPoints[nPoints] = canvas.m2cx(a.x + dir.x*s - moment*normal.x*factor);
				zPoints[nPoints] = canvas.m2cz(a.z + dir.z*s - moment*normal.z*factor);
				nPoints++;
			}
			else {


				// pokud má každý bod jiné znaménko, spočítáme průsečík, vykreslíme
				// body z pole, změníme barvu a přidáme do pole průsečík a aktuální bod
				Line2D.Float beamLine = new Line2D.Float(
						canvas.m2cx(a.x),
						canvas.m2cz(a.z),
						canvas.m2cx(b.x),
						canvas.m2cz(b.z));
				Line2D.Float plotLine = new Line2D.Float(
						xPoints[nPoints-1],
						zPoints[nPoints-1],
						canvas.m2cx(a.x + dir.x*s - moment*normal.x*factor),
						canvas.m2cz(a.z + dir.z*s - moment*normal.z*factor)
						);
				Point2D.Float intersection = getIntersection(beamLine, plotLine);
				if (Float.isNaN(intersection.x)) {
					intersection.x = (plotLine.x1 + plotLine.x2) / 2;
				}
				if (Float.isNaN(intersection.y)) {
					intersection.y = (plotLine.y1 + plotLine.y2) / 2;
				}
				//System.out.printf(Locale.ENGLISH, ("%f; %f\n", intersection.x, intersection.y);
				int sx = (int)Math.round(intersection.x);
				int sz = (int)Math.round(intersection.y);

				xPoints[nPoints] = sx;
				zPoints[nPoints] = sz;
				nPoints++;

				if (sign == 1) g.setColor(CL_INTERNAL_FORCE_POSITIVE_FILL);
				else g.setColor(CL_INTERNAL_FORCE_NEGATIVE_FILL);

				g.fillPolygon(xPoints, zPoints, nPoints);

				if (sign == 1) g.setColor(CL_INTERNAL_FORCE_POSITIVE);
				else g.setColor(CL_INTERNAL_FORCE_NEGATIVE);

				g.drawPolyline(xPoints, zPoints, nPoints);

				sign *= -1; // otočíme znaménko

				// přidáme průsečík
				xPoints[0] = sx;
				zPoints[0] = sz;
				xPoints[1] = canvas.m2cx(a.x + dir.x*s - moment*normal.x*factor);
				zPoints[1] = canvas.m2cz(a.z + dir.z*s - moment*normal.z*factor);
				nPoints = 2;
			}



			// pokud jsme u posledního bodu, musíme kreslit!
			if (i == segments - 1) {
				xPoints[nPoints] = canvas.m2cx(b.x);
				zPoints[nPoints] = canvas.m2cz(b.z);
				nPoints++;

				if (sign == 1) g.setColor(CL_INTERNAL_FORCE_POSITIVE_FILL);
				else g.setColor(CL_INTERNAL_FORCE_NEGATIVE_FILL);

				g.fillPolygon(xPoints, zPoints, nPoints);

				if (sign == 1) g.setColor(CL_INTERNAL_FORCE_POSITIVE);
				else g.setColor(CL_INTERNAL_FORCE_NEGATIVE);

				g.drawPolyline(xPoints, zPoints, nPoints);
			}
			else {
				prevMoment = moment;
			}
		}
		//System.out.println();
	}

	private void drawNormalForce(Graphics2D g, Beam beam) {
		drawInnerForces(g, beam, true);
	}


	private void drawShearForce(Graphics2D g, Beam beam) {
		drawInnerForces(g, beam, false);
	}

	private void drawBendingMoment(Graphics2D g, Beam beam) {
		beam.computeInternalForces();

		Iterator <Beam.ForcesDistribution> it = beam.internalForces.listIterator();
		Beam.ForcesDistribution current = null, next = null;

		while (it.hasNext()) {
			current = next;
			next = it.next();
			if (current == null) continue;

			VectorXZ line = new VectorXZ(next.x - current.x, next.z - current.z);



			// moment na konci, výchozí moment pro další segment
			float endMoment = current.mEnd;

			// lineární průběh momentu
			if (current.ddm == 0) {
				drawLinearDistribution(g, -0.1f*canvas.model.innerForcesScale/100f, current.getStartingPoint(), next.getStartingPoint(), current.dirV, current.m, endMoment);
			}
			// kvadratický průběh
			else {
				// zkusíme najít extrém
				//float exS = -0.5f*current.dm/current.ddm;

				//if (exS > 0 && exS < length) {
				if (!Float.isNaN(current.msExtreme)) {
					VectorXZ middle = new VectorXZ(current.x + line.x*current.msExtreme/current.length, current.z + line.z*current.msExtreme/current.length);

					drawQuadraticDistribution(g, -0.1f*canvas.model.innerForcesScale/100f, current.getStartingPoint(), middle, current.dirV, current.ddm, current.dm, current.m);
					//float extreme = current.ddm*exS*exS + current.dm*exS + current.m;
					drawQuadraticDistribution(g, -0.1f*canvas.model.innerForcesScale/100f, middle, next.getStartingPoint(), current.dirV, current.ddm, 0, current.mExtreme);
				}
				else {
					drawQuadraticDistribution(g, -0.1f*canvas.model.innerForcesScale/100f, current.getStartingPoint(), next.getStartingPoint(), current.dirV, current.ddm, current.dm, current.m);
				}
			}
		}
	}



	public static Point2D.Float getIntersection(Line2D.Float line1, Line2D.Float line2) {
        float x1,y1, x2,y2, x3,y3, x4,y4;
        x1 = line1.x1; y1 = line1.y1; x2 = line1.x2; y2 = line1.y2;
        x3 = line2.x1; y3 = line2.y1; x4 = line2.x2; y4 = line2.y2;
        float x = ((x2 - x1)*(x3*y4 - x4*y3) - (x4 - x3)*(x1*y2 - x2*y1)) /
                ((x1 - x2)*(y3 - y4) - (y1 - y2)*(x3 - x4) );
        float y = ((y3 - y4)*(x1*y2 - x2*y1) - (y1 - y2)*(x3*y4 - x4*y3)) /
                ((x1 - x2)*(y3 - y4) - (y1 - y2)*(x3 - x4));

        return new Point2D.Float(x, y);
    }


	@Override
	public void drawNewEntity(Graphics2D g) {
		g.setColor(CL_NEW_OBJECT);
		for (int i = 0; i < canvas.newBeam.mainPoints.size(); i++) {
			float currentX = canvas.newBeam.mainPoints.get(i).x;
			float currentZ = canvas.newBeam.mainPoints.get(i).z;

			if (i > 0) { // pokud jsme minimálně u druhého bodu, nakreslíme čáru
				g.setStroke(ST_BEAM);
				g.drawLine(canvas.m2cx(canvas.newBeam.mainPoints.get(i-1).x), canvas.m2cz(canvas.newBeam.mainPoints.get(i-1).z), canvas.m2cx(currentX), canvas.m2cz(currentZ));
				g.setStroke(ST_DEFAULT);
			}
		}

		if (canvas.newBeam.mainPoints.size() > 0) {
			g.setStroke(ST_BEAM);
			VectorXZ p = canvas.newBeam.mainPoints.get(canvas.newBeam.mainPoints.size()-1);
			g.drawLine(canvas.m2cx(p.x), canvas.m2cz(p.z), canvas.gridX(canvas.mouseX), canvas.gridZ(canvas.mouseZ));
			g.setStroke(ST_DEFAULT);
		}
		else {
			canvas.drawPoint(g, canvas.gridX(canvas.mouseX), canvas.gridZ(canvas.mouseZ));
		}

		canvas.drawMousePosition(g);
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		VectorXZ p = canvas.getPointAtMouse();
		if (canvas.newBeam.mainPoints.size() > 0 && p.equals(canvas.newBeam.mainPoints.get(canvas.newBeam.mainPoints.size()-1))) return;
		canvas.newBeam.mainPoints.add(p);
		if (!e.isControlDown() && canvas.newBeam.mainPoints.size() >= 2) {
			Beam newBeam2 = new Beam();
			newBeam2.mainPoints.add(canvas.newBeam.mainPoints.get(canvas.newBeam.mainPoints.size()-1).clone());
			canvas.newBeam.copyMainPointsToAll();
			canvas.model.addBeam(canvas.newBeam);
			canvas.newBeam = newBeam2;
		}
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity e) {
		final Beam beam = (Beam)e;
		menu.add("Informace o nosníku...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.infoWindow.setText(beam.getInfo());
				canvas.infoWindow.setVisible(true);
			}
		});
		menu.add("Izolovat v zobrazení").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.isolatedBeam = beam;
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.addSeparator();
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.model.removeBeam(beam);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.add("Topologie...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.editor.setEditedObject(beam);
				Toolkit.getDefaultToolkit().beep();
				canvas.editor.setVisible(true);
			}
		});
		menu.add("Přejmenovat...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().beep();
				String newName = (String)JOptionPane.showInputDialog(canvas, "Jméno: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, beam.name);
				if (newName != null) beam.name = newName;
				canvas.model.recalculate();
			}
		});
	}

}
