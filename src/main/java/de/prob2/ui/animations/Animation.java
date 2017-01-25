package de.prob2.ui.animations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import de.prob.statespace.Trace;
import de.prob2.ui.beditor.BEditorStage;

public class Animation {
	private String modelName;
	private String lastOperation;
	private String steps;
	private Trace trace;
	private boolean isCurrent;
	private boolean isProtected;
	private String time;
	private BEditorStage beditorStage;

	public Animation(String modelName, String lastOperation, String steps, Trace trace, boolean isCurrent,
			boolean isProtected, BEditorStage beditorStage) {
		this.modelName = modelName;
		this.lastOperation = lastOperation;
		this.steps = steps;
		this.trace = trace;
		this.isCurrent = isCurrent;
		this.isProtected = isProtected;
		this.beditorStage = beditorStage;
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

	public void setTime(LocalDateTime time) {
		this.time = time.format(DateTimeFormatter.ofPattern("HH:mm:ss d MMM uuuu"));
	}

	public String getTime() {
		return time;
	}

	public void openEditor() {
		beditorStage.show();
	}

}
