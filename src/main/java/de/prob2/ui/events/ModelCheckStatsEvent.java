package de.prob2.ui.events;

import de.prob2.ui.modelchecking.ModelCheckStats;

public class ModelCheckStatsEvent {
	private ModelCheckStats modelCheckStats;
	private String result;
	private String message;

	public ModelCheckStatsEvent(ModelCheckStats modelCheckStats, String result, String message) {
		this.modelCheckStats = modelCheckStats;
		this.result = result;
		this.message = message;
	}

	public ModelCheckStats getModelCheckStats() {
		return modelCheckStats;
	}
	
	public String getResult() {
		return result;
	}
	
	public String getMessage() {
		return message;
	}
	
}
