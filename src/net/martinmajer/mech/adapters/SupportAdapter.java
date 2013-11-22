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
import net.martinmajer.mech.*;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class SupportAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Support support = (Support)e;

		if (active) g.setColor(CL_ACTIVE_SUPPORT);
		else if (support.enabled) g.setColor(CL_SUPPORT);
		else g.setColor(CL_DISABLED_SUPPORT);

		if (support instanceof Support.Fixed) {
			canvas.drawSupportSymbol(g, canvas.m2cx(support.origin.x), canvas.m2cz(support.origin.z), 0, MechCanvas.SupportType.FIXED);
		}
		else if (support instanceof Support.Pinned) {
			canvas.drawSupportSymbol(g, canvas.m2cx(support.origin.x), canvas.m2cz(support.origin.z), support.getDirectionAngle(), MechCanvas.SupportType.PINNED);
		}
		else if (support instanceof Support.Roller) {
			canvas.drawSupportSymbol(g, canvas.m2cx(support.origin.x), canvas.m2cz(support.origin.z), support.getDirectionAngle(), MechCanvas.SupportType.ROLLER);
		}
		else {
			Support.Rod rod = (Support.Rod)support;
			VectorXZ start = rod.origin;
			VectorXZ end = rod.getEnd();
			int x1 = canvas.m2cx(start.x);
			int z1 = canvas.m2cz(start.z);
			int x2 = canvas.m2cx(end.x);
			int z2 = canvas.m2cz(end.z);

			g.setStroke(ST_SUPPORT_ROD);
			g.drawLine(x1, z1, x2, z2);
			g.setStroke(ST_DEFAULT);
			canvas.drawJointCircle(g, x2, z2, 7);
		}
	}

	@Override
	public void drawNewEntity(Graphics2D g) {
		if (canvas.newSupportType != MechCanvas.SupportType.ROD) {
			int angle = 90;

			if (canvas.newObjA != null) {
				angle = canvas.getAngle(canvas.newObjA);
			}
			angle -= 180;
			if (angle < 0) angle += 360;

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
			canvas.drawSupportSymbol(g, posX, posZ, angle, canvas.newSupportType);

			if (canvas.newObjA != null) canvas.drawAngle(g, angle);
			else canvas.drawMousePosition(g);
		}
		else {
			g.setColor(CL_NEW_OBJECT);
			if (canvas.newObjA == null) {
				//g.setStroke(ST_BEAM);
				canvas.drawPoint(g, canvas.gridX(canvas.mouseX), canvas.gridZ(canvas.mouseZ));
				//g.setStroke(ST_DEFAULT);
			}
			else {
				int x1 = canvas.m2cx(canvas.newObjA.x);
				int z1 = canvas.m2cz(canvas.newObjA.z);
				int x2 = canvas.gridX(canvas.mouseX);
				int z2 = canvas.gridZ(canvas.mouseZ);

				g.setStroke(ST_SUPPORT_ROD);
				g.drawLine(x1, z1, x2, z2);
				g.setStroke(ST_DEFAULT);
			}
			canvas.drawMousePosition(g);
		}
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		if (canvas.newObjA == null) {
			canvas.newObjA = canvas.getPointAtMouse();

			if (canvas.newSupportType == MechCanvas.SupportType.FIXED) {
				Support newSupport = new Support.Fixed();
				newSupport.origin = canvas.newObjA.clone();
				newSupport.direction = new VectorXZ(0, -1);
				canvas.model.addSupport(newSupport);
				canvas.cancelDrawing(false);
			}
		}
		else {
			if (canvas.newSupportType == MechCanvas.SupportType.ROD) {
				VectorXZ b = canvas.getPointAtMouse();
				VectorXZ line = new VectorXZ(b.x - canvas.newObjA.x, b.z - canvas.newObjA.z);
				Support.Rod newSupport = new Support.Rod();
				newSupport.origin = canvas.newObjA.clone();
				newSupport.direction = line.normalize();
				newSupport.length = line.size();
				canvas.model.addSupport(newSupport);
				canvas.cancelDrawing(false);
			}
			else {
				int angle = canvas.getAngle(canvas.newObjA) - 180;
				Support newSupport = (canvas.newSupportType == MechCanvas.SupportType.ROLLER) ? new Support.Roller() : new Support.Pinned();
				newSupport.origin = canvas.newObjA.clone();
				newSupport.direction = canvas.getDirection(angle);
				canvas.model.addSupport(newSupport);
				canvas.cancelDrawing(false);
			}
		}
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity en) {
		final Support support = (Support)en;
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.model.removeSupport(support);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.add("Přesunout...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newPosition = (String)JOptionPane.showInputDialog(canvas, "Působiště podpory: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f; %.3f", support.origin.x, support.origin.z));
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

							/*support.origin.x = newX;
							support.origin.z = newZ;*/
							support.origin = new VectorXZ(newX, newZ);

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
		if (support instanceof Support.Rod) {
			menu.add("Přesunout ukotvení...").addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean ok = false;
					while (!ok) {
						Toolkit.getDefaultToolkit().beep();
						VectorXZ end = ((Support.Rod)support).getEnd();
						String newPosition = (String)JOptionPane.showInputDialog(canvas, "Souřadnice ukotvení: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f; %.3f", end.x, end.z));
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

								//support.origin = new VectorXZ(newX, newZ);
								VectorXZ line = new VectorXZ(newX - support.origin.x, newZ - support.origin.z);
								Support.Rod rod = (Support.Rod)support;
								rod.direction = line.normalize();
								rod.length = line.size();

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
		}
		else if (!(support instanceof Support.Fixed)) {
			JMenuItem menuAngle = menu.add("Otočit...");
			menuAngle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean ok = false;
					while (!ok) {
						Toolkit.getDefaultToolkit().beep();
						String newAngle = (String)JOptionPane.showInputDialog(canvas, "Orientovaný úhel: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, Integer.toString(support.getDirectionAngle()));
						if (newAngle == null) return;
						else {
							try {
								int angle = (int)Math.round(Float.parseFloat(newAngle));
								support.direction = canvas.getDirection(angle);
								canvas.model.recalculate();
								//System.out.println("recalc\n");
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
		}
		//if (support instanceof Support.Fixed) menuAngle.setEnabled(false);

		JRadioButtonMenuItem typeRoller = new JRadioButtonMenuItem("Posuvný kloub");
		JRadioButtonMenuItem typePinned = new JRadioButtonMenuItem("Pevný kloub");
		JRadioButtonMenuItem typeFixed = new JRadioButtonMenuItem("Vetknutí");
		JRadioButtonMenuItem typeRod = new JRadioButtonMenuItem("Vnější táhlo");
		typeRoller.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Support newSupport = new Support.Roller();
				newSupport.direction = support.direction;
				newSupport.origin = support.origin;
				canvas.model.removeSupport(support);
				canvas.model.addSupport(newSupport);
				canvas.repaint();
			}
		});
		typePinned.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Support newSupport = new Support.Pinned();
				newSupport.direction = support.direction;
				newSupport.origin = support.origin;
				canvas.model.removeSupport(support);
				canvas.model.addSupport(newSupport);
				canvas.repaint();
			}
		});
		typeFixed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Support newSupport = new Support.Fixed();
				newSupport.direction = support.direction;
				newSupport.origin = support.origin;
				canvas.model.removeSupport(support);
				canvas.model.addSupport(newSupport);
				canvas.repaint();
			}
		});
		typeRod.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Support.Rod newSupport = new Support.Rod();
				newSupport.direction = support.direction;
				newSupport.origin = support.origin;
				newSupport.length = 100f / canvas.model.scale;
				canvas.model.removeSupport(support);
				canvas.model.addSupport(newSupport);
				canvas.repaint();
			}
		});
		ButtonGroup g = new ButtonGroup();
		g.add(typeRoller); g.add(typePinned); g.add(typeFixed); g.add(typeRod);
		if (support instanceof Support.Roller) typeRoller.setSelected(true);
		if (support instanceof Support.Pinned) typePinned.setSelected(true);
		if (support instanceof Support.Fixed) typeFixed.setSelected(true);
		if (support instanceof Support.Rod) typeRod.setSelected(true);
		JMenu type = new JMenu("Typ");
		type.add(typeRoller);
		type.add(typePinned);
		type.add(typeFixed);
		type.add(typeRod);
		menu.add(type);
	}

}
