package de.prob2.ui.verifications.tracereplay;

import java.util.ArrayList;
import java.util.List;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace {
	enum Status {SUCCESSFUL, FAILED, NOT_CHECKED}

	private ObjectProperty<Status> status;
	private final  List<ReplayTransition> transitionList = new ArrayList<>();
	private Exception error;

	public ReplayTrace(Trace trace) {
		for(Transition t: trace.getTransitionList()) {
			transitionList.add(new ReplayTransition(t.getName(), t.getParams()));
		}
		this.setStatus(Status.NOT_CHECKED);
	}
	
	public List<ReplayTransition> getTransitionList() {
		return transitionList;
	}
	
	public void setStatus(Status status) {
		if(this.status == null) {
			this.status = new SimpleObjectProperty<>();
		}
		if(!status.equals(Status.FAILED)) {
			this.error = null;
		}
		this.status.set(status);
	}
	
	public void setError(Exception e) {
		this.error = e;
	}
	
	public ObjectProperty<Status> getStatus() {
		return status;
	}
	
	public Exception getError() {
		return error;
	}
}
