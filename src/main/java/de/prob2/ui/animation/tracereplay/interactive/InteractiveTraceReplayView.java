package de.prob2.ui.animation.tracereplay.interactive;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.MainController;
import de.prob2.ui.animation.tracereplay.ReplayedTraceRow;
import de.prob2.ui.animation.tracereplay.ReplayedTraceTable;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.error.ErrorTableView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import de.prob.check.tracereplay.interactive.InteractiveTraceReplay;
import de.prob.check.tracereplay.interactive.InteractiveReplayStep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static de.prob2.ui.internal.TranslatableAdapter.enumNameAdapter;

@Singleton
@FXMLInjected
public class InteractiveTraceReplayView extends TitledPane {

	@FXML
	private TextField fileLocationField;
	@FXML
	private Tooltip fileLocationTooltip;
	@FXML
	private Label progressLabel;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private Label warningNotFinished;
	@FXML
	private Button btnRestart, btnUndoStep, btnCurrentStep, btnFastForward, btnSkip, btnSave, btnFinish;
	@FXML
	private ReplayedTraceTable traceTable;
	@FXML
	private ErrorTableView errorTable;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final TraceFileHandler traceFileHandler;
	private final CliTaskExecutor cliExecutor;

	private final Accordion mainViewAccordion;
	private final ChangeListener<Trace> currentTraceListener;
	private InteractiveTraceReplay ireplay;
	private boolean savedTrace = false;
	private int curRowOffset = 0; // offset caused by manual animation steps

	@Inject
	public InteractiveTraceReplayView(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
	                                  final I18n i18n, final FileChooserManager fileChooserManager, final TraceFileHandler traceFileHandler,
	                                  final CliTaskExecutor cliExecutor, final StopActions stopActions, final MainController mainController) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.traceFileHandler = traceFileHandler;
		this.cliExecutor = cliExecutor;
		this.currentTraceListener = (o, oldTrace, newTrace) -> {
			savedTrace = false;
			List<Transition> newTransitions = getManuallyReplayedTransitions(newTrace);
			if (newTransitions != null) {
				Transition newTransition = newTrace.getCurrent().getTransition();
				cliExecutor.submit(() -> newTransitions.forEach(t -> {
						ireplay.performManualAnimationStep(t);
						Platform.runLater(() -> {
							traceTable.getItems().add(ireplay.getCurrentStepNr() + curRowOffset,
									new ReplayedTraceRow(ireplay.getCurrentStepNr(), "", newTransition.getPrettyRep(),
											i18n.translate("animation.tracereplay.interactive.manualPrecision"),
											"", "", Collections.singleton("manual")));
							curRowOffset++;
							updateGUIAfterExecution();
						});
					}));
			} else if (newTrace != null || ireplay.getStateSpace().isKilled()) { // trace was not an ireplay trace => restart ireplay
				restart();
			}
		};
		mainViewAccordion = mainController.getAccordionById("centerAccordion1");
		stopActions.add(this::finish);
		stageManager.loadFXML(this, "interactive_trace_replay.fxml");
	}

	@FXML
	public void initialize() {
		fileLocationTooltip.textProperty().bind(fileLocationField.textProperty());

		traceTable.setMinHeight(150);
		errorTable.setMinHeight(100);

		errorTable.visibleProperty().bind(Bindings.isNotEmpty(errorTable.getErrorItems()));
		errorTable.managedProperty().bind(errorTable.visibleProperty());
	}

	public void initializeForTrace(Path tracePath) {
		if (tracePath != null) {
			curRowOffset = 0;
			if (ireplay == null || ireplay.getStateSpace().isKilled()
					|| !tracePath.toFile().equals(ireplay.getTraceFile())) { // don't reinitialise if the same trace file is selected
				reset();
				currentTrace.addListener(currentTraceListener);
				progressBar.setVisible(true);
				btnRestart.setDisable(false);
				btnSave.setDisable(false);

				fileLocationField.setText(tracePath.toString());

				ireplay = new InteractiveTraceReplay(tracePath.toFile(), currentTrace.getStateSpace());
				progressBar.setProgress(-1);
				progressLabel.setText(i18n.translate("animation.tracereplay.loadButton.loadTrace"));
			} else {
				if (currentTrace.get().size() > 0 && !confirmTraceChange())
					return;
				ireplay.restart();
			}

			cliExecutor.submit(() -> ireplay.initialise()).whenComplete((res, exc) -> {
				if (exc == null) {
					List<InteractiveReplayStep> replaySteps = ireplay.getReplaySteps();
					Platform.runLater(() -> {
						traceTable.setItems(replaySteps.stream().map(step ->
										new ReplayedTraceRow(step.getNr(), step.getDescription(), "", "",
												createFilteredErrorMessage("",step.getErrors()), "", new ArrayList<>()) )
								.collect(Collectors.toCollection(FXCollections::observableArrayList)));
						errorTable.getErrorItems().setAll(ireplay.getErrors());
						updateGUIAfterExecution();
						currentTrace.set(ireplay.getCurrentTrace());
					});
				} else {
					stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
				}
			});
		}
	}

	public void addToMainView() {
		if (mainViewAccordion != null && !mainViewAccordion.getPanes().contains(this)) {
			mainViewAccordion.getPanes().addFirst(this);
			this.setExpanded(true);
		}
	}

	public void removeFromMainView() {
		if (mainViewAccordion != null) {
			mainViewAccordion.getPanes().remove(this);
		}
	}

	@FXML
	private void selectTraceFile() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getProB2TraceFilter());
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName());
		Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, this.getScene().getWindow());
		initializeForTrace(traceFile);
	}

	@FXML
	private void currentStep() {
		InteractiveReplayStep step = ireplay.getCurrentStep();
		cliExecutor.submit(() -> ireplay.replayCurrentStep()).whenComplete((res, exc) -> {
			if (exc == null) {
				Trace trace = ireplay.getCurrentTrace();
				Platform.runLater(() -> {
					updateReplayedTraceRow(step.getNr()-1, ireplay.getStepTransition(step.getNr()-1),
							step.getPrecision(), step.getErrors(), false); // row index is Prolog stepNr-1
					updateGUIAfterExecution();
					currentTrace.set(trace);
				});
			} else {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		});
	}

	@FXML
	private void fastForward() {
		int prevStepNr = ireplay.getCurrentStepNr();
		cliExecutor.submit(() -> ireplay.fastForward()).whenComplete((res, exc) -> {
			if (exc == null) {
				Trace trace = ireplay.getCurrentTrace();
				Platform.runLater(() -> {
					for (int i = prevStepNr; i < ireplay.getCurrentStepNr(); i++) { // don't update current row!
						InteractiveReplayStep step = ireplay.getStep(i);
						updateReplayedTraceRow(i, ireplay.getStepTransition(i), step.getPrecision(),
								step.getErrors(), false);
					}
					updateGUIAfterExecution();
					currentTrace.set(trace);
				});
			} else {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		});
	}

	@FXML
	private void skipStep() {
		updateReplayedTraceRow(ireplay.getCurrentStepNr(), null, null, new ArrayList<>(), true);
		cliExecutor.submit(() -> ireplay.skipCurrentStep()).whenComplete((res, exc) -> {
			if (exc == null) {
				Platform.runLater(this::updateGUIAfterExecution);
			} else {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		}); // no trace update
	}

	@FXML
	private void undoLastStep() {
		int prevStepNr = ireplay.getCurrentStepNr();
		int prevSize = prevStepNr + curRowOffset; // is not the trace size because of skips!
		cliExecutor.submit(() -> ireplay.undoLastStep()).whenComplete((res, exc) -> {
			if (exc == null) {
				Platform.runLater(() ->  {
					ReplayedTraceRow deleteRowCand = traceTable.getItems().get(prevSize-1);
					if (deleteRowCand != null && deleteRowCand.getStyleClasses().contains("manual")) {
						curRowOffset--; // delete manual animation row
						traceTable.getItems().remove(deleteRowCand);
					}
					updateReplayedTraceRow(prevStepNr, null, null, new ArrayList<>(), false);
					updateGUIAfterExecution();
					currentTrace.set(currentTrace.get().gotoPosition(ireplay.getCurrentTrace().size()-1));
				});
			} else {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		});
	}

	private void updateGUIAfterExecution() {
		int curStep = ireplay.getCurrentStepNr();
		int nrSteps = ireplay.getReplaySteps().size();
		if (curStep < nrSteps) {
			updateReplayedTraceRow(curStep, ireplay.getNextTransition(), ireplay.getNextTransitionPrecision(),
					ireplay.getNextTransitionErrors(), false);
		}

		progressBar.setProgress((double) curStep/nrSteps);
		progressLabel.setText(curStep + "/" + nrSteps);

		btnUndoStep.setDisable(curStep < 1);
		btnCurrentStep.setDisable(!ireplay.isReplayStepPossible() || ireplay.isFinished());
		btnFastForward.setDisable(!ireplay.isReplayStepPossible() || ireplay.isFinished());
		btnSkip.setDisable(ireplay.isFinished());
		warningNotFinished.setVisible(!ireplay.isFinished());

		//traceTable.scrollTo(curStep);
		traceTable.refresh();
	}

	private List<Transition> getManuallyReplayedTransitions(Trace trace) {
		if (trace == null || ireplay == null || trace.size() < ireplay.getCurrentTrace().size()) { // trace should extend ireplay trace
			return null;
		}
		List<Transition> ireplayTransitions = ireplay.getCurrentTrace().getTransitionList();
		List<Transition> newManualTransitions = new ArrayList<>();
		for (int i = 0; i < trace.size(); i++) {
			Transition t = trace.getTransitionList().get(i);
			if (i < ireplayTransitions.size()) {
				if (!ireplayTransitions.get(i).getId().equals(t.getId())) {
					return null; // traces do not have the same prefix
				}
			} else {
				if (i > trace.getCurrent().getIndex()) {
					break;
				}
				newManualTransitions.add(t); // add all transitions after the last replayed one (but not these after the current index of the trace)
			}
		}
		return newManualTransitions;
	}

	private void updateReplayedTraceRow(int stepNr, Transition transition, TransitionReplayPrecision precision,
	                                    List<String> errors, boolean isSkip) {
		int index = stepNr + curRowOffset;
		if (index < 0 || index >= traceTable.getItems().size()) { // = can happen when last step is undone
			return;
		}
		ReplayedTraceRow oldRow = traceTable.getItems().get(index);

		Set<String> styleClass = new HashSet<>();
		String precisionString = "";
		if (isSkip) {
			styleClass.add("skip");
		} else if (precision != null) {
			if (stepNr == ireplay.getCurrentStepNr()) {
				styleClass.add(precision == TransitionReplayPrecision.FAILED ? "not_possible" : "possible");
			} else {
				styleClass.add(precision == TransitionReplayPrecision.PRECISE ? "precise" : "imprecise");
			}
			precisionString = i18n.translate(enumNameAdapter("animation.tracereplay.replayedStatus.transitionReplayPrecision"), precision);
		}

		String transitionString = "";
		if (isSkip) {
			transitionString = "SKIP";
		} else if (transition != null) {
			transitionString = transition.getPrettyRep();
		}

		ReplayedTraceRow newRow = new ReplayedTraceRow(
				oldRow.stepProperty().getValue().intValue(),
				oldRow.fileTransitionProperty().getValue(),
				transitionString,
				precisionString,
				createFilteredErrorMessage(oldRow.errorMessageProperty().getValue(), errors),
				oldRow.styleProperty().getValue(),
				styleClass);
		traceTable.getItems().set(index, newRow);
	}

	private String createFilteredErrorMessage(String oldErrorMessage, List<String> newErrors) {
		LinkedList<String> newErrorList = new LinkedList<>(newErrors);
		if (!oldErrorMessage.isEmpty()) {
			newErrorList.removeIf(oldErrorMessage::contains);
			newErrorList.addFirst(oldErrorMessage);
		}
		return String.join("\n", newErrorList);
	}

	@FXML
	private void finish() {
		if (ireplay != null && !confirmTraceChange()) {
			return;
		}
		removeFromMainView();
		reset();
	}

	private boolean confirmTraceChange() {
		if (savedTrace) {
			return true;
		}
		final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
				"common.alerts.unsavedTraceChanges.header",
				"common.alerts.cancelTraceChanges.content");
		alert.initOwner(null);
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}

	@FXML
	private void restart() {
		initializeForTrace(ireplay.getTraceFile().toPath());
	}

	@FXML
	public void saveTrace() {
		try {
			File traceFile = ireplay.getTraceFile();
			Path saved = traceFileHandler.save(currentTrace.get(), traceFile.getParentFile().toPath(), traceFile.getName(), currentProject.getCurrentMachine());
			savedTrace = saved != null;
		} catch (IOException | RuntimeException e) {
			traceFileHandler.showSaveError(e);
		}
	}

	private void reset() {
		fileLocationField.setText("");
		ireplay = null;
		traceTable.getItems().clear();
		errorTable.getErrorItems().clear();
		btnRestart.setDisable(true);
		btnUndoStep.setDisable(true);
		btnCurrentStep.setDisable(true);
		btnFastForward.setDisable(true);
		btnSkip.setDisable(true);
		btnSave.setDisable(true);
		progressBar.setVisible(false);
		progressLabel.setText("");
		currentTrace.removeListener(currentTraceListener);
	}
}
