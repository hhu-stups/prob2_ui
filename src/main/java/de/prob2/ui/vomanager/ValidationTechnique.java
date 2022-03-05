package de.prob2.ui.vomanager;

public enum ValidationTechnique {
	MODEL_CHECKING("Model Checking", "MC"),
	LTL_MODEL_CHECKING("LTL Model Checking", "LTL"),
	SYMBOLIC_MODEL_CHECKING("Symbolic Model Checking", "SMC"),
	TRACE_REPLAY("Trace Replay", "TR"),
	SIMULATION("Simulation", "SIM"),
	PARALLEL("Parallel Composition", "PAR"),
	SEQUENTIAL("Sequential Composition", "SEQ");

	private final String name;

	private final String id;

	ValidationTechnique(String name, String id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getName();
	}
}
