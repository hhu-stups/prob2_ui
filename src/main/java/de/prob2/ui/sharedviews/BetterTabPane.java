package de.prob2.ui.sharedviews;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/**
 * <p>Slightly modified version of {@link TabPane} that calculates a proper minimum size based on its contents.</p>
 */
public final class BetterTabPane extends TabPane {
	public BetterTabPane() {
		this((Tab[])null);
	}
	
	public BetterTabPane(final Tab... tabs) {
		super(tabs);
	}
	
	private boolean isHorizontal() {
		final Side tabPosition = this.getSide();
		return Side.TOP.equals(tabPosition) || Side.BOTTOM.equals(tabPosition);
	}

	private Region getTabHeaderArea() {
		final Node tabHeaderArea = this.lookup(".tab-header-area");
		return tabHeaderArea instanceof Region ? (Region)tabHeaderArea : null;
	}
	
	@Override
	protected double computeMinWidth(final double height) {
		final double defaultMinWidth = super.computeMinWidth(height);
		if (defaultMinWidth != 0.0) {
			return defaultMinWidth;
		}
		
		double highestMinWidth = 0.0;
		for (final Tab tab : this.getTabs()) {
			highestMinWidth = Math.max(highestMinWidth, this.snapSizeX(tab.getContent().minWidth(-1)));
		}
		
		final boolean isHorizontal = this.isHorizontal();
		final Region tabHeaderArea = this.getTabHeaderArea();
		final double tabHeaderAreaSize;
		if (tabHeaderArea == null) {
			tabHeaderAreaSize = 0.0;
		} else {
			tabHeaderAreaSize = snapSizeX(isHorizontal ? tabHeaderArea.prefWidth(-1) : tabHeaderArea.prefHeight(-1));
		}
		
		double prefWidth = isHorizontal ? Math.max(highestMinWidth, tabHeaderAreaSize) : highestMinWidth + tabHeaderAreaSize;
		return snapSizeX(prefWidth) + snappedRightInset() + snappedLeftInset();
	}
	
	@Override
	protected double computeMinHeight(final double width) {
		final double defaultMinHeight = super.computeMinHeight(width);
		if (defaultMinHeight != 0.0) {
			return defaultMinHeight;
		}
		
		double highestMinHeight = 0.0;
		for (final Tab tab : this.getTabs()) {
			highestMinHeight = Math.max(highestMinHeight, this.snapSizeY(tab.getContent().minHeight(-1)));
		}
		
		final boolean isHorizontal = this.isHorizontal();
		final Region tabHeaderArea = this.getTabHeaderArea();
		final double tabHeaderAreaSize;
		if (tabHeaderArea == null) {
			tabHeaderAreaSize = 0.0;
		} else {
			tabHeaderAreaSize = snapSizeY(isHorizontal ? tabHeaderArea.prefHeight(-1) : tabHeaderArea.prefWidth(-1));
		}
		
		double prefWidth = isHorizontal ? highestMinHeight + snapSizeY(tabHeaderAreaSize) : Math.max(highestMinHeight, tabHeaderAreaSize);
		return snapSizeY(prefWidth) + snappedTopInset() + snappedBottomInset();
	}
}
