package de.prob2.ui.events;

import de.prob2.ui.modelchecking.ModelCheckStats;

public class ModelCheckStatsEvent {
	private ModelCheckStats modelCheckStats;
	private String result;
	private String message;
	private String note;

	public ModelCheckStatsEvent(ModelCheckStats modelCheckStats, String result, String message, String note) {
		this.modelCheckStats = modelCheckStats;
		this.result = result;
		this.message = message;
		this.note = note;
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
	
	public String getNote() {
		return note;
	}
}
