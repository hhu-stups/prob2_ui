package de.prob2.ui.verifications.tracereplay;

import java.util.ArrayList;
import java.util.List;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace{

	enum Status {
		SUCCESSFUL, FAILED, NOT_CHECKED
	}

	private transient ObjectProperty<Status> status;
	private final List<ReplayTraceTransition> transitionList = new ArrayList<>();
	private transient Exception error;

	public ReplayTrace(Trace trace) {

		for (Transition transition : trace.getTransitionList()) {
			transitionList.add(new ReplayTraceTransition(transition));
		}
		this.setStatus(Status.NOT_CHECKED);
	}

	public List<ReplayTraceTransition> getTransitionList() {
		return transitionList;
	}

	public void setStatus(Status status) {
		if (this.status == null) {
			this.status = new SimpleObjectProperty<>();
		}
		if (!status.equals(Status.FAILED)) {
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
