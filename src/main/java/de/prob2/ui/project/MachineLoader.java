package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.node.AAbstractMachineParseUnit;
import de.be4.classicalb.core.parser.node.AMachineHeader;
import de.be4.classicalb.core.parser.node.AMachineMachineVariant;
import de.be4.classicalb.core.parser.node.EOF;
import de.be4.classicalb.core.parser.node.Start;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MachineLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineLoader.class);
	private static final Start EMPTY_MACHINE_AST = new Start(
		new AAbstractMachineParseUnit( // pParseUnit
			new AMachineMachineVariant(), // variant
			new AMachineHeader( // header
				Collections.singletonList(new TIdentifierLiteral("empty", 1, 9)), // name
				Collections.emptyList() // parameters
			),
			Collections.emptyList() // machineClauses
		),
		new EOF(1, 18) // eof
	);

	private final Object openLock;
	private final Api api;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final StatusBar statusBar;
	private final ReadOnlyBooleanWrapper loading;
	private StateSpace emptyStateSpace;

	@Inject
	public MachineLoader(final Api api, final CurrentProject currentProject, final StageManager stageManager,
			final CurrentTrace currentTrace, final GlobalPreferences globalPreferences, final StatusBar statusBar) {

		this.api = api;
		this.openLock = new Object();
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.statusBar = statusBar;
		this.loading = new ReadOnlyBooleanWrapper(this, "loading", false);
		this.emptyStateSpace = null;
	}

	public ReadOnlyBooleanProperty loadingProperty() {
		return loading.getReadOnlyProperty();
	}

	public boolean isLoading() {
		return this.loadingProperty().get();
	}

	public StateSpace getEmptyStateSpace() {
		synchronized (this) {
			if (this.emptyStateSpace == null) {
				try {
					this.emptyStateSpace = api.b_load(EMPTY_MACHINE_AST, this.globalPreferences);
				} catch (CliError | IOException | ModelTranslationError | ProBError e) {
					throw new IllegalStateException("Failed to load empty machine, this should never happen!", e);
				}
			}
		}
		return this.emptyStateSpace;
	}

	public void loadAsync(Machine machine, Map<String, String> pref) {
		new Thread(() -> {
			try {
				this.load(machine, pref);
			} catch (FileNotFoundException e) {
				LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
								"project.machineLoader.alerts.fileNotFound.content", getPathToMachine(machine))
						.showAndWait());
			} catch (CliError | IOException | ModelTranslationError | ProBError e) {
				LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeExceptionAlert(e, "", "project.machineLoader.alerts.couldNotOpen.content", machine.getName())
						.showAndWait());
			}
		} , "Machine Loader").start();
	}

	private void setLoadingStatus(final StatusBar.LoadingStatus loadingStatus) {
		Platform.runLater(() -> {
			this.loading.set(loadingStatus != StatusBar.LoadingStatus.NOT_LOADING);
			this.statusBar.setLoadingStatus(loadingStatus);
		});
	}

	private void load(Machine machine, Map<String, String> prefs) throws IOException, ModelTranslationError {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from loadAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			try {
				this.currentTrace.set(null);
				setLoadingStatus(StatusBar.LoadingStatus.LOADING_FILE);
				final Path path = getPathToMachine(machine);

				final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
				allPrefs.putAll(prefs);
				final StateSpace stateSpace = machine.getType().getLoader().load(api, path.toString(), allPrefs);
				setLoadingStatus(StatusBar.LoadingStatus.ADDING_ANIMATION);
				this.currentTrace.set(new Trace(stateSpace));
			} finally {
				setLoadingStatus(StatusBar.LoadingStatus.NOT_LOADING);
			}
		}
	}

	private Path getPathToMachine(Machine machine) {
		final Path path;
		if (currentProject.getMachines().contains(machine)) {
			path = currentProject.get().getLocation().resolve(machine.getPath());
		} else {
			path = machine.getPath();
		}
		return path;
	}
}
