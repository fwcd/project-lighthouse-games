package lighthouse.ui.board.viewmodel.graphics;

/**
 * A drawable scene item. Coordinates
 * are in grid space, but with double precision.
 */
public interface SceneShape {
	void accept(SceneShapeVisitor visitor);
}
