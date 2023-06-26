package de.prob2.ui.simulation.external;

public class ExternalSimulationStep {

	private String op;

	private String predicate;

	private String delta;

	private boolean done;

	public ExternalSimulationStep(String op, String predicate, String delta, boolean done) {
		this.op = op;
		this.predicate = predicate;
		this.delta = delta;
		this.done = done;
	}

	public String getOp() {
		return op;
	}

	public String getPredicate() {
		return predicate;
	}

	public String getDelta() {
		return delta;
	}

	public boolean isDone() {
		return done;
	}
}
