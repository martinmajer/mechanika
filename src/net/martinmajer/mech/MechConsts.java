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

/**
 *
 * @author Martin
 */
public class MechConsts {

	// ====== BARVY ======
	public static final Color CL_BACKGROUND = Color.WHITE;
	public static final Color CL_BUTTON = Color.BLACK;
	public static final Color CL_BUTTON_ACTIVE = new Color(80, 80, 80);
	public static final Color CL_BUTTON_HOVER = new Color(60, 60, 60);
	public static final Color CL_BUTTON_TEXT = Color.WHITE;

	public static final Color CL_AXIS_X = Color.BLUE;
	public static final Color CL_AXIS_Z = Color.RED;

	public static final Color CL_INNER_FORCES_SCALE = Color.BLACK;

	public static final Color CL_GRID = new Color(192, 192, 192);
	public static final Color CL_MAIN_GRID = new Color(224, 224, 224);
	public static final Color CL_NEW_OBJECT = new Color(255, 128, 0);

	public static final Color CL_BEAM = Color.BLACK;
	public static final Color CL_ACTIVE_BEAM = new Color(128, 128, 128);
	public static final Color CL_JOINT = Color.BLACK;
	public static final Color CL_ACTIVE_JOINT = new Color(0, 192, 0);

	public static final Color CL_FORCE = new Color(0, 192, 0);
	public static final Color CL_ACTIVE_FORCE = new Color(170, 210, 60);
	public static final Color CL_DISABLED_FORCE = new Color(128, 128, 128);

	public static final Color CL_LOAD = new Color(0, 192, 0);
	public static final Color CL_ACTIVE_LOAD = new Color(170, 210, 60);
	public static final Color CL_DISABLED_LOAD = new Color(128, 128, 128);

	public static final Color CL_SUPPORT = Color.BLACK;
	public static final Color CL_ACTIVE_SUPPORT = new Color(170, 210, 60);
	public static final Color CL_DISABLED_SUPPORT = new Color(128, 128, 128);

	public static final Color CL_INTERNAL_FORCE_NEGATIVE = Color.RED;
	public static final Color CL_INTERNAL_FORCE_NEGATIVE_FILL = new Color(255, 0, 0, 80);
	public static final Color CL_INTERNAL_FORCE_POSITIVE = Color.BLUE;
	public static final Color CL_INTERNAL_FORCE_POSITIVE_FILL = new Color(0, 0, 255, 80);

	public static final Color CL_TEXT = Color.BLACK;

	// ====== PÍSMA ======
	public static Font FNT_BUTTON;
	public static Font FNT_TEXT;


	static {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String fontNames[] = ge.getAvailableFontFamilyNames();
		String fontName = "Monospaced";
		for (String font: fontNames) {
			if (font.equalsIgnoreCase("Courier New")) {
				fontName = "Courier New";
				break;
			}
		}
		FNT_BUTTON = new Font(fontName, Font.PLAIN, 22);
		FNT_TEXT = new Font(fontName, Font.PLAIN, 12);
	}

	// ====== ČÁRY ======
	public static final Stroke ST_DEFAULT = new BasicStroke(1);
	public static final Stroke ST_BEAM = new BasicStroke(3);
	public static final Stroke ST_SUPPORT_ROD = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 2, new float[] {5, 5}, 0);
	public static final Stroke ST_ROD = new BasicStroke(1);

	// ====== OSTATNÍ KONSTANTY ======
	public static final int GRID_SPACING = 10;
	public static final int MAIN_GRID_SPACING = 50;
	public static final int AXIS_LENGTH = 50;
	public static final int ARROW_LENGTH = 30;
	public static final int LOAD_ARROW_LENGTH = 20;
	public static final int SUPPORT_VIRTUAL_LENGTH = 13;

	public static final String VERSION = "1.0";
	public static final String TITLE = "Mechanika";
	public static final String FULL_TITLE = TITLE + " " + VERSION;

}
