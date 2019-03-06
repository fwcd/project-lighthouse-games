package lighthouse.ui.scene.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lighthouse.ui.scene.input.SceneKeyInput;
import lighthouse.ui.scene.input.SceneMouseInput;
import lighthouse.ui.scene.viewmodel.graphics.Graphics2DSceneRenderer;
import lighthouse.ui.scene.viewmodel.graphics.SceneLayer;
import lighthouse.ui.scene.viewmodel.graphics.SceneShapeVisitor;
import lighthouse.ui.scene.viewmodel.graphics.SceneViewModel;
import lighthouse.util.DoubleVec;
import lighthouse.util.IntVec;

public class LocalSceneView implements SceneView {
	private static final Logger LOG = LoggerFactory.getLogger(LocalSceneView.class);
	private final JComponent component;
	private SceneViewModel scene;
	
	private final Color background = Color.WHITE;
	private final Color gridLineColor = Color.LIGHT_GRAY;
	private final int gridDashLength = 3;
	private final int gridLineThickness = 1;
	private Function<DoubleVec, IntVec> gridPosToPixels;
	private Function<DoubleVec, IntVec> gridSizeToPixels;
	
	public LocalSceneView(Function<DoubleVec, IntVec> gridToPixels) {
		this(gridToPixels, gridToPixels);
	}
	
	public LocalSceneView(Function<DoubleVec, IntVec> gridPosToPixels, Function<DoubleVec, IntVec> gridSizeToPixels) {
		this.gridPosToPixels = gridPosToPixels;
		this.gridSizeToPixels = gridSizeToPixels;
		
		component = new JPanel() {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				render((Graphics2D) g, getSize());
			}
		};
		component.setBackground(background);
	}
	
	public void addKeyInput(SceneKeyInput keyInput) {
		LOG.debug("Added key input");
		
		InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = component.getActionMap();
		
		for (int keyCode : keyInput.getBoundKeys()) {
			inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), keyCode);
			actionMap.put(keyCode, new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					component.requestFocus();
					keyInput.keyPressed(keyCode);
				}
			});
		}
	}
	
	public void addMouseInput(SceneMouseInput mouseInput) {
		LOG.debug("Added mouse input");
		component.addMouseListener(mouseInput);
		component.addMouseMotionListener(mouseInput);
	}
	
	public void removeMouseInput(SceneMouseInput mouseInput) {
		LOG.debug("Removed mouse input");
		component.removeMouseListener(mouseInput);
		component.removeMouseMotionListener(mouseInput);
	}
	
	public void setGridPosToPixels(Function<DoubleVec, IntVec> gridPosToPixels) {
		this.gridPosToPixels = gridPosToPixels;
	}
	
	public void setGridSizeToPixels(Function<DoubleVec, IntVec> gridSizeToPixels) {
		this.gridSizeToPixels = gridSizeToPixels;
	}
	
	@Override
	public void draw(SceneViewModel scene) {
		this.scene = scene;
		SwingUtilities.invokeLater(component::repaint);
	}
	
	public void relayout(IntVec gridSize) {
		IntVec mapped = gridSizeToPixels.apply(gridSize.toDouble());
		component.setPreferredSize(new Dimension(mapped.getX(), mapped.getY()));
	}
	
	private boolean shouldDrawGrid() {
		for (SceneLayer layer : scene) {
			if (layer.requiresGridBackground()) {
				return true;
			}
		}
		return false;
	}
	
	private void render(Graphics2D g2d, Dimension canvasSize) {
		if (scene == null) {
			g2d.setFont(g2d.getFont().deriveFont(18F));
			g2d.drawString("No scene model drawn", 30, 30);
		} else {
			int canvasWidth = (int) canvasSize.getWidth();
			int canvasHeight = (int) canvasSize.getHeight();
			IntVec cellSize = gridSizeToPixels.apply(DoubleVec.ONE_ONE);
			
			if (shouldDrawGrid()) {
				float[] dash = {gridDashLength};
				g2d.setColor(gridLineColor);
				g2d.setStroke(new BasicStroke(gridLineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
				
				for (int y = 0; y < canvasHeight; y += cellSize.getY()) {
					g2d.drawLine(0, y, canvasWidth, y);
				}
				
				for (int x = 0; x < canvasWidth; x += cellSize.getX()) {
					g2d.drawLine(x, 0, x, canvasHeight);
				}
			}
			
			SceneShapeVisitor renderer = new Graphics2DSceneRenderer(g2d, gridPosToPixels, gridSizeToPixels);
			scene.acceptForAllLayers(renderer);
		}
	}
	
	public JComponent getComponent() { return component; }
}
