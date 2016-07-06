package de.prob2.ui.events;

import de.prob2.ui.modelchecking.ModelCheckStats;

public class ModelCheckStatsEvent {
	private ModelCheckStats modelCheckStats;
	private String result;

	public ModelCheckStatsEvent(ModelCheckStats modelCheckStats, String result) {
		this.modelCheckStats = modelCheckStats;
		this.result = result;
	}

	public ModelCheckStats getModelCheckStats() {
		return modelCheckStats;
	}
	
	public String getResult() {
		return result;
	}
}
