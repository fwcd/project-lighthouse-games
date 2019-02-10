package lighthouse.ui.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import lighthouse.model.Grid;

/**
 * A local (Swing-based) view of the Lighthouse grid.
 */
public class LocalGridView implements GridView {
	private final JComponent component;
	private Grid model = null;
	private int cellWidth = 10;
	private int cellHeight = 10;
	
	public LocalGridView() {
		component = new JPanel() {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				render((Graphics2D) g, getSize());
			}
		};
	}
	
	public int getCellWidth() {
		return cellWidth;
	}
	
	public int getCellHeight() {
		return cellHeight;
	}
	
	@Override
	public void draw(Grid model) {
		this.model = model;
		component.repaint();
	}
	
	private void render(Graphics2D g2d, Dimension canvasSize) {
		if (model == null) {
			g2d.setFont(g2d.getFont().deriveFont(18F)); // Make font larger
			g2d.drawString("No Grid model drawn", 30, 30);
		} else {
			int rows = model.getHeight();
			int cols = model.getWidth();
			
			for (int y = 0; y < rows; y++) {
				for (int x = 0; x < cols; x++) {
					g2d.setColor(model.getCell(x, y));
					g2d.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
				}
			}
		}
	}
	
	public void addMouseListener(MouseListener listener) {
		component.addMouseListener(listener);
	}
	
	public void addMouseMotionListener(MouseMotionListener listener) {
		component.addMouseMotionListener(listener);
	}
	
	public void addKeyListener(KeyListener listener) {
		component.addKeyListener(listener);
	}
	
	public JComponent getComponent() {
		return component;
	}
}
