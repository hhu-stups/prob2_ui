package de.prob2.ui.animations;

public class Animation {
	private String modelName;
	private String lastOperation;
	private String steps;
	
	public Animation(String modelName, String lastOperation, String steps) {
		this.modelName = modelName;
		this.lastOperation = lastOperation;
		this.steps = steps;
	}

	public String getModelName() {
		return modelName;
	}
	
	public String getLastOperation() {
		return lastOperation;
	}
	
	public String getSteps() {
		return steps;
	}
	
	void setLastOperation(String lastOperation) {
		this.lastOperation = lastOperation;
	}
	
	void setSteps(String steps) {
		this.steps = steps;
	}
}
