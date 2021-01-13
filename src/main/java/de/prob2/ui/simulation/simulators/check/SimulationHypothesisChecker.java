package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import org.apache.commons.math3.analysis.function.Gaussian;

import java.util.List;
import java.util.Map;

public class SimulationHypothesisChecker extends SimulationMonteCarlo {

	public enum HypothesisCheckingType {
		LEFT_TAILED("Left-tailed hypothesis test"),
		RIGHT_TAILED("Right-tailed hypothesis test"),
		TWO_TAILED("Two-tailed hypothesis test");

		private String name;

		HypothesisCheckingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

    public enum HypothesisCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private final SimulationCheckingType type;

	private final HypothesisCheckingType hypothesisCheckingType;

	private final double probability;

    private final Map<String, Object> additionalInformation;

    private int numberSuccess;

    private HypothesisCheckResult result;

    public SimulationHypothesisChecker(final Trace trace, final int numberExecutions, final int numberStepsPerExecution, final SimulationCheckingType type,
									   final HypothesisCheckingType hypothesisCheckingType, final double probability, final Map<String, Object> additionalInformation) {
        super(trace, numberExecutions, numberStepsPerExecution);
		this.type = type;
		this.hypothesisCheckingType = hypothesisCheckingType;
		this.probability = probability;
		this.additionalInformation = additionalInformation;
		this.result = HypothesisCheckResult.NOT_FINISHED;
    }

	@Override
    public void checkTrace(Trace trace, int time) {
		switch (type) {
			case ALL_INVARIANTS:
				checkAllInvariants(trace);
				break;
			case INVARIANT:
				checkInvariant(trace);
				break;
			case ALMOST_CERTAIN_PROPERTY:
				checkAlmostCertainProperty(trace);
				break;
			case TIMING:
				checkTiming(time);
				break;
			default:
				break;
		}
	}

	public void checkAllInvariants(Trace trace) {
    	boolean invariantOk = true;
    	for(Transition transition : trace.getTransitionList()) {
			State destination = transition.getDestination();
			if(!destination.isInvariantOk()) {
				invariantOk = false;
				break;
			}
		}
    	if(invariantOk) {
    		numberSuccess++;
		}
	}

	public void checkInvariant(Trace trace) {
		boolean invariantOk = true;
		String invariant = (String) additionalInformation.get("INVARIANT");
		for(Transition transition : trace.getTransitionList()) {
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				AbstractEvalResult evalResult = destination.eval(invariant, FormulaExpand.TRUNCATE);
				if ("FALSE".equals(evalResult.toString())) {
					invariantOk = false;
					break;
				}
			}
		}
		if(invariantOk) {
			numberSuccess++;
		}
	}

	public void checkAlmostCertainProperty(Trace trace) {
    	String property = (String) additionalInformation.get("PROPERTY");
    	for(Transition transition : trace.getTransitionList()) {
    		State destination = transition.getDestination();
    		if(destination.isInitialised()) {
				AbstractEvalResult evalResult = destination.eval(property, FormulaExpand.TRUNCATE);
				if("TRUE".equals(evalResult.toString())) {
					List<Transition> transitions = destination.getOutTransitions();
					String stateID = destination.getId();
					//TODO: Implement some caching
					boolean propertyOk = transitions.stream()
							.map(Transition::getDestination)
							.map(dest -> stateID.equals(dest.getId()))
							.reduce(true, (e, a) -> e && a);
					if(propertyOk) {
						numberSuccess++;
						break;
					}
				}
			}
		}
	}

	private void checkTwoTailed() {
		int n = resultingTraces.size();
		double p = probability;
    	double mu = Math.round(n * p);
    	double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for(int i = 1; i <= Math.ceil(mu/2); i++) {
			if(100.0 - coverage < p) {
				range = i;
				break;
			}
			coverage = gaussian.value(mu + i + 0.5) - gaussian.value(mu - i - 0.5);
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= mu - range && numberFailed <= mu + range) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

	private void checkLeftTailed() {
		int n = resultingTraces.size();
		double p = probability;
		double mu = Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		if(p >= 0.5) {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage < p) {
					range = i;
					break;
				}
				coverage = 100.0 - gaussian.value(mu - i - 0.5);
			}
		} else {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage >= p) {
					range = i;
					break;
				}
				coverage = 100.0 - gaussian.value(mu + i - 0.5);
			}
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= mu - range && numberFailed <= n) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

	private void checkRightTailed() {
		int n = resultingTraces.size();
		double p = probability;
		int mu = (int) Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		if(p >= 0.5) {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage < p) {
					range = i;
					break;
				}
				coverage = gaussian.value(mu + i + 0.5);
			}
		} else {
			for (int i = 1; i <= mu; i++) {
				if (100.0 - coverage >= p) {
					range = i;
					break;
				}
				coverage = gaussian.value(mu - i + 0.5);
			}
		}
		int numberFailed = n - numberSuccess;
		if(numberFailed >= 0 && numberFailed <= mu + range) {
			this.result = HypothesisCheckResult.SUCCESS;
		} else {
			this.result = HypothesisCheckResult.FAIL;
		}
	}

	public void checkTiming(int time) {
		int maximumTime = (int) additionalInformation.get("TIME");
		if(time <= maximumTime) {
			numberSuccess++;
		}
	}

    public void check() {
    	switch (hypothesisCheckingType) {
			case LEFT_TAILED:
				checkLeftTailed();
				break;
			case RIGHT_TAILED:
				checkRightTailed();
				break;
			case TWO_TAILED:
				checkTwoTailed();
				break;
			default:
				break;
		}
    }

	public HypothesisCheckResult getResult() {
		return result;
	}
}
