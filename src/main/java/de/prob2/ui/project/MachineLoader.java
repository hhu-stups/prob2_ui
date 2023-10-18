package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.error.WarningAlert;
import de.prob2.ui.internal.ErrorDisplayFilter;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.output.PrologOutput;
import de.prob2.ui.output.PrologOutputStage;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.scene.control.ButtonType;
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
	private final PrologOutput prologOutput;
	private final StatusBar statusBar;
	private final CliTaskExecutor cliExecutor;
	private final Injector injector;

	private final ReadOnlyBooleanWrapper loading;
	private final Object openLock;
	private final Object currentAnimatorLock;
	private final ReadOnlyObjectWrapper<ReusableAnimator> currentAnimator;
	private final ReadOnlyBooleanWrapper currentAnimatorStarting;

	@Inject
	public MachineLoader(
		final CurrentProject currentProject,
		final StageManager stageManager,
		final CurrentTrace currentTrace,
		final GlobalPreferences globalPreferences,
		final ErrorDisplayFilter errorDisplayFilter,
		final PrologOutput prologOutput,
		final StatusBar statusBar,
		final CliTaskExecutor cliExecutor,
		final Injector injector
	) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.errorDisplayFilter = errorDisplayFilter;
		this.prologOutput = prologOutput;
		this.statusBar = statusBar;
		this.cliExecutor = cliExecutor;
		this.injector = injector;

		this.loading = new ReadOnlyBooleanWrapper(this, "loading", false);
		this.openLock = new Object();
		this.currentAnimatorLock = new Object();
		this.currentAnimator = new ReadOnlyObjectWrapper<>(this, "currentAnimator", null);
		this.currentAnimatorStarting = new ReadOnlyBooleanWrapper(this, "currentAnimatorStarting", false);
	}

	public ReadOnlyBooleanProperty loadingProperty() {
		return loading.getReadOnlyProperty();
	}

	public boolean isLoading() {
		return this.loadingProperty().get();
	}

	/**
	 * For internal use only by {@link PrologOutputStage}.
	 * In other code, please use {@link #getActiveStateSpace()} instead.
	 * 
	 * @return property holding the current shared animator
	 */
	public ReadOnlyObjectProperty<ReusableAnimator> currentAnimatorProperty() {
		return this.currentAnimator.getReadOnlyProperty();
	}

	public ReadOnlyBooleanProperty currentAnimatorStartingProperty() {
		return this.currentAnimatorStarting.getReadOnlyProperty();
	}

	private static void shutdownAnimator(final ReusableAnimator animator) {
		LOGGER.info("Shutting down animator: {}", animator);
		final StateSpace currentStateSpace = animator.getCurrentStateSpace();
		if (currentStateSpace != null) {
			currentStateSpace.kill();
		}
		animator.kill();
	}

	private ReusableAnimator createAnimator() {
		final ReusableAnimator animator = injector.getInstance(ReusableAnimator.class);
		animator.addWarningListener(warnings -> {
			final List<ErrorItem> filteredWarnings = this.errorDisplayFilter.filterErrors(warnings);
			if (!filteredWarnings.isEmpty()) {
				Platform.runLater(() -> {
					final WarningAlert alert = injector.getInstance(WarningAlert.class);
					alert.getWarnings().setAll(filteredWarnings);
					alert.show();
				});
			}
		});
		animator.addConsoleOutputListener(prologOutput.getOutputListener());
		return animator;
	}

	/**
	 * Get the single shared animator instance used by the UI.
	 * If no shared animator instance has been started yet or it is no longer working,
	 * a new animator is started and saved as the shared animator.
	 * 
	 * @return the single shared animator instance
	 */
	private ReusableAnimator getAnimator() {
		synchronized (this.currentAnimatorLock) {
			if (this.currentAnimator.get() != null) {
				// Check that the existing animator is still working,
				// by executing a command that does nothing and looking for errors.
				try {
					this.currentAnimator.get().execute(new GetVersionCommand());
				} catch (CliError | ProBError e) {
					LOGGER.warn("Main animator is no longer working - restarting", e);
					shutdownAnimator(this.currentAnimator.get());
					this.currentAnimator.set(null);
				}
			}
			if (this.currentAnimator.get() == null) {
				// Create a new animator if there is no existing one (or it was not working anymore).
				LOGGER.info("Starting a new main animator");
				this.currentAnimatorStarting.set(true);
				try {
					this.currentAnimator.set(createAnimator());
				} finally {
					this.currentAnimatorStarting.set(false);
				}
			}
			return this.currentAnimator.get();
		}
	}

	/**
	 * Create a new state space based on the shared animator.
	 * The shared animator is automatically started if necessary (see {@link #getAnimator()}).
	 * If another state space based on the shared animator is still running,
	 * it is automatically killed before the new one is created.
	 * 
	 * @return a new state space based on the shared animator
	 */
	private StateSpace createNewStateSpace() {
		synchronized (this.currentAnimatorLock) {
			final StateSpace currentStateSpace = this.getAnimator().getCurrentStateSpace();
			if (currentStateSpace != null) {
				currentStateSpace.kill();
			}
			this.getAnimator().resetProB();
			return this.getAnimator().createStateSpace();
		}
	}

	/**
	 * <p>
	 * Get the shared animator's currently active state space,
	 * or if none is active,
	 * a new state space with an empty machine.
	 * The shared animator is automatically started if necessary (see {@link #getAnimator()}).
	 * </p>
	 * <p>
	 * This method is meant for use by code that needs an animator in which it can execute commands or evaluate formulas,
	 * but doesn't care about the loaded machine or other animator state.
	 * This is useful for getting constant information from probcli,
	 * such as the version or preference information.
	 * </p>
	 * 
	 * @return the shared animator's currently active state space, or an empty one
	 */
	public StateSpace getActiveStateSpace() {
		synchronized (this.currentAnimatorLock) {
			final StateSpace currentStateSpace = this.getAnimator().getCurrentStateSpace();
			if (currentStateSpace == null || currentStateSpace.isKilled()) {
				final StateSpace stateSpace = this.createNewStateSpace();
				stateSpace.changePreferences(this.globalPreferences);
				injector.getInstance(ClassicalBFactory.class)
					.create("empty", "MACHINE empty END")
					.loadIntoStateSpace(stateSpace);
				if (Thread.currentThread().isInterrupted()) {
					stateSpace.kill();
				}
				return stateSpace;
			} else {
				return currentStateSpace;
			}
		}
	}
	
	/**
	 * Load the shared animator (probcli instance) used by the UI.
	 * This method is used to load the animator in the background during UI startup,
	 * so that the user can use the console or load machines more quickly
	 * than if the animator would be started on demand.
	 */
	public void startSharedAnimator() {
		this.getActiveStateSpace();
	}

	/**
	 * Shut down the current shared animator (probcli instance), if any.
	 * This method normally should not be used.
	 * It is only meant for use by {@link PrologOutputStage},
	 * to allow the user to manually shut down probcli in case something went wrong.
	 */
	public void shutdownSharedAnimator() {
		synchronized (this.currentAnimatorLock) {
			if (this.currentAnimator.get() != null) {
				shutdownAnimator(this.currentAnimator.get());
				this.currentAnimator.set(null);
			}
		}
	}

	public CompletableFuture<?> loadAsync(Machine machine, Map<String, String> pref) {
		return this.cliExecutor.submit(() -> {
			this.load(machine, pref);
			return null;
		}).whenComplete((r, e) -> {
			if (e != null) {
				LOGGER.error("Exception while loading machine {}", machine.getName());
				if (e instanceof EventBFileNotFoundException exc) {
					if(!exc.refreshProject()) {
						// the source file (e.g. .bum) does not exist
						showAlert(exc, machine);
					} else {
						Platform.runLater(() -> stageManager
							.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
								"project.machineLoader.alerts.eventBFileNotFound.content", exc.getLocalizedMessage(), machine.getName())
							.showAndWait());
					}
				} else if (TraceFileHandler.isFileNotFound(e)) {
					LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
					showAlert(e, machine);
				} else {
					LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
					Platform.runLater(() -> stageManager
						.makeExceptionAlert(e, "", "project.machineLoader.alerts.couldNotOpen.content", machine.getName())
						.showAndWait());
				}
			}
		});
	}

	private void showAlert(Throwable exception, Machine machine) {
		Platform.runLater(() -> {
			List<ButtonType> buttons = new ArrayList<>();
			buttons.add(ButtonType.YES);
			buttons.add(ButtonType.NO);
			Alert alert =
				stageManager.makeAlert(AlertType.ERROR, buttons,
					"project.machineLoader.alerts.fileNotFound.header",
					"project.machineLoader.alerts.fileNotFound.content",
					exception.getLocalizedMessage(),
					machine.getName());
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				currentProject.removeMachine(machine);
			}
		});
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

		setLoadingStatus(StatusBar.LoadingStatus.STARTING_ANIMATOR);
		final StateSpace stateSpace = this.createNewStateSpace();
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		try {
			final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
			allPrefs.putAll(prefs);
			stateSpace.changePreferences(allPrefs);
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
