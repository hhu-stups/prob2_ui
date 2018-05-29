package de.prob2.ui.verifications.tracereplay;

import java.nio.file.Path;
import java.util.Objects;

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
	private String errorMessage;

	public ReplayTrace(Path location) {
		this.status = new SimpleObjectProperty<>(this, "status", Status.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.location = location;
		this.errorMessage = null;
		
		this.status.addListener((o, from, to) -> {
			if (to != Status.FAILED) {
				this.errorMessage = null;
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
	}
	
	public Path getLocation() {
		return this.location;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(!(obj instanceof ReplayTrace)) {
			return false;
		}
		if(location.equals(((ReplayTrace) obj).getLocation())) {
			return true;
		}
		return false;
	}
	
	
}
