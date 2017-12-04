package de.prob2.ui.verifications.tracereplay;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace{

	enum Status {
		SUCCESSFUL, FAILED, NOT_CHECKED
	}

	private ObjectProperty<Status> status;
	private PersistentTrace persistentTrace;
	private Exception error;

	public ReplayTrace(Trace trace) {
		this.persistentTrace = new PersistentTrace(trace);
		this.setStatus(Status.NOT_CHECKED);
	}

	public ReplayTrace(PersistentTrace pTrace) {
		this.persistentTrace = pTrace;
		this.setStatus(Status.NOT_CHECKED);
	}

	public PersistentTrace getStoredTrace() {
		return this.persistentTrace;
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
