package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

import java.util.List;

public interface ISimulationPropertyChecker {
	List<Trace> getResultingTraces();
	List<List<Integer>> getResultingTimestamps();
	List<Checked> getResultingStatus();
	SimulationStats getStats();
	SimulationCheckingSimulator.MonteCarloCheckResult getResult();
	void setResult(SimulationCheckingSimulator.MonteCarloCheckResult result);
	void setStats(SimulationStats stats);
	int getNumberSuccess();
	SimulationExtendedStats calculateExtendedStats();
	void run();
	void check();
	Checked checkTrace(Trace trace, int time);
	void calculateStatistics(long time);
}
