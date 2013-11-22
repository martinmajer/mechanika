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

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import net.martinmajer.mech.*;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class LoadAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Load load = (Load)e;

		int x1 = canvas.m2cx(load.start.x);
		int z1 = canvas.m2cz(load.start.z);
		int x2 = canvas.m2cx(load.end.x);
		int z2 = canvas.m2cz(load.end.z);

		int angle = load.getDirectionAngle();

		float length = (float)Math.sqrt((x1-x2)*(x1-x2) + (z1-z2)*(z1-z2));
		float steps = length / 10;
		float xStep = (x2-x1) / steps;
		float zStep = (z2-z1) / steps;

		Iterator <Float> it = load.activeParts.iterator();
		boolean disabled = false;
		float currentActivePart = 0;
		boolean partActive = false;
		if (load.activeParts.size() > 0) {
			currentActivePart = it.next();
		}
		else  {
			currentActivePart = Float.POSITIVE_INFINITY;
		}

		for (int i = 0; i < (int)steps + 1; i++) {
			float dist = length / steps * i;
			float p = dist / length;

			if (active) g.setColor(CL_ACTIVE_LOAD);
				else {
				// 1 / length -> tolerance
				if (partActive) {
					if (p > currentActivePart + 1 / length) {
						partActive = false;
						if (it.hasNext()) currentActivePart = it.next();
						else currentActivePart = Float.POSITIVE_INFINITY;
						g.setColor(CL_DISABLED_LOAD);
					}
					else {
						g.setColor(CL_LOAD);
					}
				}
				else {

					if (p >= currentActivePart - 1 / length) {
						partActive = true;
						if (it.hasNext()) currentActivePart = it.next();
						else currentActivePart = Float.POSITIVE_INFINITY;
						g.setColor(CL_LOAD);
					}
					else {
						g.setColor(CL_DISABLED_LOAD);
					}
				}
			}

			canvas.drawArrow(g, (int)Math.round(x1 + i*xStep), (int)Math.round(z1 + i*zStep), angle, 12);
		}
	}

	@Override
	public void drawNewEntity(Graphics2D g) {
		g.setColor(CL_NEW_OBJECT);

		int angle = 90;

		if (canvas.newObjA == null) {
			//g.setStroke(ST_BEAM);
			canvas.drawPoint(g, canvas.gridX(canvas.mouseX), canvas.gridZ(canvas.mouseZ));
			//g.setStroke(ST_DEFAULT);
		}
		else {

			int x1 = canvas.m2cx(canvas.newObjA.x);
			int z1 = canvas.m2cz(canvas.newObjA.z);
			int x2, z2;
			if (canvas.newObjB != null) {
				x2 = canvas.m2cx(canvas.newObjB.x);
				z2 = canvas.m2cz(canvas.newObjB.z);
			}
			else {
				x2 = canvas.gridX(canvas.mouseX);
				z2 = canvas.gridZ(canvas.mouseZ);
			}

			if (canvas.newObjB != null) {
				angle = canvas.getAngle(new VectorXZ(
						(canvas.newObjA.x + canvas.newObjB.x) / 2,
						(canvas.newObjA.z + canvas.newObjB.z) / 2
						));
			}

			// nakreslíme šipky nad čáru...
			float length = (float)Math.sqrt((x1-x2)*(x1-x2) + (z1-z2)*(z1-z2));
			float steps = length / 10;
			float xStep = (x2-x1) / steps;
			float zStep = (z2-z1) / steps;

			for (int i = 0; i < (int)steps + 1; i++) {
				canvas.drawArrow(g, (int)Math.round(x1 + i*xStep), (int)Math.round(z1 + i*zStep), angle, 12);
			}
		}

		if (canvas.newObjB == null) canvas.drawMousePosition(g);
		else canvas.drawAngle(g, angle);
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		if (canvas.newObjA == null) {
			canvas.newObjA = canvas.getPointAtMouse();
		}
		else if (canvas.newObjB == null) {
			canvas.newObjB = canvas.getPointAtMouse();
		}
		else {
			int angle = canvas.getAngle(new VectorXZ(
					(canvas.newObjA.x + canvas.newObjB.x) / 2,
					(canvas.newObjA.z + canvas.newObjB.z) / 2
					));

			boolean sizeOk = false;
			float size = 0;
			while (!sizeOk) {
				Toolkit.getDefaultToolkit().beep();
				String sizeStr = (String)JOptionPane.showInputDialog(canvas, "Velikost zatížení [kN/m]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, "5.000" /*Integer.toString(canvas.model.scale)*/);
				if (sizeStr == null) return;
				try {
					size = Float.parseFloat(sizeStr);
					sizeOk = true;
				}
				catch (NumberFormatException ex) {}
			}

			Load newLoad = new Load();
			newLoad.start = canvas.newObjA;
			newLoad.end = canvas.newObjB;
			newLoad.direction = canvas.getDirection(angle);
			newLoad.sizePerPixel = size;

			canvas.model.addLoad(newLoad);

			//canvas.cancelDrawing(false);
			canvas.newObjA = null;
			canvas.newObjB = null;
		}
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity en) {
		final Load load = (Load)en;
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.model.removeLoad(load);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.add("Přesunout...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newPosition = (String)JOptionPane.showInputDialog(canvas, "Působiště síly: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f; %.3f; %.3f; %.3f", load.start.x, load.start.z, load.end.x, load.end.z));
					if (newPosition == null) return;
					else {
						String[] coords = newPosition.split(";");
						if (coords.length < 4) {
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(canvas, "Zadali jste málo souřadnic!", TITLE, JOptionPane.ERROR_MESSAGE);
							continue;
						}
						try {
							float newX1 = Float.parseFloat(coords[0]);
							float newZ1 = Float.parseFloat(coords[1]);
							float newX2 = Float.parseFloat(coords[2]);
							float newZ2 = Float.parseFloat(coords[3]);


							load.start = new VectorXZ(newX1, newZ1);
							load.end = new VectorXZ(newX2, newZ2);

							canvas.model.recalculate();
							ok = true;
							canvas.repaint();
						}
						catch (NumberFormatException ex) {
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(canvas, "Chybný formát souřadnic!", TITLE, JOptionPane.ERROR_MESSAGE);
							continue;
						}
					}
				}
			}
		});
		menu.add("Otočit...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newAngle = (String)JOptionPane.showInputDialog(canvas, "Orientovaný úhel: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, Integer.toString(load.getDirectionAngle()));
					if (newAngle == null) return;
					else {
						try {
							int angle = (int)Math.round(Float.parseFloat(newAngle));
							load.direction = canvas.getDirection(angle);
							canvas.model.recalculate();
							ok = true;
							canvas.repaint();
						}
						catch (NumberFormatException ex) {
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(canvas, "Chybný formát úhlu!", TITLE, JOptionPane.ERROR_MESSAGE);
							continue;
						}
					}
				}
			}
		});
		menu.add("Změnit velikost...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newSize = (String)JOptionPane.showInputDialog(canvas, "Velikost [kN/m]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.6f", load.sizePerPixel));
					if (newSize == null) return;
					else {
						try {
							float size = Float.parseFloat(newSize);
							load.sizePerPixel = size;
							canvas.model.recalculate();
							ok = true;
							canvas.repaint();
						}
						catch (NumberFormatException ex) {
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(canvas, "Chybný formát velikosti!", TITLE, JOptionPane.ERROR_MESSAGE);
							continue;
						}
					}
				}
			}
		});
		menu.add("Přejmenovat...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().beep();
				String newName = (String)JOptionPane.showInputDialog(canvas, "Jméno: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, load.name);
				if (newName != null) load.name = newName;
				canvas.model.recalculate();
			}
		});
	}

}
