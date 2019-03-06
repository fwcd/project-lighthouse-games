package lighthouse.ui;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lighthouse.gameapi.Game;
import lighthouse.gameapi.GameInitializationContext;
import lighthouse.gameapi.SceneInteractionFacade;
import lighthouse.model.AppModel;
import lighthouse.puzzle.PuzzleGame;
import lighthouse.ui.discordrpc.DiscordRPCRunner;
import lighthouse.ui.scene.SceneInteractionBackend;
import lighthouse.ui.scene.SceneViewController;
import lighthouse.ui.scene.view.LocalSceneView;
import lighthouse.ui.sidebar.SideBarViewController;
import lighthouse.ui.util.SwapPanel;

/**
 * The application's base view controller.
 */
public class AppViewController implements SwingViewController {
	private static final Logger LOG = LoggerFactory.getLogger(AppViewController.class);
	private final AppModel model;
	
	private final JComponent component;
	private final JToolBar tabBar;
	private final SceneViewController scene;
	private final SideBarViewController sideBar;
	private final SwapPanel contentPane;
	
	private final AppContext context = new AppContext();
	private final SceneInteractionFacade interactionFacade;
	private final DiscordRPCRunner discordRPC = new DiscordRPCRunner();
	
	private final Set<Game> gameRegistry = new HashSet<>();
	
	public AppViewController(AppModel model) {
		this.model = model;
		
		component = new JPanel();
		component.setLayout(new BorderLayout());
		
		JPanel centerPane = new JPanel();
		centerPane.setLayout(new BorderLayout());
		
		tabBar = new JToolBar();
		centerPane.add(tabBar, BorderLayout.NORTH);
		
		scene = new SceneViewController();
		interactionFacade = new SceneInteractionBackend(scene, scene.getResponder(), this::update);
		contentPane = new SwapPanel(scene.getComponent());
		centerPane.add(contentPane, BorderLayout.CENTER);
		
		component.add(centerPane, BorderLayout.CENTER);
		
		sideBar = new SideBarViewController(model, scene);
		component.add(sideBar.getComponent(), BorderLayout.EAST);
		
		// Register known games
		registerGames();

		// Setup RPC
		discordRPC.setState(context.getStatus().getMessage());
		discordRPC.updatePresenceSoon();
		discordRPC.start();
		
		context.getStatusListeners().add(newStatus -> {
			discordRPC.setState(newStatus.getMessage());
			discordRPC.updatePresenceSoon();
		});
	}
	
	private void registerGames() {
		registerGame(new PuzzleGame());
	}
	
	private void update() {
		scene.render();
	}
	
	public void registerGame(Game game) {
		game.initialize(new GameInitializationContext(context.getObservableStatus(), interactionFacade));
		
		// TODO: Toggle buttons to indicate the active tab
		JButton tab = new JButton(game.getName());
		tab.addActionListener(e -> open(game));
		tabBar.add(tab);
		
		if (gameRegistry.isEmpty()) {
			open(game);
		}
		
		gameRegistry.add(game);
	}
	
	private void open(Game game) {
		LOG.info("Opening game {}...", game.getName());
		
		model.setActiveGameState(game.getModel());
		scene.getViewModel().setLayers(game.getGameLayer());
		
		LocalSceneView localView = scene.getLocalView();
		localView.setGridPosToPixels(game.getGridPosToPixels().floor());
		localView.setGridSizeToPixels(game.getGridSizeToPixels().floor());
		
		if (game.hasCustomGameViewController()) {
			contentPane.swapTo(game.getCustomGameViewController().getComponent());
		} else {
			contentPane.swapTo(scene.getComponent());
		}
		
		scene.relayout(game.getModel().getGridSize());
		scene.setGridTransforms(game.getGridPosToPixels(), game.getGridSizeToPixels());
		scene.setLighthouseTransforms(game.getLighthouseToGridSize(), game.getLighthouseToGridPos());
		
		sideBar.setGameControls(game.getControlsViewController().getComponent());
		sideBar.setGameStatistics(game.getStatisticsViewController().getComponent());
		sideBar.setSolver(game.getSolverViewController().getComponent());
		
		discordRPC.setDetails("Playing " + game.getName());
		discordRPC.updatePresenceSoon();
		update();
	}
	
	@Override
	public JComponent getComponent() {
		return component;
	}
}
