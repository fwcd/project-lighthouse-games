package lighthouse.view;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import lighthouse.model.AppModel;
import lighthouse.view.remote.LighthouseGridView;

/**
 * The application's base view.
 */
public class AppViewController {
	private final JComponent view;
	
	public AppViewController(AppModel model) {
		view = new JPanel();
		view.setLayout(new BorderLayout());
		
		GameBoardViewController board = new GameBoardViewController(model.getBoard());
		
		// Register local view (with doubles as a Swing component)
		LocalGridView localView = new LocalGridView(model.getBoard());
		board.addView(localView);
		view.add(new LocalGridView(model.getBoard()).getComponent(), BorderLayout.CENTER);
		
		// Register remote Lighthouse view
		board.addView(new LighthouseGridView());
	}
	
	public JComponent getComponent() {
		return view;
	}
}
