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

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import net.martinmajer.mech.adapters.*;
import net.martinmajer.mech.model.*;

import static net.martinmajer.mech.MechConsts.*;

/**
 * Plátno pro kreslení a zobrazování modelu.
 * @author Martin
 *
 * @todo přidat zobrazování AWT výjimek v MessageBoxu
 */
public class MechCanvas extends JPanel implements ComponentListener, MouseListener, MouseMotionListener, KeyListener {


	public boolean applet = false;

	// ====== MODEL ======

	/** Model. */
	public Model model;

	// ====== ZOBRAZENÍ MODELU ======

	/** Počátek souřadného systému. */
	private int originX, originZ;


	private boolean showAxes = true;
	private boolean showGrid = true;
	private boolean antialiasing = true;
	private boolean showReactions = true;

	/** Zpráva ve statovém řádku. */
	private String status = null;

	// Stav myši apod.
	public int mouseX, mouseZ;
	public int mousePressedX, mousePressedZ;
	public boolean mouseMiddlePressed = false;

	public Joint draggedJoint = null;

	// měřítko vykreslování vnitřních sil
	//public int innerForcesScale = 100;

	/** Seznam aktivních objektů. */
	private ArrayList <ModelEntity> activeObjects = new ArrayList <ModelEntity>();


	public static enum ViewMode { EDITOR, NORMAL_FORCE, SHEAR_FORCE, BENDING_MOMENT };

	public ViewMode viewMode = ViewMode.EDITOR;

	public Beam isolatedBeam = null;


	// ====== PŘÍDAVNÉ KOMPONENTY ======

	// tlačítka
	private MechCanvasButton btnBeam, btnForce, btnMoment, btnLoad, btnSupport;
	private MechCanvasButton btnEditor, btnShear, btnNormal, btnBendMoment;

	/** Vyskakovací menu. */
	private JPopupMenu popupMenu = new JPopupMenu();

	private JMenu menuModel;
	private JMenu menuViewMode;
	private JRadioButtonMenuItem menuViewModeEditor, menuViewModeShear, menuViewModeNormal, menuViewModeBendMoment;
	private JMenuItem menuShowAll;
	private JMenuItem menuScale;
	private JMenuItem menuInfo;
	private JMenuItem menuAbout;
	
	public MechInfoWindow infoWindow;
	public MechTextEditor editor;
	private MechAboutDialog aboutDialog = null;


	// ====== KRESLENÍ NOVÝCH OBJEKTŮ ======
	public static enum SupportType { ROLLER, PINNED, FIXED, ROD }

	/** Kreslící stav / typ kresleného objektu. */
	private Class drawingState = null;

	public Beam newBeam = null;
	public VectorXZ newObjA = null;
	public VectorXZ newObjB = null;
	public SupportType newSupportType = null;
	

	// ====== ADAPTÉRY PRO VYKRESLOVÁNÍ OBJEKTŮ Z MODELU ======
	private ModelEntityAdapter beamAdapter;
	private ModelEntityAdapter forceActionAdapter;
	private ModelEntityAdapter jointAdapter;
	private ModelEntityAdapter loadAdapter;
	private ModelEntityAdapter momentActionAdapter;
	private ModelEntityAdapter supportAdapter;

	private Map <Class, ModelEntityAdapter> modelAdapters = new HashMap <Class, ModelEntityAdapter>();




	/** Vytvoří nové plátno. */
	public MechCanvas(boolean applet) {
		this.applet = applet;

		setupAdapters();		// adaptéry objektů z modelu
		setupButtons();			// tlačítka
		setupPopupMenu();		// vyskakovací menu
		setupOtherWindows();	// ostatní okna
		setupListeners();		// posluchače událostí

		this.setOpaque(false);
		this.setFocusable(true);
		this.requestFocus();
	}

	private void setupAdapters() {
		beamAdapter = new BeamAdapter();
		modelAdapters.put(Beam.class, beamAdapter);
		forceActionAdapter = new ForceActionAdapter();
		modelAdapters.put(Force.Action.class, forceActionAdapter);
		jointAdapter = new JointAdapter();
		modelAdapters.put(Joint.class, jointAdapter);
		loadAdapter = new LoadAdapter();
		modelAdapters.put(Load.class, loadAdapter);
		momentActionAdapter = new MomentActionAdapter();
		modelAdapters.put(Moment.Action.class, momentActionAdapter);
		supportAdapter = new SupportAdapter();
		modelAdapters.put(Support.class, supportAdapter);
		modelAdapters.put(Support.Pinned.class, supportAdapter);
		modelAdapters.put(Support.Roller.class, supportAdapter);
		modelAdapters.put(Support.Fixed.class, supportAdapter);
		modelAdapters.put(Support.Rod.class, supportAdapter);

		for (Class cl: modelAdapters.keySet()) {
			modelAdapters.get(cl).canvas = this;
		}
	}

	private void setupButtons() {
		// Tlačítka
		this.setLayout(null);
		btnBeam = new MechCanvasButton("Nosník");
		btnBeam.setLocation(10, 10);
		btnBeam.setSize(115, 30);
		btnBeam.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				startDrawingBeam();
			}
		});
		this.add(btnBeam);

		btnForce = new MechCanvasButton("Síla");
		btnForce.setLocation(10, 48);
		btnForce.setSize(90, 30);
		btnForce.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				startDrawingForce();
			}
		});
		this.add(btnForce);

		btnMoment = new MechCanvasButton("Moment");
		btnMoment.setLocation(110, 48);
		btnMoment.setSize(110, 30);
		btnMoment.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				startDrawingMoment();
			}
		});
		this.add(btnMoment);

		btnLoad = new MechCanvasButton("Spojité zatížení");
		btnLoad.setLocation(10, 86);
		btnLoad.setSize(240, 30);
		btnLoad.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				startDrawingLoad();
			}
		});
		this.add(btnLoad);

		btnSupport = new MechCanvasButton("Vnější vazba");
		btnSupport.setLocation(10, 124);
		btnSupport.setSize(190, 30);
		btnSupport.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				startDrawingSupport();
			}
		});
		this.add(btnSupport);

		btnEditor = new MechCanvasButton("E");
		btnEditor.setToolTipText("Editor");
		btnEditor.setSize(45, 30);
		btnEditor.setActive(true);
		btnEditor.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setViewMode(ViewMode.EDITOR);
			}
		});
		this.add(btnEditor);

		btnNormal = new MechCanvasButton("N");
		btnNormal.setToolTipText("Normálové síly");
		btnNormal.setSize(45, 30);
		btnNormal.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setViewMode(ViewMode.NORMAL_FORCE);
			}
		});
		this.add(btnNormal);

		btnShear = new MechCanvasButton("V");
		btnShear.setToolTipText("Posouvající síly");
		btnShear.setSize(45, 30);
		btnShear.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setViewMode(ViewMode.SHEAR_FORCE);
			}
		});
		this.add(btnShear);

		btnBendMoment = new MechCanvasButton("M");
		btnBendMoment.setToolTipText("Ohybový moment");
		btnBendMoment.setSize(45, 30);
		btnBendMoment.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setViewMode(ViewMode.BENDING_MOMENT);
			}
		});
		this.add(btnBendMoment);
	}

	private void setupPopupMenu() {
		menuModel = new JMenu("Model");
		JMenuItem menuModelClear = new JMenuItem("Vymazat");
		JMenuItem menuModelExport = new JMenuItem("Exportovat...");
		JMenuItem menuModelImport = new JMenuItem("Importovat...");

		menuModel.add(menuModelClear);
		menuModel.add(menuModelExport);
		menuModel.add(menuModelImport);
		if (applet) {
			menuModelExport.setEnabled(false);
			menuModelImport.setEnabled(false);
		}

		menuModelClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().beep();
				int result = JOptionPane.showConfirmDialog(getCanvas(), "Opravdu chcete smazat současný model?", TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result == JOptionPane.NO_OPTION) return;

				getCanvas().model = new Model();
			}
		});
		menuModelExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				fileChooser.setDialogTitle(TITLE);
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
				int retVal = fileChooser.showSaveDialog(getCanvas());
				if (retVal == JFileChooser.APPROVE_OPTION) {
					File f = fileChooser.getSelectedFile();
					if (f.exists()) {
						Toolkit.getDefaultToolkit().beep();
						int result = JOptionPane.showConfirmDialog(getCanvas(), "Soubor '" + f.getName() + "' existuje, chcete ho přepsat?", TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
						if (result == JOptionPane.NO_OPTION) return;
					}
					try {
						ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
						oos.writeObject(model);
						oos.close();
					} catch (Exception ex) {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(getCanvas(), "Při ukládání nastala chyba!\n\n" + ex.toString(), TITLE, JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					}
				}
			}
		});
		menuModelImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				fileChooser.setDialogTitle(TITLE);
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
				int retVal = fileChooser.showOpenDialog(getCanvas());
				if (retVal == JFileChooser.APPROVE_OPTION) {
					File f = fileChooser.getSelectedFile();
					try {
						ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
						Model newModel = (Model)ois.readObject();
						newModel.afterLoad();
						newModel.recalculate();
						getCanvas().model = newModel;
						fitView();
						repaint();
					} catch (Exception ex) {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(getCanvas(), "Při otevírání nastala chyba!\n\n" + ex.toString(), TITLE, JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					}
				}
			}
		});


		menuViewMode = new JMenu("Zobrazení");
		menuViewModeEditor = new JRadioButtonMenuItem("Editor");
		menuViewModeEditor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.EDITOR);
			}
		});
		menuViewModeNormal = new JRadioButtonMenuItem("Normálové síly");
		menuViewModeNormal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.NORMAL_FORCE);
			}
		});
		menuViewModeShear = new JRadioButtonMenuItem("Posouvající síly");
		menuViewModeShear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.SHEAR_FORCE);
			}
		});
		menuViewModeBendMoment = new JRadioButtonMenuItem("Ohybové momenty");
		menuViewModeBendMoment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.BENDING_MOMENT);
			}
		});
		menuViewMode.add(menuViewModeEditor);
		menuViewMode.add(menuViewModeNormal);
		menuViewMode.add(menuViewModeShear);
		menuViewMode.add(menuViewModeBendMoment);

		ButtonGroup group = new ButtonGroup();
		group.add(menuViewModeEditor);
		group.add(menuViewModeNormal);
		group.add(menuViewModeShear);
		group.add(menuViewModeBendMoment);

		menuViewModeEditor.setSelected(true);

		menuViewMode.addSeparator();

		final JMenuItem menuInnerForcesScale = new JMenuItem("Měřítko průběhů sil...");
		menuInnerForcesScale.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().beep();
				String scaleStr = (String)JOptionPane.showInputDialog(getCanvas(), "Zadejte měřítko [%]:", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, Integer.toString(model.innerForcesScale));
				if (scaleStr == null) return;
				try {
					int scale = Integer.parseInt(scaleStr);
					model.innerForcesScale = scale;
					repaint();
				}
				catch (NumberFormatException ex) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(getCanvas(), "Neplatné měřítko!", TITLE, JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		menuViewMode.add(menuInnerForcesScale);

		final JCheckBoxMenuItem menuShowAxes = new JCheckBoxMenuItem("Zobrazit osy");
		menuShowAxes.setSelected(true);
		menuShowAxes.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				showAxes = menuShowAxes.getState();
				repaint();
			}
		});
		menuViewMode.add(menuShowAxes);
		final JCheckBoxMenuItem menuShowGrid = new JCheckBoxMenuItem("Zobrazit mřížku");
		menuShowGrid.setSelected(true);
		menuShowGrid.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				showGrid = menuShowGrid.getState();
				repaint();
			}
		});
		final JCheckBoxMenuItem menuAntialiasing = new JCheckBoxMenuItem("Vyhlazování čar");
		menuAntialiasing.setSelected(true);
		menuAntialiasing.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				antialiasing = menuAntialiasing.getState();
				repaint();
			}
		});
		final JCheckBoxMenuItem menuShowReactions = new JCheckBoxMenuItem("Seznam reakcí");
		menuShowReactions.setSelected(true);
		menuShowReactions.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				showReactions = menuShowReactions.getState();
				repaint();
			}
		});
		menuViewMode.add(menuShowReactions);
		menuViewMode.add(menuShowGrid);
		menuViewMode.add(menuShowAxes);
		menuViewMode.add(menuAntialiasing);



		menuScale = new JMenuItem("Nastavit měřítko...");
		menuScale.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (model.beams.size() > 0) {
					Toolkit.getDefaultToolkit().beep();
					int result = JOptionPane.showConfirmDialog(getCanvas(), "Výpočty budou po změně měřítka prováděny s jinou tolerancí, pro kterou nemusí současný model vyhovovat.\n\nOpravdu chcete pokračovat?", TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result == JOptionPane.NO_OPTION) return;
				}

				Toolkit.getDefaultToolkit().beep();
				String scaleStr = (String)JOptionPane.showInputDialog(getCanvas(), "Zadajte měřítko (počet pixelů / metr):", TITLE, JOptionPane.QUESTION_MESSAGE, null, null, Integer.toString(model.scale));
				if (scaleStr == null) return;
				try {
					int scale = Integer.parseInt(scaleStr);
					model.scale = scale;
					model.recalculate();
					repaint();
				}
				catch (NumberFormatException ex) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(getCanvas(), "Neplatné měřítko!", TITLE, JOptionPane.ERROR_MESSAGE);
				}

			}
		});

		menuShowAll = new JMenuItem("Zobrazit vše");
		menuShowAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isolatedBeam = null;
				findActiveObjects();
				repaint();
			}
		});


		menuInfo = new JMenuItem("Informace o modelu...");
		menuInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				infoWindow.setText(model.getModelInfo());
				infoWindow.setVisible(true);
			}
		});

		menuAbout = new JMenuItem("O programu...");
		menuAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (aboutDialog == null) {
					aboutDialog = new MechAboutDialog(null, false);
					aboutDialog.setLocation(100, 100);
				}
				aboutDialog.setVisible(true);
			}
		});
	}

	private void setupOtherWindows() {
		infoWindow = new MechInfoWindow();
		infoWindow.setLocation(100, 100);

		editor = new MechTextEditor(this);
		editor.setLocation(100, 100);
	}

	private void setupListeners() {
		this.addComponentListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);
	}

	private void setViewMode(ViewMode viewMode) {
		btnEditor.setActive(false);
		btnNormal.setActive(false);
		btnShear.setActive(false);
		btnBendMoment.setActive(false);
		menuViewModeEditor.setSelected(false);
		menuViewModeNormal.setSelected(false);
		menuViewModeShear.setSelected(false);
		menuViewModeBendMoment.setSelected(false);

		this.viewMode = viewMode;

		if (viewMode == ViewMode.EDITOR) {
			btnEditor.setActive(true);
			menuViewModeEditor.setSelected(true);
		}
		else if (viewMode == ViewMode.NORMAL_FORCE) {
			btnNormal.setActive(true);
			menuViewModeNormal.setSelected(true);
		}
		else if(viewMode == ViewMode.SHEAR_FORCE) {
			btnShear.setActive(true);
			menuViewModeShear.setSelected(true);
		}
		else if (viewMode == ViewMode.BENDING_MOMENT) {
			btnBendMoment.setActive(true);
			menuViewModeBendMoment.setSelected(true);
		}

		repaint();
	}

	/** Vrací "this" - pro použití ve vnitřních třídách. */
	protected MechCanvas getCanvas() {
		return this;
	}

	/** Model X -> plátno X. */
	public int m2cx(float modelX) { return Math.round(modelX*model.scale) + originX; }

	/** Model Z -> plátno Z. */
	public int m2cz(float modelZ) { return Math.round(modelZ*model.scale) + originZ; }

	/** Plátno X -> model X. */
	public float c2mx(int canvasX) { return (float)(canvasX - originX) / model.scale; }

	/** Plátno Z -> model Z. */
	public float c2mz(int canvasZ) { return (float)(canvasZ - originZ) / model.scale; }

	/** Přepočítá x-ovou souřadnici na obrazovce podle mřížky. */
	public int gridX(int x) {
		if (!showGrid) return x;
		int o = originX % GRID_SPACING;
		int p = 5 - o;
		return (x+p) - (x+p) % GRID_SPACING + o;
	}

	/** Přepočítá z-ovou souřadnici na obrazovce podle mřížky. */
	public int gridZ(int z) {
		if (!showGrid) return z;
		int o = originZ % GRID_SPACING;
		int p = 5 - o;
		return (z+p) - (z+p) % GRID_SPACING + o;
	}

	
	public String getStatusBarMessage() {
		if (status != null) return status;
		//else return "Seminární práce SMA1 / Martin Majer / A1-5.";
		else return "";
	}

	/** Kreslící metoda plátna. */
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		g.setFont(FNT_TEXT);
		if (antialiasing) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

		drawBackground(g);	// pozadí
		if (showGrid) drawGrid(g); // mřížka
		drawModel(g);		// model
		drawNewObjects(g);	// nové objekty přidávané do modelu
		if (showAxes) drawAxes(g); // osy
		drawStatus(g);		// statový řádek
		if (showReactions) drawReactions(g);   // reakce
		drawInnerForcesScale(g);

		this.paintChildren(g); // tlačítka apod.
	}

	/** Nakreslí kolečko. */
	public void drawJointCircle(Graphics g, int x, int z, int r) {
		r -= 1;
		g.drawArc(x-r/2, z-r/2, r, r, 0, 360);
	}

	/** Nakreslí křížek (pod myší). */
	public void drawPoint(Graphics g, int x, int z) {
		g.drawLine(x-3, z-3, x+3, z+3);
		g.drawLine(x-3, z+3, x+3, z-3);
	}

	/** Nakreslí šipku. */
	public void drawArrow(Graphics2D g, int x, int z, float angle, int length) {
		int length2 = length / 3;
		int width = length2 + 2;
		angle += 180;
		double angleRad = angle / 180 * Math.PI;

		float cos = (float)Math.cos(angleRad);
		float sin = (float)Math.sin(angleRad);

		int x2 = x + (int)Math.round(cos*length);
		int z2 = z + (int)Math.round(sin*length);

		int x3 = x + (int)Math.round(cos*length2 - sin*(width/2));
		int z3 = z + (int)Math.round(sin*length2 + cos*(width/2));

		int x4 = x + (int)Math.round(cos*length2 - sin*(-width/2));
		int z4 = z + (int)Math.round(sin*length2 + cos*(-width/2));

		g.drawLine(x, z, x2, z2);
		g.drawLine(x, z, x3, z3);
		g.drawLine(x, z, x4, z4);
	}

	public void drawMomentSymbol(Graphics2D g, int x, int z, boolean positive) {
		int r = 16;
		int a = 5;
		if (positive) {
			g.drawArc(x-r/2, z-r/2, r, r, 180, 270);
			g.drawLine(x, z-r/2, x+r/8, z-r/2+a);
			g.drawLine(x, z-r/2, x+a, z-r/2-r/8);
		}
		else {
			g.drawArc(x-r/2, z-r/2, r, r, 90, 270);
			g.drawLine(x, z-r/2, x-r/8, z-r/2+a);
			g.drawLine(x, z-r/2, x-a, z-r/2-r/8);
		}
	}

	public void drawInnerForcesScale(Graphics2D g) {
		float scale = 1;

		if (viewMode == ViewMode.EDITOR) return;
		else if (viewMode == ViewMode.NORMAL_FORCE) {
			scale = 0.1f*0.2f*model.innerForcesScale/100f*model.scale;
		}
		else if (viewMode == ViewMode.SHEAR_FORCE) {
			scale = 0.1f*model.innerForcesScale/100f*model.scale;
		}
		else if (viewMode == ViewMode.BENDING_MOMENT) {
			scale = 0.1f*model.innerForcesScale/100f*model.scale;
		}

		/*int size = 50;		// velikost veličiny
		int step = 0;		// krok ve zvyšování
		float pixels = 0;	// počet pixelů odpovídající dané velikosti

		while (true) {
			pixels = size * scale;
			if (pixels >= 99.5f && pixels <= 100.45f) break;

			if (step == 0) size = 2*size;
			else if (step == 1) size = 5*size / 2;
			else if (step == 2) size = 2*size;

			if (++step == 3) step = 0;
		}

		int iPixels = (int)Math.round(pixels);*/

		int pixels = 100;
		float size = pixels / scale;

		g.setColor(CL_INNER_FORCES_SCALE);
		/*g.fillRect(getWidth() - pixels - 10, 60, pixels, 3);
		g.drawLine(getWidth() - pixels - 10, 58, getWidth() - pixels - 10, 64);
		g.drawLine(getWidth() - 10, 58, getWidth() - 10, 64);*/

		String sizeStr = String.format(Locale.ENGLISH, "%d px ~ %.0f", pixels, size);
		if (viewMode == ViewMode.BENDING_MOMENT) sizeStr += " kNm";
		else sizeStr += " kN";

		g.drawString(sizeStr, getWidth() - 10 - g.getFontMetrics().stringWidth(sizeStr), 65);
	}

	/** Nakreslí symbol podpory. */
	public void drawSupportSymbol(Graphics2D g, int x, int z, float angle, SupportType type) {
		angle += 180;
		double angleRad = angle / 180 * Math.PI;

		float cos = (float)Math.cos(angleRad);
		float sin = (float)Math.sin(angleRad);

		if (type == SupportType.ROLLER) {
			float height = 12;
			float height2 = 15;
			float width = 16;

			int x2 = x + (int)Math.round(cos*height - sin*(width/2));
			int z2 = z + (int)Math.round(sin*height + cos*(width/2));

			int x3 = x + (int)Math.round(cos*height - sin*(-width/2));
			int z3 = z + (int)Math.round(sin*height + cos*(-width/2));

			int x4 = x + (int)Math.round(cos*height2 - sin*(width/2));
			int z4 = z + (int)Math.round(sin*height2 + cos*(width/2));

			int x5 = x + (int)Math.round(cos*height2 - sin*(-width/2));
			int z5 = z + (int)Math.round(sin*height2 + cos*(-width/2));

			g.drawLine(x, z, x2, z2);
			g.drawLine(x, z, x3, z3);
			g.drawLine(x2, z2, x3, z3);
			g.drawLine(x4, z4, x5, z5);
		}
		else if (type == SupportType.PINNED) {
			float height = 15;
			float width = 20;

			int x2 = x + (int)Math.round(cos*height - sin*(width/2));
			int z2 = z + (int)Math.round(sin*height + cos*(width/2));

			int x3 = x + (int)Math.round(cos*height - sin*(-width/2));
			int z3 = z + (int)Math.round(sin*height + cos*(-width/2));

			g.drawLine(x, z, x2, z2);
			g.drawLine(x, z, x3, z3);
			g.drawLine(x2, z2, x3, z3);
		}
		else if (type == SupportType.FIXED) {
			int size = 12;
			g.drawRect(x-size/2, z-size/2, size, size);
			g.drawLine(x-size/2, z-size/2, x+size/2, z+size/2);
			g.drawLine(x-size/2, z+size/2, x+size/2, z-size/2);
		}
		else if (type == SupportType.ROD) {

		}
	}

	/** Vykreslí pozadí. */
	public void drawBackground(Graphics2D g) {
		g.setColor(CL_BACKGROUND);
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	/** Vykreslí mřížku. */
	public void drawGrid(Graphics2D g) {
		int width = getWidth();
		int height = getHeight();

		// mřížka
		g.setColor(CL_GRID);
		for (int x = originX % GRID_SPACING; x < width; x += GRID_SPACING) {
			for (int y = originZ % GRID_SPACING; y < height; y += GRID_SPACING) {
				g.drawLine(x, y, x, y);
			}
		}

		// hlavní mřížka
		g.setColor(CL_MAIN_GRID);
		for (int x = originX % MAIN_GRID_SPACING; x < width; x += MAIN_GRID_SPACING) {
			g.drawLine(x, 0, x, height-1);
		}
		for (int y = originZ % MAIN_GRID_SPACING; y < height; y += MAIN_GRID_SPACING) {
			g.drawLine(0, y, width-1, y);
		}
	}

	/** Vykreslí osy. */
	private void drawAxes(Graphics2D g) {
		g.setColor(CL_AXIS_X);
		g.drawLine(originX, originZ, originX + AXIS_LENGTH, originZ);
		g.drawLine(originX + AXIS_LENGTH - 5, originZ - 5, originX + AXIS_LENGTH, originZ);
		g.drawLine(originX + AXIS_LENGTH - 5, originZ + 5, originX + AXIS_LENGTH, originZ);
		g.setColor(CL_AXIS_Z);
		g.drawLine(originX, originZ, originX, originZ + AXIS_LENGTH);
		g.drawLine(originX- 5, originZ + AXIS_LENGTH  - 5, originX, originZ + AXIS_LENGTH);
		g.drawLine(originX + 5, originZ + AXIS_LENGTH  - 5, originX, originZ + AXIS_LENGTH);
	}

	/** Vykreslí stavový řádek. */
	private void drawStatus(Graphics2D g) {
		g.setColor(CL_TEXT);
		if (status != null) {
			g.drawString(status, 12, getHeight() - 10);
		}
		else {
			int z = getHeight() - 10;
			for (ModelEntity so: activeObjects) {
				g.drawString(so.toLongString(), 12, z);
				z -= 16;
			}
		}
	}

	/** Vykreslí reakce. */
	private void drawReactions(Graphics2D g) {
		g.setColor(CL_TEXT);
		g.setFont(FNT_TEXT);
		if (model.reactionsString != null) {
			String[] lines = model.reactionsString.split("\n");
			int x = 0, y = 0;
			for (String line: lines) {
				g.drawString(line, 12 + x, 180 + y);
				y += 16;
				if (y > getHeight() - 250) {
					y = 0;
					x += 160;
				}
			}
		}
	}

	/** Vykreslí bod pod myší. */
	public void drawMousePosition(Graphics2D g) {
		g.setColor(CL_TEXT);
		g.setFont(FNT_TEXT);
		String coords = String.format(Locale.ENGLISH, "[%.3f; %.3f]", c2mx(gridX(mouseX)), c2mz(gridZ(mouseZ)));
		g.drawString(coords, getWidth() - g.getFontMetrics().stringWidth(coords) - 10, getHeight() - 10);
	}

	/** Vykreslí zadaný úhel. */
	public void drawAngle(Graphics2D g, int angle) {
		g.setColor(CL_TEXT);
		g.setFont(FNT_TEXT);
		VectorXZ dir = getDirection(angle);
		String angleStr = String.format(Locale.ENGLISH, "[%d°; (%.2f; %.2f)]", angle, dir.x, dir.z);
		g.drawString(angleStr, getWidth() - g.getFontMetrics().stringWidth(angleStr) - 10, getHeight() - 10);
	}


	/** Projde zadanou kolekci a vykreslí prvky v ní. */
	private void drawCollection(Graphics2D g, Object collection, Class cls) {
		ModelEntityAdapter adapter = modelAdapters.get(cls);
		if (adapter == null) return;

		if (collection instanceof Map) {
			Map map = (Map)collection;
			for (Object key: map.keySet()) {
				Object obj = map.get(key);
				ModelEntity en = (ModelEntity)obj;
				if (!isObjectActive(en)) adapter.drawEntity(g, en, false);
			}
		}
		else if (collection instanceof java.util.List) {
			java.util.List list = (java.util.List)collection;
			for (Object obj: list) {
				ModelEntity en = (ModelEntity)obj;
				if (!isObjectActive(en)) adapter.drawEntity(g, en, false);
			}
		}

		for (ModelEntity en: activeObjects) {
			if (cls.isInstance(en)) adapter.drawEntity(g, en, true);
		}
	}

	/** Vykreslí model. */
	private void drawModel(Graphics2D g) {
		if (isolatedBeam != null) {
			beamAdapter.drawEntity(g, isolatedBeam, isObjectActive(isolatedBeam));
		}
		else {
			drawCollection(g, model.beams, Beam.class);
			drawCollection(g, model.joints, Joint.class);
			drawCollection(g, model.forces, Force.Action.class);
			drawCollection(g, model.loads, Load.class);
			drawCollection(g, model.moments, Moment.Action.class);
			drawCollection(g, model.supports, Support.class);
		}
	}

	/** Kreslení přidávaných objektů. */
	private void drawNewObjects(Graphics2D g) {
		if (drawingState != null) {
			ModelEntityAdapter adapter = modelAdapters.get(drawingState);
			adapter.drawNewEntity(g);
		}
	}

	@Override
	public Dimension getPreferredSize() { return new Dimension(800, 600); }

	/** Zahájí kreslení nosníků. */
	private void startDrawingBeam() {
		cancelDrawing(false);
		drawingState = Beam.class;
		status = "Kreslete nosníky. [ctrl] - zalomený nosník. [right click] - zrušit.";
		btnBeam.setActive(true);
		newBeam = new Beam();
		repaint();
	}

	/** Zahájí kreslení síly. */
	private void startDrawingForce() {
		cancelDrawing(false);
		drawingState = Force.Action.class;
		status = "Nakreslete sílu a zadejte velikost. [right click] - zrušit.";
		btnForce.setActive(true);
		repaint();
	}

	/** Zahájí kreslení momentu. */
	private void startDrawingMoment() {
		cancelDrawing(false);
		drawingState = Moment.Action.class;
		status = "Nakreslete moment a zadejte velikost. [right click] - zrušit.";
		btnMoment.setActive(true);
		repaint();
	}

	/** Zahájí kreslení spojitého zatížení. */
	private void startDrawingLoad() {
		cancelDrawing(false);
		drawingState = Load.class;
		status = "Nakreslete spojité zatížení a zadejte velikost. [right click] - zrušit.";
		btnLoad.setActive(true);
		repaint();
	}

	/** Zahájí kreslení podpory. */
	private void startDrawingSupport() {
		cancelDrawing(false);
		drawingState = Support.class;

		// Výběr typu podpory
		String ROLLER = "Posuvný kloub";
		String PINNED = "Pevný kloub";
		String FIXED = "Vetknutí";
		String ROD = "Vnější táhlo";
		String[] types = { ROLLER, PINNED, FIXED, ROD };
		Toolkit.getDefaultToolkit().beep();
		String selectedType = (String)JOptionPane.showInputDialog(getCanvas(), "Vyberte typ podpory: ", TITLE, JOptionPane.QUESTION_MESSAGE, null, types, ROLLER);
		if (selectedType == null) {
			cancelDrawing(false);
			return;
		}
		else {
			if (selectedType == ROLLER) newSupportType = SupportType.ROLLER;
			else if (selectedType == PINNED) newSupportType = SupportType.PINNED;
			else if (selectedType == ROD) newSupportType = SupportType.ROD;
			else newSupportType = SupportType.FIXED;
		}

		status = "Nakreslete podporu. [right click] - zrušit.";
		btnSupport.setActive(true);
		repaint();
	}

	/** Zruší kreslení. */
	public void cancelDrawing(boolean repaint) {
		isolatedBeam = null;
		btnBeam.setActive(false);
		btnForce.setActive(false);
		btnMoment.setActive(false);
		btnLoad.setActive(false);
		btnSupport.setActive(false);
		status = null;
		drawingState = null;
		newBeam = null;
		newObjA = null;
		newObjB = null;
		if (repaint) repaint();
	}

	private boolean isPointUnderMouse(float x, float y, int rectSize) {
		Rectangle2D.Float mouse = new Rectangle2D.Float(mouseX - rectSize/2, mouseZ - rectSize/2, rectSize, rectSize);
		return mouse.contains(x, y);
	}
	private boolean isPointUnderMouse(int x, int y) {
		return isPointUnderMouse(x, y, 7);
	}

	private boolean isLineUnderMouse(float x1, float y1, float x2, float y2) {
		Line2D.Float line = new Line2D.Float(x1, y1, x2, y2);
		Rectangle2D.Float mouse = new Rectangle2D.Float(mouseX - 2, mouseZ - 2, 5, 5);
		return line.intersects(mouse);
	}

	/** Najde objekt pod myší. */
	public void findActiveObjects() {
		activeObjects.clear();

		if (isolatedBeam != null) {
			Beam beam = isolatedBeam;
			for (int i = 0; i < beam.mainPoints.size(); i++) {
				int x1, z1, x2, z2;
				if (i == 0) {
					if (!beam.closed) continue;
					else {
						x1 = m2cx(beam.mainPoints.get(beam.mainPoints.size()-1).x);
						z1 = m2cz(beam.mainPoints.get(beam.mainPoints.size()-1).z);
					}
				}
				else {
					x1 = m2cx(beam.mainPoints.get(i-1).x);
					z1 = m2cz(beam.mainPoints.get(i-1).z);
				}
				x2 = m2cx(beam.mainPoints.get(i).x);
				z2 = m2cz(beam.mainPoints.get(i).z);

				if (isLineUnderMouse(x1, z1, x2, z2)) {
					activeObjects.add(beam);
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					return;
				}
			}
			setCursor(Cursor.getDefaultCursor());
			return;
		}

		boolean activeJoint = false;
		boolean activeSupport = false;

		// Projdeme podpory
		for (Support support: model.supports) {
			int x = m2cx(support.origin.x);
			int z = m2cz(support.origin.z);
			if (support instanceof Support.Fixed) {
				if (isPointUnderMouse(x, z, 11)) {
					activeObjects.add(support);
					activeSupport = true;
				}
			}
			else if(support instanceof Support.Rod) {
				int x1 = m2cx(support.origin.x);
				int z1 = m2cz(support.origin.z);
				VectorXZ end = ((Support.Rod)support).getEnd();
				int x2 = m2cx(end.x);
				int z2 = m2cz(end.z);
				if (isLineUnderMouse(x1, z1, x2, z2)) {
					activeObjects.add(support);
					activeSupport = true;
				}
			}
			else {
				int x2 = m2cx(support.origin.x - SUPPORT_VIRTUAL_LENGTH*support.direction.x/model.scale);
				int z2 = m2cz(support.origin.z - SUPPORT_VIRTUAL_LENGTH*support.direction.z/model.scale);
				if (isLineUnderMouse(x, z, x2, z2)) {
					activeObjects.add(support);
					activeSupport = true;
				}
			}
		}

		// Projdeme klouby
		for (VectorXZ jointPoint: model.joints.keySet()) {
			Joint joint = model.joints.get(jointPoint);
			int x = m2cx(joint.position.x);
			int z = m2cz(joint.position.z);
			if (isPointUnderMouse(x, z)) {
				activeObjects.add(joint);
				activeJoint = true;
				break;
			}
		}

		// Projdeme nosníky
		if (!activeJoint && !activeSupport)
		beamLoop: for (Beam beam: model.beams) {
			for (int i = 0; i < beam.mainPoints.size(); i++) {
				int x1, z1, x2, z2;
				if (i == 0) {
					if (!beam.closed) continue;
					else {
						x1 = m2cx(beam.mainPoints.get(beam.mainPoints.size()-1).x);
						z1 = m2cz(beam.mainPoints.get(beam.mainPoints.size()-1).z);
					}
				}
				else {
					x1 = m2cx(beam.mainPoints.get(i-1).x);
					z1 = m2cz(beam.mainPoints.get(i-1).z);
				}
				x2 = m2cx(beam.mainPoints.get(i).x);
				z2 = m2cz(beam.mainPoints.get(i).z);

				if (isLineUnderMouse(x1, z1, x2, z2)) {
					activeObjects.add(beam);
					continue beamLoop;
				}
			}
		}

		// Projdeme síly
		if (!activeJoint && !activeSupport)
		for (Force.Action force: model.forces) {
			int x = m2cx(force.origin.x);
			int z = m2cz(force.origin.z);
			int x2 = m2cx(force.origin.x - ARROW_LENGTH*force.direction.x/model.scale);
			int z2 = m2cz(force.origin.z - ARROW_LENGTH*force.direction.z/model.scale);
			if (isLineUnderMouse(x, z, x2, z2)) {
				activeObjects.add(force);
			}
		}

		// Projdeme momenty
		if (!activeJoint && !activeSupport)
		for (Moment.Action moment: model.moments) {
			int x = m2cx(moment.origin.x);
			int z = m2cz(moment.origin.z);
			if (isPointUnderMouse(x, z, 16)) {
				activeObjects.add(moment);
			}
		}

		// Projdeme spojitá zatížení
		// @todo Vylepšit výběr spojitého zatížení (z čáry na obdélník)
		if (!activeJoint && !activeSupport)
		for (Load load: model.loads) {
			int x1 = m2cx(load.start.x);
			int z1 = m2cz(load.start.z);
			int x2 = m2cx(load.end.x);
			int z2 = m2cz(load.end.z);
			if (isLineUnderMouse(x1, z1, x2, z2)) {
				activeObjects.add(load);
			}
		}


		if (activeObjects.size() > 0) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/** Zjistí, zda je objekt aktivní. */
	private boolean isObjectActive(ModelEntity o) {
		return activeObjects.contains(o);
	}


	/** Naplní vyskakovací menu nabídkou příslušící k danému objektu. */
	private void fillPopupMenu(JMenu menu, final ModelEntity en) {
		ModelEntityAdapter adapter = modelAdapters.get(en.getClass());
		adapter.fillPopupMenu(menu, en);
	}

	/** Vytvoří a aktivuje vyskakovací menu. */
	private void popupMenu() {
		popupMenu.removeAll();
		if (activeObjects.size() > 0) {
			for (ModelEntity active: activeObjects) {
				JMenu menu = new JMenu(active.toString());
				fillPopupMenu(menu, active);
				popupMenu.add(menu);
			}

			popupMenu.addSeparator();
		}

		popupMenu.add(menuModel);
		popupMenu.add(menuViewMode);
		popupMenu.addSeparator();
		if (isolatedBeam != null) popupMenu.add(menuShowAll);
		popupMenu.add(menuScale);
		popupMenu.addSeparator();
		popupMenu.add(menuInfo);
		//popupMenu.addSeparator();
		popupMenu.add(menuAbout);

		popupMenu.show(this, mouseX, mouseZ);
	}
	

	public void mouseClicked(MouseEvent e) {}

	public void mouseDragged(MouseEvent e) {
		if (mouseMiddlePressed) {
			mouseX = e.getX();
			mouseZ = e.getY();

			int dx = mousePressedX - mouseX;
			int dy = mousePressedZ - mouseZ;

			mousePressedX = mouseX;
			mousePressedZ = mouseZ;

			originX -= dx;
			originZ -= dy;
			repaint();
		}
		else if (draggedJoint != null) {
			mouseX = e.getX();
			mouseZ = e.getY();

			VectorXZ newPosition = new VectorXZ(c2mx(gridX(mouseX)), c2mz(gridZ(mouseZ)));
			if (!newPosition.equals(draggedJoint.position)) {

				// projdeme všechny nosníky a změníme jim topologii...
				for (Beam beam: draggedJoint.beams) {
					for (int i = 0; i < beam.mainPoints.size(); i++) {
						if (beam.mainPoints.get(i).equals(draggedJoint.position)) {
							beam.mainPoints.set(i, newPosition);
							break;
						}
					}
				}

				if (model.stiffJoints.contains(draggedJoint.position)) {
					model.stiffJoints.remove(draggedJoint.position);
					model.stiffJoints.add(newPosition);
				}

				draggedJoint.position = newPosition;

				model.recalculate();
				//findActiveObjects();
				repaint();
			}
		}
	}

	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseZ = e.getY();

		// výběr - vyhledání prvku pod myší
		if (drawingState == null) {
			findActiveObjects();
		}

		repaint();
	}

	public void mousePressed(MouseEvent e) {
		mouseX = e.getX();
		mouseZ = e.getY();

		// Obsloužení pravého tlačítka
		if (e.getButton() == MouseEvent.BUTTON3) {
			if (drawingState == null) {
				popupMenu();
			}
			else {
				cancelDrawing(true);
				return;
			}
		}

		// Obsloužení prostředního tlačítka, resp. začátku přesouvání
		else if (e.getButton() == MouseEvent.BUTTON2) {
			mousePressedX = mouseX;
			mousePressedZ = mouseZ;
			mouseMiddlePressed = true;
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		else if (e.getButton() == MouseEvent.BUTTON1 && drawingState == null) {
			// pokud je vybraný kloub, posuneme ho i se všemi nosníky
			for (ModelEntity en: activeObjects) {
				if (en instanceof Joint) {
					draggedJoint = (Joint)en;
				}
			}
		}

		// Obsloužení kreslení
		else if(drawingState != null) {
			ModelEntityAdapter adapter = modelAdapters.get(drawingState);
			adapter.handleNewEntityMousePressed(e);
		}

		repaint();
	}

	/** Vrátí bod, na který uživatel kliknul myší. */
	public VectorXZ getPointAtMouse() {
		return new VectorXZ(c2mx(gridX(mouseX)), c2mz(gridZ(mouseZ)));
	}


	/** Vrátí úhel od zadaného bodu. */
	public int getAngle(VectorXZ point) {
		VectorXZ mouse = getPointAtMouse();
		int angle;
		if (mouse.z == point.z && mouse.x == point.x) angle = 90;
		else angle = (int) Math.round(Math.atan2(mouse.z - point.z, mouse.x - point.x) * 180 / Math.PI);
		if (angle < 0) angle += 360;
		if (showGrid) angle -= angle % 15;
		return angle;
	}

	/** Vrátí na základě úhlu jednotkový vektor směru. */
	public VectorXZ getDirection(int angle) {
		double angleRad = angle * Math.PI / 180;
		return new VectorXZ((float)Math.cos(angleRad), (float)Math.sin(angleRad));
	}

	public void mouseReleased(MouseEvent e) {
		mouseMiddlePressed = false;
		if (draggedJoint != null) {
			
			draggedJoint = null;
		}
		setCursor(Cursor.getDefaultCursor());
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			cancelDrawing(true);
		}
	}

	public void keyReleased(KeyEvent e) {}

	
	private int oldWidth = -1, oldHeight = -1;

	public void componentResized(ComponentEvent e) {
		if (oldWidth == -1) {
			//this.originX = getWidth() / 2;
			//this.originZ = getHeight() / 2;
		}
		else {
			this.originX = (int)(this.originX * ((float)getWidth() / oldWidth));
			this.originZ = (int)(this.originZ * ((float)getHeight() / oldHeight));
		}

		this.oldWidth = getWidth();
		this.oldHeight = getHeight();

		btnEditor.setLocation(getWidth() - 220, 10);
		btnNormal.setLocation(getWidth() - 165, 10);
		btnShear.setLocation(getWidth() - 110, 10);
		btnBendMoment.setLocation(getWidth() - 55, 10);
	}

	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}


	public void fitView() {
		int width = oldWidth == -1 ? getPreferredSize().width : oldWidth;
		int height = oldHeight == -1 ? getPreferredSize().height : oldHeight;

		originX = width / 2;
		originZ = height / 2 + 50;

		float[] bounds = model.getModelBounds();
		int xMin = m2cx(bounds[0]); int xMax = m2cx(bounds[2]);
		int zMin = m2cz(bounds[1]); int zMax = m2cz(bounds[3]);

		originX += originX - (xMin + xMax) / 2;
		originZ += originZ - (zMin + zMax) / 2;
	}

}
