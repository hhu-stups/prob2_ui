package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.io.MoreFiles;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ICheckingResult;
import de.prob2.ui.verifications.ICliTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"location",
	"selected",
})
public final class ReplayTrace extends AbstractCheckableItem implements ICliTask {
	public static final class Result implements ICheckingResult {
		private final ReplayedTrace replayed;
		private final Trace trace;
		private final Trace traceWithSkips;

		public Result(ReplayedTrace replayed, Trace trace, Trace traceWithSkips) {
			this.replayed = Objects.requireNonNull(replayed, "replayed");
			this.trace = Objects.requireNonNull(trace, "trace");
			this.traceWithSkips = Objects.requireNonNull(traceWithSkips, "traceWithSkips");
		}

		public ReplayedTrace getReplayed() {
			return this.replayed;
		}

		public Trace getTraceWithSkips() {
			return this.traceWithSkips;
		}

		@Override
		public CheckingStatus getStatus() {
			return this.getReplayed().getErrors().isEmpty() ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
		}

		@Override
		public List<Trace> getTraces() {
			return Collections.singletonList(this.trace);
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ReplayTrace.class);

	@JsonIgnore
	private final ObjectProperty<TraceJsonFile> loadedTrace;
	private final Path location; // relative to project location
	@JsonIgnore
	private Path absoluteLocation;
	@JsonIgnore
	private TraceManager traceManager;

	public ReplayTrace(String id, Path location, Path absoluteLocation, TraceManager traceManager) {
		super(id);

		this.loadedTrace = new SimpleObjectProperty<>(this, "loadedTrace", null);
		this.location = Objects.requireNonNull(location, "location");
		this.absoluteLocation = absoluteLocation;
		this.traceManager = traceManager;
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
	public ValidationTaskType<ReplayTrace> getTaskType() {
		return BuiltinValidationTaskTypes.REPLAY_TRACE;
	}

	public ReplayTrace withId(final String id) {
		return new ReplayTrace(id, this.location, this.absoluteLocation, this.traceManager);
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

	public Path getLocation() {
		return this.location;
	}

	public Path getAbsoluteLocation() {
		return this.absoluteLocation;
	}

	@JsonIgnore
	public String getName() {
		return MoreFiles.getNameWithoutExtension(this.location);
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
		this.traceManager.save(this.getAbsoluteLocation(), newTrace);
		this.loadedTrace.set(newTrace);
	}

	private void executeInternal(ExecutionContext context) {
		ReplayedTrace replayed = TraceReplay.replayTraceFile(context.stateSpace(), this.getAbsoluteLocation());
		List<ErrorItem> errors = replayed.getErrors();
		if (errors.isEmpty() && replayed.getReplayStatus() != TraceReplayStatus.PERFECT) {
			// FIXME Should this case be reported as an error on the Prolog side?
			final ErrorItem error = new ErrorItem("Trace could not be replayed completely", ErrorItem.Type.ERROR, Collections.emptyList());
			errors = new ArrayList<>(errors);
			errors.add(error);
		}
		replayed = replayed.withErrors(errors);
		Trace trace = replayed.getTrace(context.stateSpace());
		Trace traceWithSkips = replayed.getTraceWithSkips(context.stateSpace());
		this.setResult(new ReplayTrace.Result(replayed, trace, traceWithSkips));
	}

	@Override
	public void execute(final ExecutionContext context) {
		try {
			this.reset();
			this.setResult(new CheckingResult(CheckingStatus.IN_PROGRESS));
			this.executeInternal(context);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Trace check interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED));
		} catch (RuntimeException exc) {
			this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", exc.toString()));
			throw exc;
		}
	}

	@Override
	public void reset() {
		super.reset();
		this.loadedTrace.set(null);
	}

	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof ReplayTrace that
			&& Objects.equals(this.getLocation(), that.getLocation());
	}

	@Override
	public ReplayTrace copy() {
		return new ReplayTrace(this.getId(), this.location);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("location", this.getLocation())
			       .toString();
	}
}
