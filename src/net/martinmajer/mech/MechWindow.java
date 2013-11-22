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
import java.io.*;
import javax.swing.*;
import java.util.*;
import net.martinmajer.mech.model.Model;

import static net.martinmajer.mech.MechConsts.*;

/**
 *
 * @author Martin
 */
public class MechWindow extends JFrame {

	public Model model;
	public MechCanvas canvas;

	public MechWindow(String[] args) {
		super(FULL_TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		ImageIcon icon16 = new ImageIcon(getClass().getClassLoader().getResource("res/icons/mgif16.gif"));
		ImageIcon icon32 = new ImageIcon(getClass().getClassLoader().getResource("res/icons/mgif32.gif"));
		ImageIcon icon48 = new ImageIcon(getClass().getClassLoader().getResource("res/icons/mgif48.gif"));
		ArrayList <Image> icons = new ArrayList <Image> (3);
		icons.add(icon16.getImage());
		icons.add(icon32.getImage());
		icons.add(icon48.getImage());

		setIconImages(icons);

		canvas = new MechCanvas(false);

		//model = Model.getTestModel();
		model = new Model();

		if (args.length >= 1) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
				Model newModel = (Model)ois.readObject();
				newModel.afterLoad();
				model = newModel;
				repaint();
			} catch (Exception ex) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "Při otevírání nastala chyba!\n\n" + ex.toString(), TITLE, JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}

		model.recalculate();
		canvas.model = model;
		canvas.fitView();

		this.getContentPane().add(canvas);
		this.pack();
	}

}
