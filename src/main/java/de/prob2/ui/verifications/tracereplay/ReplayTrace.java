package de.prob2.ui.verifications.tracereplay;

import java.util.ArrayList;
import java.util.List;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

public class ReplayTrace {

	private final  List<ReplayTransition> transitionList = new ArrayList<>();

	public ReplayTrace(Trace trace) {
		for(Transition t: trace.getTransitionList()) {
			transitionList.add(new ReplayTransition(t.getName(), t.getParams()));
		}
	}
	
	public List<ReplayTransition> getTransitionList() {
		return transitionList;
	}
}
