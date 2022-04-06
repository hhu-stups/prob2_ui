package de.prob2.ui.tracediff;

import javafx.application.Platform;
import javafx.scene.control.ListCell;

class TraceDiffCell extends ListCell<TraceDiffItem> {
	private final int cellHeight = 20;
	public enum TraceDiffCellStyle {
		FAULTY, FOLLOWING,
	}

	TraceDiffCell() {
		getStyleClass().add("trace-diff-cell");
	}

	@Override
	protected void updateItem(TraceDiffItem item, boolean empty) {
		super.updateItem(item, empty);
		getStyleClass().removeAll(TraceDiffCellStyle.FAULTY.name(), TraceDiffCellStyle.FOLLOWING.name());
		if (item != null){
			String s = item.getString();
			setText(s);
			Platform.runLater(() -> {
				int lines = s == null? 0 : s.split("\n").length;
				if (TraceDiff.indexLinesMap.get(item.getId()) == null || TraceDiff.indexLinesMap.get(item.getId()) < lines) {
					TraceDiff.indexLinesMap.put(item.getId(), lines);
				} else {
					lines = TraceDiff.indexLinesMap.get(item.getId());
				}
				setPrefHeight(lines * cellHeight);
			});

			if (null != item.getStyle()) {
				getStyleClass().add(item.getStyle().name());
			}
		} else {
			setText(null);
		}
	}
}
