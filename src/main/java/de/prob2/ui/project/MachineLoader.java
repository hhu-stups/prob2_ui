package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.ReusableAnimator;
import de.prob.animator.command.GetVersionCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.model.eventb.translate.EventBFileNotFoundException;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ExtractedModel;
import de.prob.scripting.ModelFactory;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.error.WarningAlert;
import de.prob2.ui.internal.ErrorDisplayFilter;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
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

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final ErrorDisplayFilter errorDisplayFilter;
	private final StatusBar statusBar;
	private final StopActions stopActions;
	private final Injector injector;

	private final ReadOnlyBooleanWrapper loading;
	private final Object openLock;
	private final Object emptyStateSpaceLock;
	private StateSpace emptyStateSpace;
	private ReusableAnimator currentAnimator;

	@Inject
	public MachineLoader(
		final CurrentProject currentProject,
		final StageManager stageManager,
		final CurrentTrace currentTrace,
		final GlobalPreferences globalPreferences,
		final ErrorDisplayFilter errorDisplayFilter,
		final StatusBar statusBar,
		final StopActions stopActions,
		final Injector injector
	) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.errorDisplayFilter = errorDisplayFilter;
		this.statusBar = statusBar;
		this.stopActions = stopActions;
		this.injector = injector;

		this.loading = new ReadOnlyBooleanWrapper(this, "loading", false);
		this.openLock = new Object();
		this.emptyStateSpaceLock = new Object();
		this.emptyStateSpace = null;
		this.currentAnimator = null;
	}

	public ReadOnlyBooleanProperty loadingProperty() {
		return loading.getReadOnlyProperty();
	}

	public boolean isLoading() {
		return this.loadingProperty().get();
	}

	private ReusableAnimator getAnimator() {
		if (this.currentAnimator != null) {
			// Check that the existing animator is still working,
			// by executing a command that does nothing and looking for errors.
			try {
				this.currentAnimator.execute(new GetVersionCommand());
			} catch (CliError | ProBError e) {
				LOGGER.warn("Main animator is no longer working - restarting", e);
				this.currentAnimator.kill();
				this.currentAnimator = null;
			}
		}
		if (this.currentAnimator == null) {
			// Create a new animator if there is no existing one (or it was not working anymore).
			LOGGER.info("Starting a new main animator");
			this.currentAnimator = injector.getInstance(ReusableAnimator.class);
		}
		return this.currentAnimator;
	}

	private void initStateSpace(final StateSpace stateSpace, final Map<String, String> preferences) {
		stateSpace.addWarningListener(warnings -> {
			final List<ErrorItem> filteredWarnings = this.errorDisplayFilter.filterErrors(warnings);
			if (!filteredWarnings.isEmpty()) {
				Platform.runLater(() -> {
					final WarningAlert alert = injector.getInstance(WarningAlert.class);
					alert.getWarnings().setAll(filteredWarnings);
					alert.show();
				});
			}
		});
		stateSpace.changePreferences(preferences);
	}

	public StateSpace getEmptyStateSpace() {
		synchronized (this.emptyStateSpaceLock) {
			if (this.emptyStateSpace == null) {
				this.emptyStateSpace = injector.getInstance(StateSpace.class);
				initStateSpace(this.emptyStateSpace, this.globalPreferences);
				injector.getInstance(ClassicalBFactory.class)
					.create("empty", "MACHINE empty END")
					.loadIntoStateSpace(this.emptyStateSpace);
				if (Thread.currentThread().isInterrupted()) {
					this.emptyStateSpace.kill();
				}
			}
		}
		return this.emptyStateSpace;
	}
	
	/**
	 * Load the shared animators (probcli instances) used by the UI.
	 * This method is used to load the animators in the background during UI startup,
	 * so that the user can use the console or load machines more quickly
	 * than if the animators were started on demand.
	 */
	public void preloadAnimators() {
		this.getEmptyStateSpace();
		this.getAnimator();
	}

	public void loadAsync(Machine machine, Map<String, String> pref) {
		final Thread machineLoader = new Thread(() -> {
			try {
				this.load(machine, pref);
			} catch (EventBFileNotFoundException e) {
				LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
				if(!e.refreshProject()) {
					Platform.runLater(() -> stageManager
							.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
									"project.machineLoader.alerts.fileNotFound.content", e.getPath())
							.showAndWait());
				} else {
					Platform.runLater(() -> stageManager
							.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
									"project.machineLoader.alerts.eventBFileNotFound.content", e.getPath())
							.showAndWait());
				}
			} catch (FileNotFoundException e) {
				LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
								"project.machineLoader.alerts.fileNotFound.content", currentProject.get().getAbsoluteMachinePath(machine))
						.showAndWait());
			} catch (CliError | IOException | ProBError e) {
				LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeExceptionAlert(e, "", "project.machineLoader.alerts.couldNotOpen.content", machine.getName())
						.showAndWait());
			}
		}, "Machine Loader");
		this.stopActions.add(machineLoader::interrupt);
		machineLoader.start();
	}

	private void setLoadingStatus(final StatusBar.LoadingStatus loadingStatus) {
		Platform.runLater(() -> {
			this.loading.set(loadingStatus != StatusBar.LoadingStatus.NOT_LOADING);
			this.statusBar.setLoadingStatus(loadingStatus);
		});
	}

	private void loadInternal(final Machine machine, final Map<String, String> prefs) throws IOException {
		this.currentTrace.set(null);
		setLoadingStatus(StatusBar.LoadingStatus.PARSING_FILE);
		final Path path = currentProject.get().getAbsoluteMachinePath(machine);

		final ModelFactory<?> modelFactory = injector.getInstance(machine.getModelFactoryClass());
		final ExtractedModel<?> extract = modelFactory.extract(path.toString());
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		setLoadingStatus(StatusBar.LoadingStatus.STARTING_PROB_CORE);
		final ReusableAnimator animator = this.getAnimator();
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		setLoadingStatus(StatusBar.LoadingStatus.PREPARING_ANIMATOR);
		final StateSpace stateSpace = animator.createStateSpace();
		try {
			final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
			allPrefs.putAll(prefs);
			initStateSpace(stateSpace, allPrefs);
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			
			setLoadingStatus(StatusBar.LoadingStatus.LOADING_MODEL);
			extract.loadIntoStateSpace(stateSpace);
			if (Thread.currentThread().isInterrupted()) {
				stateSpace.kill();
				return;
			}
			
			setLoadingStatus(StatusBar.LoadingStatus.SETTING_CURRENT_MODEL);
			this.currentTrace.set(new Trace(stateSpace));
		} catch (RuntimeException e) {
			// Don't leave state space active if an exception was thrown before the current trace could be set.
			stateSpace.kill();
			throw e;
		}
	}

	private void load(Machine machine, Map<String, String> prefs) throws IOException {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from loadAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			try {
				loadInternal(machine, prefs);
			} finally {
				setLoadingStatus(StatusBar.LoadingStatus.NOT_LOADING);
			}
		}
	}
}
