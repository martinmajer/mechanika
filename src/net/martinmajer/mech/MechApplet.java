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

import java.util.Locale;
import javax.swing.*;
import net.martinmajer.mech.model.*;

/**
 *
 * @author Martin
 */
public class MechApplet extends JApplet {

	@Override
	public void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException ex) {}
		catch (InstantiationException ex) {}
		catch (IllegalAccessException ex) {}
		catch (UnsupportedLookAndFeelException ex) {}

		/*try {
			Locale.setDefault(Locale.ENGLISH);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.toString());
		}*/

		MechCanvas canvas = new MechCanvas(true);
		canvas.model = new Model();
		canvas.model.recalculate();
		getContentPane().add(canvas);
	}

}
