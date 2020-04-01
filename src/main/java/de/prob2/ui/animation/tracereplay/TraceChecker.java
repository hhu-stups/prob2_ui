package de.prob2.ui.animation.tracereplay;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.hhu.stups.prob.translator.BValue;
import de.hhu.stups.prob.translator.Translator;
import de.hhu.stups.prob.translator.exceptions.TranslationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.tracediff.TraceDiffStage;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

@FXMLInjected
@Singleton
public class TraceChecker {

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final Injector injector, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
	}

	void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> replayTrace(trace, false));
	}

	void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		this.replayTrace(replayTrace, setCurrentAnimation);
	}

	private void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		if(!replayTrace.selected()) {
			return;
		}
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		if(persistentTrace == null) {
			return;
		}

		replayTrace.setChecked(Checked.NOT_CHECKED);
		Thread replayThread = new Thread(() -> {
			StateSpace stateSpace = currentTrace.getStateSpace();
			Trace trace = new Trace(stateSpace);
			trace.setExploreStateByDefault(false);
			Checked status = Checked.SUCCESS;
			final List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
			for (int i = 0; i < transitionList.size(); i++) {
				final int finalI = i;
				Platform.runLater(() -> replayTrace.setProgress((double) finalI / transitionList.size()));
				Transition trans = replayPersistentTransition(replayTrace, trace, transitionList.get(i),
						setCurrentAnimation);
				if (trans != null) {
					trace = trace.add(trans);
				} else {
					status = Checked.FAIL;
					break;
				}

				if (Thread.currentThread().isInterrupted()) {
					currentJobThreads.remove(Thread.currentThread());
					return;
				}
			}

			final Checked finalStatus = status;
			Platform.runLater(() -> {
				replayTrace.setChecked(finalStatus);
				replayTrace.setProgress(-1);
			});
			trace.setExploreStateByDefault(true);
			if (setCurrentAnimation) {
				// set the current trace if no error has occured. Otherwise leave the
				trace.getCurrentState().explore();
				final Trace copyTrace = trace;
				if (replayTrace.getErrorMessageBundleKey() != null) {
					Platform.runLater(() -> {
						TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, replayTrace.getErrorMessageBundleKey(), replayTrace.getErrorMessageParams());
						alert.initOwner(stageManager.getCurrent());
						boolean isEqual = currentTrace.get().getTransitionList().equals(copyTrace.getTransitionList());
						alert.setErrorMessage(isEqual, copyTrace.getTransitionList().size(), persistentTrace.getTransitionList().size());
						setAlertButtons(isEqual, copyTrace, persistentTrace, alert);
					});
				} else {
					currentTrace.set(trace);
				}

			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Trace Replay Thread");
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	private void setAlertButtons(boolean isEqual, Trace copyTrace, PersistentTrace persistentTrace, Alert alert) {
		if (isEqual) {
			alert.getButtonTypes().addAll(ButtonType.OK);
			alert.showAndWait();
		} else {
			ButtonType traceDiffButton = new ButtonType(injector.getInstance(ResourceBundle.class).getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff"));
			alert.getButtonTypes().addAll(traceDiffButton, ButtonType.YES, ButtonType.NO);
			handleAlert(alert, copyTrace, persistentTrace, traceDiffButton);
		}
	}

	public void handleAlert(Alert alert, Trace copyTrace, PersistentTrace persistentTrace, ButtonType traceDiffButton) {
		injector.getInstance(TraceDiffStage.class).close();
		Optional<ButtonType> type = alert.showAndWait();
		if (type.get() == ButtonType.YES) {
			currentTrace.set(copyTrace);
		} else if (type.get() == traceDiffButton) {
			TraceDiffStage traceDiffStage = injector.getInstance(TraceDiffStage.class);
			traceDiffStage.setAlert(alert);
			traceDiffStage.setLists(copyTrace, persistentTrace, currentTrace.get());
			traceDiffStage.show();
			alert.close();
		}
	}

	private Transition replayPersistentTransition(ReplayTrace replayTrace, Trace t,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		StateSpace stateSpace = t.getStateSpace();
		PredicateBuilder predicateBuilder = new PredicateBuilder().addMap(persistentTransition.getParameters());
		predicateBuilder.addMap(persistentTransition.getDestinationStateVariables());
		final IEvalElement pred = stateSpace.getModel().parseFormula(predicateBuilder.toString(), FormulaExpand.EXPAND);
		final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
				t.getCurrentState().getId(), persistentTransition.getOperationName(), pred, 1);
		stateSpace.execute(command);
		if (command.hasErrors()) {
			replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage");
			replayTrace.setErrorMessageParams(persistentTransition.getOperationName(), predicateBuilder, String.join(", ", command.getErrors()));
			return null;
		}
		List<Transition> possibleTransitions = command.getNewTransitions();
		if (possibleTransitions.isEmpty()) {
			replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage.operationNotPossible");
			replayTrace.setErrorMessageParams(persistentTransition.getOperationName(), predicateBuilder);
			return null;
		}
		Transition trans = possibleTransitions.get(0);
		if (!checkOutputParams(replayTrace, trans, persistentTransition, setCurrentAnimation)) {
			return null;
		}
		return trans;
	}

	private boolean checkOutputParams(ReplayTrace replayTrace, Transition trans,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		String operationName = trans.getName();
		OperationInfo machineOperationInfo = trans.getStateSpace().getLoadedMachine().getMachineOperationInfo(operationName);
		final Map<String, String> ouputParameters = persistentTransition.getOuputParameters();
		if (machineOperationInfo != null && ouputParameters != null) {
			List<String> outputParameterNames = machineOperationInfo.getOutputParameterNames();
			try {
				List<BValue> translatedReturnValues = trans.getTranslatedReturnValues();
				for (int i = 0; i < outputParameterNames.size(); i++) {
					String outputParamName = outputParameterNames.get(i);
					BValue paramValueFromTransition = translatedReturnValues.get(i);
					if (ouputParameters.containsKey(outputParamName)) {
						String stringValue = ouputParameters.get(outputParamName);
						BValue bValue = Translator.translate(stringValue);
						if (!bValue.equals(paramValueFromTransition)) {
							// do we need further checks here?
							// because the value translator does not
							// support enum values properly
							if (setCurrentAnimation) {
								replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage.mismatchingOutputValues");
								replayTrace.setErrorMessageParams(operationName, outputParamName, bValue.toString(), paramValueFromTransition);
							}
							return false;
						}
					}

				}
			} catch (TranslationException e) {
				Platform.runLater(
						() -> stageManager
								.makeExceptionAlert(e,
										"animation.tracereplay.alerts.traceReplayError.header",
										"animation.tracereplay.alerts.traceReplayError.content")
								.showAndWait());
				return false;
			}
		}
		return true;
	}

	void cancelReplay() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobThreads.clear();
	}

	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
}
