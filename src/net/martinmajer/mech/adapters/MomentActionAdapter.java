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
public class MomentActionAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Moment.Action moment = (Moment.Action)e;

		if (active) g.setColor(CL_ACTIVE_FORCE);
		else if (moment.enabled) g.setColor(CL_FORCE);
		else g.setColor(CL_DISABLED_FORCE);
		canvas.drawMomentSymbol(g, canvas.m2cx(moment.origin.x), canvas.m2cz(moment.origin.z), moment.size > 0);
	}

	@Override
	public void drawNewEntity(Graphics2D g) {
		int posX, posZ;

		posX = canvas.gridX(canvas.mouseX);
		posZ = canvas.gridZ(canvas.mouseZ);

		g.setColor(CL_NEW_OBJECT);
		canvas.drawMomentSymbol(g, posX, posZ, true);

		canvas.drawMousePosition(g);
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		boolean sizeOk = false;
		float size = 0;
		while (!sizeOk) {
			Toolkit.getDefaultToolkit().beep();
			String sizeStr = (String)JOptionPane.showInputDialog(canvas, "Velikost momentu [kNm]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, "20.000");
			if (sizeStr == null) return;
			try {
				size = Float.parseFloat(sizeStr);
				sizeOk = true;
			}
			catch (NumberFormatException ex) {}
		}

		int posX = canvas.gridX(canvas.mouseX);
		int posZ = canvas.gridZ(canvas.mouseZ);

		Moment.Action newMoment = new Moment.Action();
		newMoment.origin = new VectorXZ(canvas.c2mx(posX), canvas.c2mz(posZ));
		newMoment.size = size;

		canvas.model.addMoment(newMoment);
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity en) {
		final Moment.Action moment = (Moment.Action)en;
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.model.removeMoment(moment);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});
		menu.add("Přesunout...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newPosition = (String)JOptionPane.showInputDialog(canvas, "Působiště momentu: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f; %.3f", moment.origin.x, moment.origin.z));
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

							moment.origin = new VectorXZ(newX, newZ);

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
		menu.add("Změnit velikost...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean ok = false;
				while (!ok) {
					Toolkit.getDefaultToolkit().beep();
					String newSize = (String)JOptionPane.showInputDialog(canvas, "Velikost [kNm]: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, String.format(Locale.ENGLISH, "%.3f", moment.size));
					if (newSize == null) return;
					else {
						try {
							float size = Float.parseFloat(newSize);
							moment.size = size;
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
				String newName = (String)JOptionPane.showInputDialog(canvas, "Jméno: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, moment.name);
				if (newName != null) moment.name = newName;
				canvas.model.recalculate();
			}
		});
	}

}
