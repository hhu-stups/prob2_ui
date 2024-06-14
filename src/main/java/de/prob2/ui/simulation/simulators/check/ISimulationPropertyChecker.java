package de.prob2.ui.simulation.simulators.check;

import java.util.List;
import java.util.Map;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.CheckingStatus;

public interface ISimulationPropertyChecker {
	List<Trace> getResultingTraces();
	List<List<Integer>> getResultingTimestamps();
	List<CheckingStatus> getResultingStatus();
	SimulationStats getStats();
	SimulationCheckingSimulator.MonteCarloCheckResult getResult();
	void setResult(SimulationCheckingSimulator.MonteCarloCheckResult result);
	void setStats(SimulationStats stats);
	int getNumberSuccess();
	SimulationExtendedStats calculateExtendedStats();
	void run();
	void check();
	CheckingStatus checkTrace(Trace trace, int time);
	void calculateStatistics(long time);
	Map<String, List<Integer>> getOperationExecutions();
}
