package gui;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import model.Field;
import chord.Peer;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class GameofLife {
	
	// Time between generations in milliseconds.
	private static int PAUSE_RATE = 500;
	
	// Width of the extent in meters.
	private static float EXTENT_WIDTH = 16;
	
	// Starting distance of the camera from the game world.
	private static final double DISTANCE = 3d;
	
	// The model for the game.
	private Field slices;
	
	// Map of nodes to display so we don't have to keep recreating them.
	private Map<Vector3f, BranchGroup> cellMap;
	
	// This instance's peer object.
	private Peer p2p;
	
	// UI frame.
	private JFrame appFrame;

	// Main scene object.
	private BranchGroup scene;

	private SimpleUniverse simpleU;

	/**
	 * Main method.
	 * @param String args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GameofLife().createAndShowGUI();
			}
		});
	}

	private void tick() {
//		for (int z = 0; z < EXTENT_WIDTH; z++) {
//			slices = slices.updateToCopy();
//		}
		// Draw the slices for this field
		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					Vector3f key = new Vector3f(x, y, z);
					BranchGroup cellGroup = cellMap.get(key);
					if (slices.getCell(x, y, z) > 0) {
						// If the cell should be alive and is not already alive, cell is born
						if (scene.indexOfChild(cellGroup) == -1) {
							scene.addChild(cellGroup);
						}
					} else {
						// Otherwise, cell dies
						cellGroup.detach();
					}
				}
			}
		}
	}

	/*
	 * Launches the UI for the game. 
	 */
	private void createAndShowGUI() {
		// Fix for background flickering on some platforms
		System.setProperty("sun.awt.noerasebackground", "true");
		
//		final JCanvas3D jCanvas3d = new JCanvas3D();
//		jCanvas3d.setPreferredSize(new Dimension(800,600));
//		jCanvas3d.setSize(new Dimension(800,600));
//		final Canvas3D canvas3D = jCanvas3d.getOffscreenCanvas3D();
		
		// Add a scaling transform that resizes the virtual world to fit
		// within the standard view frustum.
		BranchGroup trueScene = new BranchGroup();
		TransformGroup worldScaleTG = new TransformGroup();
		Transform3D t3D = new Transform3D();
		t3D.setScale(.9 / EXTENT_WIDTH);
		worldScaleTG.setTransform(t3D);
		trueScene.addChild(worldScaleTG);
		scene = new BranchGroup();
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_DETACH);
		worldScaleTG.addChild(scene);
		
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		final Canvas3D canvas3D = new Canvas3D(config);
		simpleU = new SimpleUniverse(canvas3D);
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.getViewer().getView().setSceneAntialiasingEnable(true);
		simpleU.addBranchGraph(trueScene);
		
		// View movement
		Point3d focus = new Point3d();
        Point3d camera = new Point3d(1,1,1);
        Vector3d up = new Vector3d(0,1,0);
        TransformGroup lightTransform = new TransformGroup();
        TransformGroup curTransform = new TransformGroup();
        FlyCam fc = new FlyCam(simpleU.getViewingPlatform().getViewPlatformTransform(),focus,camera,up,DISTANCE, lightTransform, curTransform);
        fc.setSchedulingBounds(new BoundingSphere(new Point3d(),1000.0));
        BranchGroup fcGroup = new BranchGroup();
        fcGroup.addChild(fc);
        scene.addChild(fcGroup);
		        
        // Map of cell objects
        cellMap = new HashMap<>();
		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					TransformGroup cubeGroup = new TransformGroup();
					ColorCube cube = new ColorCube(0.3f);
					cubeGroup.addChild(cube);
					Transform3D cubeTransform = new Transform3D();
					
					cubeTransform.setTranslation(new Vector3f(x, y, z));
					cubeGroup.setTransform(cubeTransform);
					// Added branchgroup to deal with exceptions
					BranchGroup bg = new BranchGroup();
					bg.addChild(cubeGroup);
					bg.setCapability(BranchGroup.ALLOW_DETACH);
					cellMap.put(new Vector3f(x, y, z), bg);
				}
			}
		}
		
		slices = new Field();
		p2p = new Peer();

		appFrame = new JFrame("Physics Demo");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        appFrame.add(jCanvas3d);
        appFrame.add(canvas3D);
		canvas3D.setPreferredSize(new Dimension(800,600));
        appFrame.setJMenuBar(buildMenuBar());
        
		appFrame.pack();
        appFrame.setLocationRelativeTo(null);
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
//			appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		new Timer(PAUSE_RATE, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas3D.startRenderer();
				tick();
				canvas3D.startRenderer();
			}
		}).start();
		
		appFrame.setVisible(true);
	}
	
    /** Creates a slider **/
	private final JSlider buildSlider(int min, int max, int value, int spacing) {
		JSlider slider = new JSlider(min, max, value);
		slider.setMinorTickSpacing(spacing);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		return slider;
	}

	/**
	 * Builds the menu bar.
	 * 
	 * @return A JMenuBar.
	 */
	private JMenuBar buildMenuBar() {
		final JMenuBar menuBar = new JMenuBar();
		// Build the menus
		final JMenu chordMenu = buildChordMenu("Chord", KeyEvent.VK_C);
		final JMenu newGameMenu = buildNewGameMenu("New Game", KeyEvent.VK_N);
		// Add the menus to the menu bar
		menuBar.add(chordMenu);
		menuBar.add(newGameMenu);
		
		return menuBar;
	}

	/**
	 * Builds the new game menu.
	 * @param label The label.
	 * @param vkN The key mnemonic.
	 * @return A JMenu.
	 */
	private JMenu buildNewGameMenu(String label, int vkN) {
		JMenu newGameMenu = new JMenu(label);
		newGameMenu.add(new JMenuItem(
				new NewGameAction("Settings...")));
		return newGameMenu;
	}

	/**
	 * Builds the new game menu.
	 * @param label The label.
	 * @param vkN The key mnemonic.
	 * @return A JMenu.
	 */
	private JMenu buildChordMenu(String label, int vkA) {
		JMenu chordMenu = new JMenu(label);
		chordMenu.add(new JMenuItem(
				new PeerConnectAction("Connect...")));
		return chordMenu;
	}
	
	/**
	 * Provides an action to add an icosahedron.
	 */
	@SuppressWarnings("serial")
	private class NewGameAction extends AbstractAction  {
		
		/**
		 * Constructs an action for the menu.
		 * 
		 * @param actionName
		 *            The name to be displayed on the menu.
		 */
		public NewGameAction(final String actionName) {
			super(actionName);
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_N);
		}
		
		/**
		 * Opens a new game dialog.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
			JSlider sizeSlider;
			JSlider timeSlider;
			
			sizeSlider = buildSlider(4, 32, 16, 2);
			timeSlider = buildSlider(100, 1000, PAUSE_RATE, 100);
			final JComponent[] inputs = new JComponent[] {
					new JLabel("World Size: "),
					sizeSlider,
					new JLabel("Time Between Generations (ms): "),
					timeSlider
			};
			int input = JOptionPane.showConfirmDialog(null, inputs, "Create New Game", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION) {
				EXTENT_WIDTH = sizeSlider.getValue();
				PAUSE_RATE = timeSlider.getValue();
				for (int z = 0; z < EXTENT_WIDTH; z++) {
					for (int y = 0; y < EXTENT_WIDTH; y++) {
						for (int x = 0; x < EXTENT_WIDTH; x++) {
							Vector3f key = new Vector3f(x, y, z);
							BranchGroup cellGroup = cellMap.get(key);
								cellGroup.detach();
							}
						}
					}
				slices = new Field();
			}
		}
	}
	/**
	 * Provides an action to add an icosahedron.
	 */
	@SuppressWarnings("serial")
	private class PeerConnectAction extends AbstractAction  {
		
		/**
		 * Constructs an action for the menu.
		 * 
		 * @param actionName
		 *            The name to be displayed on the menu.
		 */
		public PeerConnectAction(final String actionName) {
			super(actionName);
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_O);
		}
		
		/**
		 * Opens a connect dialog.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
			JTextArea ipAddress = new JTextArea();
			JTextArea chordId = new JTextArea();
			
			final JComponent[] inputs = new JComponent[] {
					new JLabel("IP Address: "),
					ipAddress,
					new JLabel("Chord ID: "),
					chordId
			};
			int input = JOptionPane.showConfirmDialog(null, inputs, "Connect to Peer", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION) {
				InetAddress host = null;
				try {
					host = InetAddress.getByName(ipAddress.getText());
				} catch (UnknownHostException e) {
					System.out.println("Unable to reach host " + ipAddress.getText() +
							". Check that the hostname is correct and that the host is available.");
					e.printStackTrace();
				}
				long id = Integer.parseInt(chordId.getText());
				p2p.connectToNetwork(host, id);
			}
		}
	}

//	/** Builds the control panel **/
//	private final JPanel buildControlPanel() {
//		// Basic panel setups
//		JPanel controlPanel = new JPanel();
//		JPanel checkBoxPanel = new JPanel();
//		JPanel sliderPanel = new JPanel();
//		
//		GridLayout radioButtonGrid = new GridLayout(0, 1);
//		GridLayout sliderGrid = new GridLayout(0, 3);
//		checkBoxPanel.setLayout(radioButtonGrid);
//		sliderPanel.setLayout(sliderGrid);
//		
//		controlPanel.add(checkBoxPanel, BorderLayout.EAST);
//		controlPanel.add(sliderPanel, BorderLayout.WEST);
//
//		// Add controls for forces
//		
//		// Add control for force field
//        JCheckBox forceFieldEnable = new JCheckBox();
//        forceFieldEnable.addItemListener(new ItemListener() {
//
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                JCheckBox source = (JCheckBox) e.getSource();
//                if (source.isSelected()) {
//                    setForceFieldEnabled(true);
//                } else {
//                    setForceFieldEnabled(false);
//                }
//            }
//        });
//     
//        forceFieldEnable.setText("Force Field");
//        forceFieldEnable.setSelected(true);
//        checkBoxPanel.add(forceFieldEnable);
//
//
//        for(final ForceBehavior fb : forceBehaviors) {
//        	// Checkboxes for behaviors
//            JCheckBox behaviorEnable = new JCheckBox();
//            behaviorEnable.addChangeListener(new ChangeListener() {
//				
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					JCheckBox source = (JCheckBox) e.getSource();
//					if (source.isSelected()) {
//						addBehavior(fb);
//					} else {
//						removeBehavior(fb);
//					}
//				}
//			});
//         
//            behaviorEnable.setText(fb.getName());
//            behaviorEnable.setSelected(true);
//            checkBoxPanel.add(behaviorEnable);
//            
//            // Sliders for magnitude of forces
//            final float max = fb.getForceMaximum();
//            final float min = fb.getForceMinimum();
//            final float cur = fb.getForceMagnitude();     
//            
//    		final JSlider forceMagnitudeSlider = new JSlider();
//            
//	        sliderPanel.add(new JLabel(fb.getName()));
//			sliderPanel.add(forceMagnitudeSlider);
//			final JLabel forceMagLabel = new JLabel("" + (int) cur);
//			sliderPanel.add(forceMagLabel);
//			
//			forceMagnitudeSlider.setMinorTickSpacing(1);
//            forceMagnitudeSlider.setMaximum((int) (max * 100));
//            forceMagnitudeSlider.setMinimum((int) (min * 100));
//            forceMagnitudeSlider.setValue((int) (cur * 100));
//            forceMagnitudeSlider.addChangeListener(new ChangeListener() {
//            	
//				ForceBehavior associatedBehavior = fb;
//				
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					JSlider slider = (JSlider) e.getSource();
//    				forceMagLabel.setText("" + Math.round(slider.getValue() / 100f));
//                    associatedBehavior.setForceMagnitude(slider.getValue() / 100f);
//                    forceField.resetMaxLength(); //clear the max length so it can adjust quickly
//                    System.out.printf("Slider Changed: Min (%02.2f) Max (%02.2f) Cur (%02.2f)\n", min, max, cur);
//				}
//			});
//            
//        }
//		
//    	JSlider coefficientOfRestitutionSlider = buildSlider(0, 100, (int)(coefficientOfRestitution*100));
//		sliderPanel.add(new JLabel("Coefficient of restitution"));
//		sliderPanel.add(coefficientOfRestitutionSlider);
//		final JLabel coefficientLabel = new JLabel("" + coefficientOfRestitutionSlider.getValue());
//		sliderPanel.add(coefficientLabel);
//		
//		ChangeListener coefficientListener = new ChangeListener() {
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				JSlider source = (JSlider) e.getSource();
//				coefficientOfRestitution = source.getValue()/100f;
//				coefficientLabel.setText("" + source.getValue() + "%");
//                //update the coefficients
//                for(CollisionBehavior cb : collisionBehaviors)
//                    cb.setCoefficientOfRestitution(coefficientOfRestitution);
//			}
//		};
//		coefficientOfRestitutionSlider.addChangeListener(coefficientListener);
//
//		return controlPanel;
//	}
//

}
