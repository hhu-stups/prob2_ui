package de.prob2.ui.tracediff;

class TraceDiffItem {
	private final int id;
	private TraceDiffCell.TraceDiffCellStyle style;
	private final String string;

	TraceDiffItem(int id, String string) {
		this.id = id;
		this.string = string;
	}

	int getId() {
		return id;
	}

	void setStyle(TraceDiffCell.TraceDiffCellStyle style) {
		this.style = style;
	}

	TraceDiffCell.TraceDiffCellStyle getStyle() {
		return style;
	}

	String getString() {
		return string;
	}
}
