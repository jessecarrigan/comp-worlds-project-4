package gui;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import model.Field;

public class GameofLife {
	// Physics updates per second (approximate).
	private static final int UPDATE_RATE = 30;
	// Number of full iterations of the collision detection and resolution system.
	private static final int COLLISION_ITERATIONS = 4;
	// Width of the extent in meters.
	private static final float EXTENT_WIDTH = 16;
	
	private Field slices;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GameofLife().createAndShowGUI();
			}
		});
	}

	public GameofLife() {

	}

	private void createAndShowGUI() {
		// Fix for background flickering on some platforms
		System.setProperty("sun.awt.noerasebackground", "true");

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		final Canvas3D canvas3D = new Canvas3D(config);
		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.getViewer().getView().setSceneAntialiasingEnable(true);

		// Add a scaling transform that resizes the virtual world to fit
		// within the standard view frustum.
		BranchGroup trueScene = new BranchGroup();
		TransformGroup worldScaleTG = new TransformGroup();
		Transform3D t3D = new Transform3D();
		t3D.setScale(.9 / EXTENT_WIDTH);
		worldScaleTG.setTransform(t3D);
		trueScene.addChild(worldScaleTG);
		BranchGroup scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		worldScaleTG.addChild(scene);
		
		final TransformGroup extentTransform = new TransformGroup();
		extentTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		extentTransform.addChild(createExtent());
		scene.addChild(extentTransform);
		
		slices = new Field();
		// Draw the slices for this field
		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					if (slices.getCell(x, y, z) > 0) {
						TransformGroup cubeGroup = new TransformGroup();
						ColorCube cube = new ColorCube(0.5f);
						cubeGroup.addChild(cube);
						Transform3D cubeTransform = new Transform3D();
						cubeTransform.setTranslation(new Vector3d(x, y, z));
						cubeGroup.setTransform(cubeTransform);
						scene.addChild(cubeGroup);
					}
				}
			}
		}
		simpleU.addBranchGraph(trueScene);

		JFrame appFrame = new JFrame("Physics Demo");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		appFrame.add(canvas3D);
        canvas3D.setPreferredSize(new Dimension(800,600));
		appFrame.pack();
        appFrame.setLocationRelativeTo(null);
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
//			appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		canvas3D.addMouseMotionListener(new MouseMotionAdapter() {
			private MouseEvent lastDragEvent;

			public void mouseDragged(MouseEvent e) {
				if (lastDragEvent != null) {
					Vector2f lastMouseVector = new Vector2f(lastDragEvent.getX() - canvas3D.getWidth() / 2, lastDragEvent.getY() - canvas3D.getHeight() / 2);
					Vector2f currentMouseVector = new Vector2f(e.getX() - canvas3D.getWidth() / 2, e.getY() - canvas3D.getHeight() / 2);
					Vector2f deltaVector = new Vector2f();
					deltaVector.scaleAdd(-1, lastMouseVector, currentMouseVector);
					float rotationAngle = -Math.signum(lastMouseVector.x * deltaVector.y - lastMouseVector.y * deltaVector.x) * lastMouseVector.angle(currentMouseVector);
					Transform3D rotationTransform = new Transform3D();
					rotationTransform.rotZ(rotationAngle);
					// Rotate the extent
					Transform3D extT3D = new Transform3D();
					extentTransform.getTransform(extT3D);
					extT3D.mul(rotationTransform, extT3D);
					extentTransform.setTransform(extT3D);
					// Rotate each boundary
					Vector3f tmp = new Vector3f();
//					for (HalfSpace hs : boundaries) {
//						// Only normals are used at the moment, so only rotate normals.
//						tmp.x = hs.normal.x;
//						tmp.y = hs.normal.y;
//						rotationTransform.transform(tmp);
//						hs.normal.x = tmp.x;
//						hs.normal.y = tmp.y;
//					}
				}
				lastDragEvent = e;
			}

			public void mouseMoved(MouseEvent e) {
				lastDragEvent = null;
			}});
		new Timer(1000 / UPDATE_RATE, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas3D.stopRenderer();
				tick();
				canvas3D.startRenderer();
			}
		}).start();
		
		appFrame.setVisible(true);
	}
	
	private void tick() {
//		for (PhysicsObject o : objects) {
//			// Hard-coded gravity
//			o.forceAccumulator.y = -10 * o.mass;
//			o.updateState(1f / UPDATE_RATE);
//		}
//		for (int i = 0; i < COLLISION_ITERATIONS; i++)
//			for (PhysicsObject o : objects) {
//				for (HalfSpace hs : boundaries)
//					CollisionHandler.checkAndResolveCollision(hs, o);
//				for (PhysicsObject o2 : objects)
//					CollisionHandler.checkAndResolveCollision(o2, o);
//			}
//		for (PhysicsObject o : objects) {
//			o.updateTransformGroup();
//			// Clear the object's force accumulator.
//			o.forceAccumulator.x = o.forceAccumulator.y = 0;
//		}
	}

	private static Node createExtent() {	
        Appearance app = new Appearance();
        PolygonAttributes polyAttribs = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0);
        app.setPolygonAttributes(polyAttribs);
        app.setColoringAttributes(new ColoringAttributes(new Color3f(1.0f, 1.0f, 1.0f), ColoringAttributes.FASTEST));
        float dim = EXTENT_WIDTH / 2  - 0.02f;
		Box extent = new Box(dim, dim, dim, app);
        extent.setPickable(true);
		return extent;
	}
}
