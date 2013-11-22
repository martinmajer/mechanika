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

import java.util.Locale;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import net.martinmajer.mech.ModelEntityAdapter;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class ForceActionAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Force.Action force = (Force.Action)e;

		if (active) g.setColor(CL_ACTIVE_FORCE);
		else if (force.enabled) g.setColor(CL_FORCE);
		else g.setColor(CL_DISABLED_FORCE);
		canvas.drawArrow(g, canvas.m2cx(force.origin.x), canvas.m2cz(force.origin.z), force.getDirectionAngle(), ARROW_LENGTH);
	}

	@Override
	public void drawNewEntity(Graphics2D g) {
		int angle = 90;

		if (canvas.newObjA != null) {
			angle = canvas.getAngle(canvas.newObjA);
		}

		int posX, posZ;
		if (canvas.newObjA != null) {
			posX = canvas.m2cx(canvas.newObjA.x);
			posZ = canvas.m2cz(canvas.newObjA.z);
		}
		else {
			posX = canvas.gridX(canvas.mouseX);
			posZ = canvas.gridZ(canvas.mouseZ);
		}

		g.setColor(CL_NEW_OBJECT);
		canvas.drawArrow(g, posX, posZ, angle, ARROW_LENGTH);

		if (canvas.newObjA != null) canvas.drawAngle(g, angle);
		else canvas.drawMousePosition(g);
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		if (canvas.newObjA == null) {
			canvas.newObjA = canvas.getPointAtMouse();
		}
		else {
			int angle = canvas.getAngle(canvas.newObjA);
			boolean sizeOk = false;
			float size = 0;
			while (!sizeOk) {
				Toolkit.getDefaultToolkit().beep();
				String sizeStr = (String)JOptionPane.showInputDialog(canvas, "Velikost síly [kN]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, /*"100"*/ String.format(Locale.ENGLISH, "%.3f", 500f / canvas.model.scale));
				if (sizeStr == null) return;
				try {
					size = Float.parseFloat(sizeStr);
					sizeOk = true;
				}
				catch (NumberFormatException ex) {}
			}

			Force.Action newForce = new Force.Action();
			newForce.origin = canvas.newObjA.clone();
			newForce.size = size;
			newForce.direction = canvas.getDirection(angle);

			canvas.model.addForce(newForce);

			canvas.newObjA = null;
		}
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity en) {
		final Force.Action force = (Force.Action)en;
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.model.removeForce(force);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.add("Přesunout...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newPosition = (String)JOptionPane.showInputDialog(canvas, "Působiště síly: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f; %.3f", force.origin.x, force.origin.z));
					if (newPosition == null) return;
					else {
						String[] coords = newPosition.split(";");
						if (coords.length < 2) {
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(canvas, "Zadali jste málo souřadnic!", TITLE, JOptionPane.ERROR_MESSAGE);
							continue;
						}
						try {
							float newX = Float.parseFloat(coords[0]);
							float newZ = Float.parseFloat(coords[1]);

							force.origin = new VectorXZ(newX, newZ);

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
					String newAngle = (String)JOptionPane.showInputDialog(canvas, "Orientovaný úhel: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, Integer.toString(force.getDirectionAngle()));
					if (newAngle == null) return;
					else {
						try {
							int angle = (int)Math.round(Float.parseFloat(newAngle));
							force.direction = canvas.getDirection(angle);
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
					String newSize = (String)JOptionPane.showInputDialog(canvas, "Velikost [kN]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f", force.size));
					if (newSize == null) return;
					else {
						try {
							float size = Float.parseFloat(newSize);
							force.size = size;
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
				String newName = (String)JOptionPane.showInputDialog(canvas, "Jméno: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, force.name);
				if (newName != null) force.name = newName;
				canvas.model.recalculate();
			}
		});
	}

}
