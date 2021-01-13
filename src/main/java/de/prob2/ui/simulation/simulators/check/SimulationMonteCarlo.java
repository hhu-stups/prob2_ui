package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationMonteCarlo extends ProbabilityBasedSimulator {

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

    protected int numberStepsPerExecutions;

    public SimulationMonteCarlo(Trace trace, int numberExecutions, int numberStepsPerExecution) {
        super();
        this.resultingTraces = new ArrayList<>();
        this.trace = trace;
        this.numberExecutions = numberExecutions;
        this.numberStepsPerExecutions = numberStepsPerExecution;
    }

    @Override
    public void run() {
		Trace startTrace = trace;
		try {
			startTrace.getStateSpace().startTransaction();
			startTrace = setupBeforeSimulation(startTrace);

			for (int i = 0; i < numberExecutions; i++) {
				Trace newTrace = startTrace;
				int stepCounter = 0;
				this.finished = false;
				while (stepCounter < numberStepsPerExecutions && !finished) {
					Trace nextTrace = simulationStep(newTrace);
					stepCounter = nextTrace.getTransitionList().size();
					newTrace = nextTrace;
					if(stepCounter >= numberStepsPerExecutions || finished) {
						Trace addedTrace = new Trace(newTrace.getStateSpace());
						addedTrace.addTransitions(newTrace.getTransitionList().subList(0, numberStepsPerExecutions));
						resultingTraces.add(addedTrace);
						checkTrace(addedTrace, time.get());
						resetSimulator();
					}
				}
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

}
