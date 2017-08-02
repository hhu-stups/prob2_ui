package de.prob2.ui.verifications.modelchecking;

import de.prob.check.ModelCheckingOptions;

import java.util.Objects;

public class ModelCheckingItem {

	private final ModelCheckingOptions options;
	private final ModelCheckStats stats;

	public ModelCheckingItem(ModelCheckingOptions options, ModelCheckStats stats) {
		Objects.requireNonNull(options);
		Objects.requireNonNull(stats);
		
		this.options = options;
		this.stats = stats;
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
	}

	public ModelCheckStats getStats() {
		return this.stats;
	}
	
	public ModelCheckStats.Result getResult() {
		return stats.getResult();
	}

}
