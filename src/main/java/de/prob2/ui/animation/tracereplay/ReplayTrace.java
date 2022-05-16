package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public class ReplayTrace implements IExecutableItem, DescriptionView.Describable {
	@JsonIgnore
	private final ObjectProperty<Checked> status;
	@JsonIgnore
	private final DoubleProperty progress;
	@JsonIgnore
	private final ObjectProperty<TraceJsonFile> loadedTrace;
	@JsonIgnore
	private final ListProperty<List<Checked>> postconditionStatus;
	@JsonIgnore
	private final ObjectProperty<ReplayedTrace> replayedTrace;
	@JsonValue
	private final Path location; // relative to project location
	@JsonIgnore
	private Path absoluteLocation;
	@JsonIgnore
	private TraceManager traceManager;
	@JsonIgnore
	private BooleanProperty selected;

	public ReplayTrace(Path location, Path absoluteLocation, TraceManager traceManager) {
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.loadedTrace = new SimpleObjectProperty<>(this, "loadedTrace", null);
		this.postconditionStatus = new SimpleListProperty<>(this, "postcondition", FXCollections.observableArrayList());
		this.replayedTrace = new SimpleObjectProperty<>(this, "replayedTrace", null);
		this.location = location;
		this.absoluteLocation = absoluteLocation;
		this.traceManager = traceManager;
		this.selected = new SimpleBooleanProperty(this, "selected", true);
	}

	@JsonCreator
	public ReplayTrace(final Path location) {
		// absoluteLocation and traceManager must be initialized later using initAfterLoad,
		// otherwise many ReplayTrace methods won't work.
		this(location, null, null);
	}

	public void initAfterLoad(final Path absoluteLocation, final TraceManager traceManager) {
		this.absoluteLocation = absoluteLocation;
		this.traceManager = traceManager;
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

	public ReadOnlyObjectProperty<TraceJsonFile> loadedTraceProperty() {
		return this.loadedTrace;
	}

	/**
	 * Get the previously loaded {@link TraceJsonFile} for this trace file.
	 * This method will <em>never</em> load the trace from the file.
	 * If the trace file might have received external changes that should be respected,
	 * call {@link #load()} instead.
	 * This is especially important before modifying the file using {@link #saveModified(TraceJsonFile)}.
	 * 
	 * @return the trace data previously loaded by {@link #load()}, or {@code null} if the trace hasn't been loaded yet
	 */
	public TraceJsonFile getLoadedTrace() {
		return this.loadedTraceProperty().get();
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
		this.selected.set(selected);
	}
	
	@Override
	public boolean selected() {
		return selected.get();
	}
	
	@Override
	public BooleanProperty selectedProperty() {
		return selected;
	}

	@JsonIgnore
	@Override
	public String getName() {
		return location.getFileName().toString();
	}

	@JsonIgnore
	@Override
	public String getDescription() {
		try {
			return this.load().getDescription();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void setDescription(String description) {
		try {
			TraceJsonFile file = this.load();
			this.saveModified(file.changeDescription(description));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Read and parse the trace file into a {@link TraceJsonFile} object.
	 * The loaded trace can also be retrieved later using {@link #getLoadedTrace()}.
	 * 
	 * @return the loaded trace file
	 * @throws IOException if the trace file is missing, invalid, or otherwise couldn't be loaded
	 */
	public TraceJsonFile load() throws IOException {
		this.loadedTrace.set(this.traceManager.load(this.getAbsoluteLocation()));
		return this.getLoadedTrace();
	}
	
	/**
	 * Overwrite the trace file with the given data.
	 * This method uses an intermediate temporary file
	 * so that the existing trace file is not corrupted
	 * if the new trace data couldn't be written successfully.
	 * 
	 * @param newTrace the new trace data to be saved
	 * @throws IOException if the trace couldn't be written for any reason
	 */
	public void saveModified(final TraceJsonFile newTrace) throws IOException {
		final Path tempLocation = Paths.get(this.getAbsoluteLocation() + ".tmp");
		try {
			this.traceManager.save(tempLocation, newTrace);
			Files.move(tempLocation, this.getAbsoluteLocation(), StandardCopyOption.REPLACE_EXISTING);
			this.loadedTrace.set(newTrace);
		} catch (IOException | RuntimeException exc) {
			try {
				Files.deleteIfExists(tempLocation);
			} catch (IOException | RuntimeException innerExc) {
				exc.addSuppressed(innerExc);
			}
			throw exc;
		}
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
