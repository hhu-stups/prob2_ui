package de.prob2.ui.verifications;

import de.prob.statespace.StateSpace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;

public final class ExecutionContext {
	private final Project project;
	private final Machine machine;
	private final StateSpace stateSpace;
	
	public ExecutionContext(final Project project, final Machine machine, final StateSpace stateSpace) {
		this.project = project;
		this.machine = machine;
		this.stateSpace = stateSpace;
	}
	
	public Project getProject() {
		return this.project;
	}
	
	public Machine getMachine() {
		return this.machine;
	}
	
	public StateSpace getStateSpace() {
		return this.stateSpace;
	}
}
