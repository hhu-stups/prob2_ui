package de.prob2.ui.verifications.tracereplay;

import java.io.File;

import de.prob.check.tracereplay.PersistentTrace;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace {
	enum Status {
		SUCCESSFUL, FAILED, NOT_CHECKED
	}

	private final ObjectProperty<Status> status;
	private final IntegerProperty progress;
	private final File location;
	private final PersistentTrace persistentTrace;
	private Exception error;

	public ReplayTrace(File location, PersistentTrace pTrace) {
		this.status = new SimpleObjectProperty<>(this, "status", Status.NOT_CHECKED);
		this.progress = new SimpleIntegerProperty(this, "progress", -1);
		this.location = location;
		this.persistentTrace = pTrace;
		this.error = null;
		
		this.status.addListener((o, from, to) -> {
			if (to != Status.FAILED) {
				this.error = null;
			}
		});
	}

	public PersistentTrace getStoredTrace() {
		return this.persistentTrace;
	}
	
	public ObjectProperty<Status> statusProperty() {
		return status;
	}
	
	public Status getStatus() {
		return this.status.get();
	}
	
	public void setStatus(Status status) {
		this.status.set(status);
	}
	
	public IntegerProperty progressProperty() {
		return this.progress;
	}
	
	public int getProgress() {
		return this.progressProperty().get();
	}
	
	public void setProgress(final int progress) {
		this.progressProperty().set(progress);
	}
	
	public File getLocation() {
		return this.location;
	}
	
	public void setError(Exception e) {
		this.error = e;
	}
	
	public Exception getError() {
		return error;
	}
}
