package de.prob2.ui.verifications.tracereplay;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace{

	enum Status {
		SUCCESSFUL, FAILED, NOT_CHECKED
	}

	private final ObjectProperty<Status> status;
	private PersistentTrace persistentTrace;
	private Exception error;

	public ReplayTrace(PersistentTrace pTrace) {
		this.status = new SimpleObjectProperty<>(this, "status", Status.NOT_CHECKED);
		this.persistentTrace = pTrace;
		this.error = null;
		
		this.status.addListener((o, from, to) -> {
			if (to != Status.FAILED) {
				this.error = null;
			}
		});
	}

	public ReplayTrace(Trace trace) {
		this(new PersistentTrace(trace));
	}

	public PersistentTrace getStoredTrace() {
		return this.persistentTrace;
	}
	
	public void setStatus(Status status) {
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
