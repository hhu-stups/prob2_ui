package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;
import org.apache.commons.math3.analysis.function.Gaussian;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationHypothesisChecker extends SimulationMonteCarlo {

	public enum CheckingType {
		ALL_INVARIANTS, INVARIANT, ALMOST_CERTAIN_PROPERTY, TIMING
	}

	public enum HypothesisCheckingType {
		LEFT_TAILED, RIGHT_TAILED, TWO_TAILED
	}

    public enum HypothesisCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private final CheckingType type;

	private final HypothesisCheckingType hypothesisCheckingType;

	private final double probability;

    private final Map<String, Object> additionalInformation;

    private int numberSuccess;

    public SimulationHypothesisChecker(final Trace trace, final int numberExecutions, final int numberStepsPerExecution, final CheckingType type,
									   final HypothesisCheckingType hypothesisCheckingType, final double probability, final Map<String, Object> additionalInformation) {
        super(trace, numberExecutions, numberStepsPerExecution);
		this.type = type;
		this.hypothesisCheckingType = hypothesisCheckingType;
		this.probability = probability;
		this.additionalInformation = additionalInformation;
    }

	@Override
    public void checkTrace(Trace trace) {
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
				// TODO
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

	private HypothesisCheckResult checkTwoTailed() {
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
			coverage = gaussian.value(mu + i) - gaussian.value(mu - i);
		}
		if(numberSuccess >= mu - range && numberSuccess <= mu + range) {
			return HypothesisCheckResult.SUCCESS;
		}
		return HypothesisCheckResult.FAIL;
	}

	private HypothesisCheckResult checkLeftTailed() {
		int n = resultingTraces.size();
		double p = probability;
		double mu = Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for(int i = 1; i <= mu; i++) {
			if(100.0 - coverage < p) {
				range = i;
				break;
			}
			coverage = 100.0 - gaussian.value(mu - i);
		}
		// TODO: What if p < 50% ?
		if(numberSuccess >= mu - range && numberSuccess <= n) {
			return HypothesisCheckResult.SUCCESS;
		}
		return HypothesisCheckResult.FAIL;
	}

	private HypothesisCheckResult checkRightTailed() {
		int n = resultingTraces.size();
		double p = probability;
		double mu = Math.round(n * p);
		double sigma = Math.sqrt(n * p * (1 - p));
		Gaussian gaussian = new Gaussian(mu, sigma);
		double coverage = 0.0;

		int range = 0;

		for(int i = 1; i <= mu; i++) {
			if(100.0 - coverage < p) {
				range = i;
				break;
			}
			coverage = gaussian.value(mu + i);
		}
		// TODO: What if p < 50% ?
		if(numberSuccess >= 0 && numberSuccess <= mu + range) {
			return HypothesisCheckResult.SUCCESS;
		}
		return HypothesisCheckResult.FAIL;
	}

    public HypothesisCheckResult check() {
    	switch (hypothesisCheckingType) {
			case LEFT_TAILED:
				return checkLeftTailed();
			case RIGHT_TAILED:
				return checkRightTailed();
			case TWO_TAILED:
				return checkTwoTailed();
			default:
				break;
		}
        return HypothesisCheckResult.NOT_FINISHED;
    }

}
