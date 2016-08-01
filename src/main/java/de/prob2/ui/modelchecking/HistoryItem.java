package de.prob2.ui.modelchecking;

import de.prob.check.ModelCheckingOptions;

public class HistoryItem {

	private ModelCheckingOptions options;
	private ModelCheckStats stats;

	public HistoryItem(ModelCheckingOptions options, ModelCheckStats stats) {
		this.options = options;
		this.stats = stats;
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
	}

	public ModelCheckStats getStats() {
		return this.stats;
	}

}
