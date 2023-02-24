package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

import java.util.List;

public interface ISimulationPropertyChecker {
	List<Trace> getResultingTraces();
	List<List<Integer>> getResultingTimestamps();
	List<Checked> getResultingStatus();
	SimulationStats getStats();
	SimulationMonteCarlo.MonteCarloCheckResult getResult();
	void setResult(SimulationMonteCarlo.MonteCarloCheckResult result);
	int getNumberSuccess();
	SimulationExtendedStats calculateExtendedStats();
	void run();
	void check();
	Checked checkTrace(Trace trace, int time);
}
