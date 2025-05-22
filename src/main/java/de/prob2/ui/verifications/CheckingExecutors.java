package de.prob2.ui.verifications;

import com.google.inject.Inject;

import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.simulation.SimulationItemHandler;

public record CheckingExecutors(CliTaskExecutor cliExecutor, SimulationItemHandler simulationItemHandler) {
	@Inject
	public CheckingExecutors {}
}
