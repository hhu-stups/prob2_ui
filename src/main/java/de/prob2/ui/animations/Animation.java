package de.prob2.ui.animations;

import java.time.LocalDateTime;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;

public class Animation {
	private final String name;
	private final AbstractModel model;
	private final String lastOperation;
	private final String steps;
	private final Trace trace;
	private final boolean isCurrent;
	private final boolean isProtected;
	private LocalDateTime time;

	public Animation(
		String name,
		AbstractModel model,
		String lastOperation,
		String steps,
		Trace trace,
		boolean isCurrent,
		boolean isProtected
	) {
		this.name = name;
		this.model = model;
		this.lastOperation = lastOperation;
		this.steps = steps;
		this.trace = trace;
		this.isCurrent = isCurrent;
		this.isProtected = isProtected;
		this.time = null;
	}

	public String getName() {
		return name;
	}

	public AbstractModel getModel() {
		return model;
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
}
