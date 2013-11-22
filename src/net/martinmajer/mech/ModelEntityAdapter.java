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
import java.awt.event.MouseEvent;
import javax.swing.JMenu;
import net.martinmajer.mech.model.*;

/**
 * Rozhraní, které převádí objekty modelu na objekty,
 * které lze nakreslit, prohlížet a editovat na plátně.
 *
 * @author Martin
 */
public abstract class ModelEntityAdapter {

	public MechCanvas canvas;


	/** Nakreslí prvek. */
	public abstract void drawEntity(Graphics2D g, ModelEntity e, boolean active);

	/** Nakreslí prvek, který je právě přidáván na plátno. */
	public abstract void drawNewEntity(Graphics2D g);

	/** Obslouží kliknutí myší při kreslení. */
	public abstract void handleNewEntityMousePressed(MouseEvent e);

	/** Naplní vyskakovací menu. */
	public abstract void fillPopupMenu(JMenu menu, ModelEntity e);

}
