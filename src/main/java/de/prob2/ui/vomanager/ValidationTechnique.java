package de.prob2.ui.vomanager;

public enum ValidationTechnique {
	MODEL_CHECKING("Model Checking"),
	LTL_MODEL_CHECKING("LTL Model Checking"),
	SYMBOLIC_MODEL_CHECKING("Symbolic Model Checking"),
	TRACE_REPLAY("Trace Replay"),
	SIMULATION("Simulation"),
	PARALLEL("Parallel Composition"),
	SEQUENTIAL("Sequential Composition");

	private final String name;

	ValidationTechnique(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}
}
