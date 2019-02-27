package lighthouse.ui;

import javax.swing.JComponent;

import lighthouse.model.Game;
import lighthouse.model.GameState;
import lighthouse.model.Level;
import lighthouse.model.Status;
import lighthouse.ui.board.BoardViewController;
import lighthouse.ui.board.controller.BoardPlayController;
import lighthouse.ui.board.controller.EditingControllerPicker;
import lighthouse.ui.board.controller.PlayControllerPicker;
import lighthouse.ui.stage.LevelStage;
import lighthouse.ui.stage.LevelStages;
import lighthouse.ui.tickers.GameWinChecker;
import lighthouse.ui.tickers.TickerList;
import lighthouse.util.ColorUtils;
import lighthouse.util.Listener;
import lighthouse.util.ListenerList;

/**
 * Manages the game board view and the current game/level stage.
 */
public class GameViewController implements ViewController {
	private final Game model;
	private final BoardViewController board;
	
	private LevelStage stage = LevelStages.IN_GAME;
	
	private final TickerList tickers = new TickerList();
	private final ListenerList<LevelStage> stageListeners = new ListenerList<>();
	
	private final GameWinChecker winChecker;
	private final Listener<LevelStage> playControlListener;
	private final Listener<LevelStage> editControlListener;
	
	public GameViewController(Game model) {
		this.model = model;
		
		// Initialize board
		board = new BoardViewController(model.getState().getBoard());
		model.getState().getBoardListeners().add(board::updateModel);
		
		// Setup tickers
		winChecker = new GameWinChecker(board.getComponent(), model.getState());
		
		// Setup controller pickers
		editControlListener = stage -> {
			board.setResponder(stage.accept(new EditingControllerPicker(model.getState().getBoard())));
		};
		playControlListener = stage -> {
			board.setResponder(stage.accept(new PlayControllerPicker(model.getState().getBoard())));
		};
		
		// Add level hooks
		model.getState().getLevelListeners().add(level -> {
			level.getGoal().bindToUpdates(level.getStart());
			stage.transitionFrom(stage, model.getState());
		});
		Level initialLevel = model.getState().getLevel();
		initialLevel.getGoal().bindToUpdates(initialLevel.getStart());
		
		// Enter playing mode
		play();
	}
	
	/** Switches to playing mode. */
	public void play() {
		model.setStatus(new Status("Playing", ColorUtils.LIGHT_GREEN));
		board.setResponder(new BoardPlayController(model.getState().getBoard()));
		
		stageListeners.remove(editControlListener);
		stageListeners.add(playControlListener);
		playControlListener.on(stage);
		
		tickers.add(winChecker);
		winChecker.reset();
		
		GameState state = model.getState();
		switchToStage(LevelStages.IN_GAME);
		state.setBoard(state.getLevel().getStart().copy());
	}
	
	/** Switches to editing mode. */
	public void edit() {
		model.setStatus(new Status("Editing", ColorUtils.LIGHT_ORANGE));
		
		stageListeners.add(editControlListener);
		stageListeners.remove(playControlListener);
		tickers.remove(winChecker);
		editControlListener.on(stage);
	}
	
	public void reset() {
		board.reset();
	}
	
	public void switchToStage(LevelStage newStage) {
		if (stage != null && newStage.getIndex() != stage.getIndex()) {
			newStage.transitionFrom(stage, model.getState());
		}
		stage = newStage;
		stageListeners.fire(newStage);
	}
	
	public BoardViewController getBoard() { return board; }
	
	/** Fetches the currently viewed stage. */
	public LevelStage getStage() { return stage; }
	
	public Game getModel() { return model; }
	
	public TickerList getTickers() { return tickers; }
	
	public ListenerList<LevelStage> getStageListeners() { return stageListeners; }
	
	@Override
	public JComponent getComponent() { return board.getComponent(); }
}
