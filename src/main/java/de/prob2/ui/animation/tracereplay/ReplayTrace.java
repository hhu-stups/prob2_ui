package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.MoreFiles;

import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace implements IExecutableItem, IValidationTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	@JsonIgnore
	private final ObjectProperty<Checked> status;
	@JsonIgnore
	private final DoubleProperty progress;
	@JsonIgnore
	private final ObjectProperty<TraceJsonFile> loadedTrace;
	@JsonIgnore
	private final ObjectProperty<ReplayedTrace> replayedTrace;
	@JsonIgnore
	private final ObjectProperty<Trace> animatedReplayedTrace;
	private final Path location; // relative to project location
	@JsonIgnore
	private Path absoluteLocation;
	@JsonIgnore
	private TraceManager traceManager;
	private final BooleanProperty selected;

	public ReplayTrace(String id, Path location, Path absoluteLocation, TraceManager traceManager) {
		this.id = id;
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.loadedTrace = new SimpleObjectProperty<>(this, "loadedTrace", null);
		this.replayedTrace = new SimpleObjectProperty<>(this, "replayedTrace", null);
		this.animatedReplayedTrace = new SimpleObjectProperty<>(this, "animatedReplayedTrace", null);
		this.location = location;
		this.absoluteLocation = absoluteLocation;
		this.traceManager = traceManager;
		this.selected = new SimpleBooleanProperty(this, "selected", true);
	}

	@JsonCreator
	private ReplayTrace(
		@JsonProperty("id") final String id,
		@JsonProperty("location") final Path location
	) {
		// absoluteLocation and traceManager must be initialized later using initAfterLoad,
		// otherwise many ReplayTrace methods won't work.
		this(id, location, null, null);
	}

	public void initAfterLoad(final Path absoluteLocation, final TraceManager traceManager) {
		this.absoluteLocation = absoluteLocation;
		this.traceManager = traceManager;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public ReplayTrace withId(final String id) {
		return new ReplayTrace(id, this.location, this.absoluteLocation, this.traceManager);
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return status;
	}

	@JsonIgnore
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

	@Override
	public void reset() {
		this.setChecked(Checked.NOT_CHECKED);
		this.setProgress(-1);
		this.loadedTrace.set(null);
		this.setReplayedTrace(null);
		this.setAnimatedReplayedTrace(null);
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
	
	public ObjectProperty<Trace> animatedReplayedTraceProperty() {
		return this.animatedReplayedTrace;
	}
	
	public Trace getAnimatedReplayedTrace() {
		return this.animatedReplayedTraceProperty().get();
	}
	
	public void setAnimatedReplayedTrace(final Trace animatedReplayedTrace) {
		this.animatedReplayedTraceProperty().set(animatedReplayedTrace);
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
	
	@JsonProperty("selected")
	@Override
	public boolean selected() {
		return selected.get();
	}
	
	@Override
	public BooleanProperty selectedProperty() {
		return selected;
	}

	@JsonIgnore
	public String getName() {
		return MoreFiles.getNameWithoutExtension(location.getFileName());
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("animation.tracereplay.type");
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		return this.getName();
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
	public boolean settingsEqual(final IExecutableItem obj) {
		if (!(obj instanceof ReplayTrace other)) {
			return false;
		}
		return Objects.equals(this.getId(), other.getId())
			&& this.getLocation().equals(other.getLocation());
	}
	
	// FIXME Is it safe to not consider id, selected, etc. in equals/hashCode? This may cause view update problems if JavaFX can't tell if fields other than location have changed.
	
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

	@Override
	@JsonIgnore
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s)", this.getClass().getSimpleName(), this.getId());
	}
	
	@Override
	public void execute(final ExecutionContext context) {
		TraceChecker.checkNoninteractive(this, context.getStateSpace());
	}
}
