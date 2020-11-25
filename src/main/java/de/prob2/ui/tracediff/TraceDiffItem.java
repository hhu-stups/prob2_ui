package de.prob2.ui.tracediff;

class TraceDiffItem {
	private final int id;
	private final String string;

	TraceDiffItem(int id, String string) {
		this.id = id;
		this.string = string;
	}

	int getId() {
		return id;
	}

	String getString() {
		return string;
	}
}
