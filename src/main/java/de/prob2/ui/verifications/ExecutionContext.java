package de.prob2.ui.verifications;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;

public record ExecutionContext(Project project, Machine machine, StateSpace stateSpace, Trace trace) {
}
