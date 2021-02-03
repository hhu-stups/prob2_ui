package de.prob2.ui.simulation.simulators.check;


import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.Simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationMonteCarlo extends Simulator {

	public enum EndingType {
		NUMBER_STEPS("Number Steps"),
		ENDING_PREDICATE("Ending Predicate"),
		ENDING_TIME("Ending Time");

		private String name;

		EndingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

    protected List<Trace> resultingTraces;

    protected Trace trace;

    protected int numberExecutions;

    protected Map<String, Object> additionalInformation;

    public SimulationMonteCarlo(final CurrentTrace currentTrace, Trace trace, int numberExecutions, Map<String, Object> additionalInformation) {
        super(currentTrace);
        this.resultingTraces = new ArrayList<>();
        this.trace = trace;
        this.numberExecutions = numberExecutions;
        this.additionalInformation = additionalInformation;
    }

    @Override
    public boolean endingConditionReached(Trace trace) {
		if(additionalInformation.containsKey("STEPS_PER_EXECUTION")) {
			int stepsPerExecution = (int) additionalInformation.get("STEPS_PER_EXECUTION");
			return stepCounter >= stepsPerExecution;
		} else if(additionalInformation.containsKey("ENDING_PREDICATE")) {
			String predicate = (String) additionalInformation.get("ENDING_PREDICATE");
			State state = trace.getCurrentState();
			String evalResult = cache.readValueWithCaching(state, predicate);
			return "TRUE".equals(evalResult);
		} else if(additionalInformation.containsKey("ENDING_TIME")) {
			int endingTime = (int) additionalInformation.get("ENDING_TIME");
			return endingTime >= time.get();
		}
		return false;
	}

    @Override
    public void run() {
		Trace startTrace = trace;

		try {
			startTrace.getStateSpace().startTransaction();

			for (int i = 0; i < numberExecutions; i++) {
				Trace newTrace = setupBeforeSimulation(startTrace);
				while (!endingConditionReached(newTrace)) {
					newTrace = simulationStep(newTrace);
				}
				resultingTraces.add(newTrace);
				checkTrace(newTrace, time.get());
				resetSimulator();
			}
			check();
		} finally {
			startTrace.getStateSpace().endTransaction();
		}
    }

    public void check() {
		// Monte Carlo Simulation does not apply any checks. But classes inheriting from SimulationMonteCarlo might apply some checks.
	}

    public void checkTrace(Trace trace, int time) {
    	// Monte Carlo Simulation does not apply any checks on a trace. But classes inheriting from SimulationMonteCarlo might apply some checks
	}

	public List<Trace> getResultingTraces() {
		return resultingTraces;
	}

}
