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

package net.martinmajer.mech;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class MechCanvasButton extends JPanel {

	private String text;

	private boolean active = false;

	public MechCanvasButton(String text) {
		this.text = text;

		this.setBackground(CL_BUTTON);
		this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				setBackground(CL_BUTTON_HOVER);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				setBackground(active ? CL_BUTTON_ACTIVE : CL_BUTTON);
			}
		});
	}

	public void setActive(boolean active) {
		this.active = active;
		setBackground(active ? CL_BUTTON_ACTIVE : CL_BUTTON);
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		super.paint(graphics);
		g.setColor(CL_BUTTON_TEXT);
		g.setFont(FNT_BUTTON);
		g.drawString(text, 15, 23);
	}

}
