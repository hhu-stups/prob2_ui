package de.prob2.ui.simulation.external;

import java.util.Objects;
import java.util.StringJoiner;

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

	public void setOp(String op) {
		this.op = op;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getDelta() {
		return delta;
	}

	public void setDelta(String delta) {
		this.delta = delta;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExternalSimulationStep that = (ExternalSimulationStep) o;
		return done == that.done && Objects.equals(op, that.op) && Objects.equals(predicate, that.predicate) && Objects.equals(delta, that.delta);
	}

	@Override
	public int hashCode() {
		return Objects.hash(op, predicate, delta, done);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ExternalSimulationStep.class.getSimpleName() + "[", "]")
				.add("op='" + op + "'")
				.add("predicate='" + predicate + "'")
				.add("delta='" + delta + "'")
				.add("done=" + done)
				.toString();
	}
}
