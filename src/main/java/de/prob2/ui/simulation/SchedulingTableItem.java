package de.prob2.ui.simulation;

import de.prob2.ui.simulation.simulators.Activation;

import java.util.Objects;

public class SchedulingTableItem {

	private final int time;

	private final Activation activation;

	public SchedulingTableItem(int time, Activation activation) {
		this.time = time;
		this.activation = activation;
	}

	public int getTime() {
		return time;
	}

	public Activation getActivation() {
		return activation;
	}

	@Override
	public String toString() {
		return String.format("{time = %s, activationDetails = %s}", time, activation.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SchedulingTableItem that = (SchedulingTableItem) o;
		return time == that.time && Objects.equals(activation, that.activation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, activation);
	}
}
