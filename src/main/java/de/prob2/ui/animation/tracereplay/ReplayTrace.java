package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.inject.Injector;

import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public class ReplayTrace implements IExecutableItem, DescriptionView.Describable {
	private final ObjectProperty<Checked> status;
	private final DoubleProperty progress;
	private final ListProperty<List<Checked>> postconditionStatus;
	private final ObjectProperty<ReplayedTrace> replayedTrace;
	private final Path location; // relative to project location
	private final Path absoluteLocation;
	private BooleanProperty shouldExecute;

	private final Injector injector;

	public ReplayTrace(Path location, Path absoluteLocation, Injector injector) {
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.postconditionStatus = new SimpleListProperty<>(this, "postcondition", FXCollections.observableArrayList());
		this.replayedTrace = new SimpleObjectProperty<>(this, "replayedTrace", null);
		this.location = location;
		this.absoluteLocation = absoluteLocation;
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.injector = injector;
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return status;
	}

	@Override
	public Checked getChecked() {
		return this.status.get();
	}
	
	public void setChecked(Checked status) {
		this.status.set(status);
	}

	public ListProperty<List<Checked>> postconditionStatusProperty() {
		return postconditionStatus;
	}

	public List<List<Checked>> getPostconditionStatus() {
		return postconditionStatus.get();
	}

	public void setPostconditionStatus(List<List<Checked>> postconditionStatus) {
		this.postconditionStatus.set(FXCollections.observableArrayList(postconditionStatus));
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
	
	public ObjectProperty<ReplayedTrace> replayedTraceProperty() {
		return this.replayedTrace;
	}
	
	public ReplayedTrace getReplayedTrace() {
		return this.replayedTraceProperty().get();
	}
	
	public void setReplayedTrace(final ReplayedTrace replayedTrace) {
		this.replayedTraceProperty().set(replayedTrace);
	}
	
	public Path getLocation() {
		return this.location;
	}
	
	public Path getAbsoluteLocation() {
		return this.absoluteLocation;
	}
	
	@Override
	public void setSelected(boolean selected) {
		this.shouldExecute.set(selected);
	}
	
	@Override
	public boolean selected() {
		return shouldExecute.get();
	}
	
	@Override
	public BooleanProperty selectedProperty() {
		return shouldExecute;
	}

	@Override
	public String getName() {
		return location.getFileName().toString();
	}

	@Override
	public String getDescription() {
		TraceJsonFile trace = getTraceJsonFile();
		if(trace != null) {
			return trace.getDescription();
		} else {
			return null;
		}
	}

	@Override
	public void setDescription(String description) {
		TraceJsonFile file = getTraceJsonFile();
		if (file.getTransitionList() != null) {
			try {
				injector.getInstance(TraceFileHandler.class).save(file.changeDescription(description), this.getAbsoluteLocation());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public TraceJsonFile getTraceJsonFile() {
		return injector.getInstance(TraceFileHandler.class).loadFile(this.getAbsoluteLocation());
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
