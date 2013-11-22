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
import java.awt.event.*;
import net.martinmajer.mech.ModelEntityAdapter;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class JointAdapter extends ModelEntityAdapter {

	@Override
	public void drawEntity(Graphics2D g, ModelEntity e, boolean active) {
		Joint joint = (Joint)e;

		int x = canvas.m2cx(joint.position.x);
		int z = canvas.m2cz(joint.position.z);

		if (active) {
			g.setColor(CL_ACTIVE_JOINT);
			g.drawRect(x-5, z-5, 10, 10);
			g.drawRect(x-4, z-4, 8, 8);
		}
		else {
			if (joint.allRods) {
				g.setColor(CL_JOINT);
				g.drawRect(x-5, z-5, 10, 10);
				g.drawRect(x-4, z-4, 8, 8);
			}
		}
	}

	@Override
	public void drawNewEntity(Graphics2D g) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void handleNewEntityMousePressed(MouseEvent e) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void fillPopupMenu(JMenu menu, ModelEntity en) {
		final Joint joint = (Joint)en;
		menu.add("Informace o kloubu...").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas.infoWindow.setText(joint.getInfo());
				canvas.infoWindow.setVisible(true);
			}
		});
		menu.addSeparator();
		menu.add("Odstranit").addActionListener(new ActionListener() {
			public void	actionPerformed(ActionEvent e) {
				for (Beam beam: joint.beams) canvas.model.removeBeam(beam);
				canvas.findActiveObjects();
				canvas.repaint();
			}
		});

		if (joint.stiff) {
			menu.add("Zrušit tuhý kloub").addActionListener(new ActionListener() {
				public void	actionPerformed(ActionEvent e) {
					canvas.model.stiffJoints.remove(joint.position);
					canvas.model.recalculate();
					canvas.repaint();
				}
			});
		}
		else {
			menu.add("Vytvořit tuhý kloub").addActionListener(new ActionListener() {
				public void	actionPerformed(ActionEvent e) {
					canvas.model.stiffJoints.add(joint.position);
					canvas.model.recalculate();
					canvas.repaint();
				}
			});
		}

	}

}
