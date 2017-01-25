package de.prob2.ui.animations;

import java.time.LocalDateTime;

import de.prob.statespace.Trace;

import de.prob2.ui.beditor.BEditorStage;

public class Animation {
	private final String modelName;
	private final String lastOperation;
	private final String steps;
	private final Trace trace;
	private final boolean isCurrent;
	private final boolean isProtected;
	private LocalDateTime time;
	private BEditorStage bEditorStage;

	public Animation(
		String modelName,
		String lastOperation,
		String steps,
		Trace trace,
		boolean isCurrent,
		boolean isProtected,
		BEditorStage bEditorStage
	) {
		this.modelName = modelName;
		this.lastOperation = lastOperation;
		this.steps = steps;
		this.trace = trace;
		this.isCurrent = isCurrent;
		this.isProtected = isProtected;
		this.time = null;
		this.bEditorStage = bEditorStage;
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

	public Trace getTrace() {
		return trace;
	}

	public boolean isCurrent() {
		return isCurrent;
	}

	public boolean isProtected() {
		return isProtected;
	}

	public LocalDateTime getTime() {
		return time;
	}

	public void setTime(LocalDateTime time) {
		this.time = time;
	}
	
	public BEditorStage getBEditorStage() {
		return bEditorStage;
	}
}
