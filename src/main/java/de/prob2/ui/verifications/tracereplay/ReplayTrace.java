package de.prob2.ui.verifications.tracereplay;

import java.nio.file.Path;
import java.util.Objects;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace implements IExecutableItem {

	private final ObjectProperty<Checked> status;
	private final DoubleProperty progress;
	private final Path location;
	private String errorMessage;
	private BooleanProperty shouldExecute;

	public ReplayTrace(Path location) {
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.location = location;
		this.errorMessage = null;
		this.shouldExecute = new SimpleBooleanProperty(true);
		
		this.status.addListener((o, from, to) -> {
			if (to != Checked.FAIL) {
				this.errorMessage = null;
			}
		});
	}

	public ObjectProperty<Checked> statusProperty() {
		return status;
	}
	
	@Override
	public Checked getChecked() {
		return this.status.get();
	}
	
	public void setChecked(Checked status) {
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
	public void setShouldExecute(boolean shouldExecute) {
		this.shouldExecute.set(shouldExecute);
	}
	
	@Override
	public boolean shouldExecute() {
		return shouldExecute.get();
	}
	
	@Override
	public BooleanProperty shouldExecuteProperty() {
		return shouldExecute;
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
		return location.equals(((ReplayTrace) obj).getLocation());
	}
	
	
}
