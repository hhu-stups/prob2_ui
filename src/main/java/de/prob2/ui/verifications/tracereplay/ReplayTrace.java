package de.prob2.ui.verifications.tracereplay;

import java.nio.file.Path;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace {
	enum Status {
		SUCCESSFUL, FAILED, NOT_CHECKED
	}

	private final ObjectProperty<Status> status;
	private final DoubleProperty progress;
	private final Path location;
	private Exception error;

	public ReplayTrace(Path location) {
		this.status = new SimpleObjectProperty<>(this, "status", Status.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.location = location;
		this.error = null;
		
		this.status.addListener((o, from, to) -> {
			if (to != Status.FAILED) {
				this.error = null;
			}
		});
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
	
	public DoubleProperty progressProperty() {
		return this.progress;
	}
	
	public double getProgress() {
		return this.progressProperty().get();
	}
	
	public void setProgress(final double progress) {
		this.progressProperty().set(progress);
		System.out.println(progress);
	}
	
	public Path getLocation() {
		return this.location;
	}
	
	public void setError(Exception e) {
		this.error = e;
	}
	
	public Exception getError() {
		return error;
	}
}
