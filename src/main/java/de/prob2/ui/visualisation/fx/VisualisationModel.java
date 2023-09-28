package de.prob2.ui.visualisation.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hhu.stups.prob.translator.BValue;
import de.hhu.stups.prob.translator.exceptions.TranslationException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.fx.exception.VisualisationParseException;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualisationModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationModel.class);

	private final CurrentTrace currentTrace;
	private final StageManager stageManager;

	private Trace oldTrace;
	private Trace newTrace;
	private Map<String, Object> valueCache;
	private Map<String, IEvalElement> parsedFormulas;
	private boolean randomExecution;
	private AbstractModel model;

	VisualisationModel(CurrentTrace currentTrace, StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.parsedFormulas = new HashMap<>();
	}

	/**
	 * Sets the new and the old trace.
	 *
	 * @param oldTrace Trace before the last executed transition.
	 * @param newTrace Current trace.
	 */
	void setTraces(Trace oldTrace, Trace newTrace) {
		this.oldTrace = oldTrace;
		this.newTrace = newTrace;
		this.valueCache = new HashMap<>();
	}

	/**
	 * The methods checks if the value of the given formulas were changed through the last transition.
	 * @param formulasParam Formulas that could have changed their values.
	 * @return Returns true if the value has changed.
	 */
	List<String> hasChanged(List<String> formulasParam) throws VisualisationParseException {
		List<String> formulas = new ArrayList<>(formulasParam);
		// parse formulas into IEvalElements
		List<IEvalElement> parsedFormulas = new ArrayList<>(formulas.size());
		for (String formula: formulas) {
			parsedFormulas.add(getParsedFormula(formula));
		}
		LOGGER.debug("Look up if the formulas {} have changed their values.", String.join(" ", formulas));
		if (oldTrace == null) {
			if (newTrace != null) {
				LOGGER.debug("The old trace is null, so the values of the formulas have changed.");
				return formulas;
			} else {
				LOGGER.debug("The old trace is null and also the new trace is null, so the values of the formulas don't have changed.");
				return new ArrayList<>();
			}
		}
		List<AbstractEvalResult> oldAbstractValues = oldTrace.getCurrentState().eval(parsedFormulas);
		List<AbstractEvalResult> newAbstractValues = newTrace.getCurrentState().eval(parsedFormulas);
		List<String> changedFormulas = new ArrayList<>(formulas.size());
		for (int i = 0; i < formulas.size(); i++) {
			String formula = formulas.get(i);
			AbstractEvalResult newAbstractValue = newAbstractValues.get(i);
			EvalResult newValue = null;
			if (newAbstractValue instanceof EvalResult) {
				newValue = (EvalResult) newAbstractValue;
				//cache value so we don't have to eval it again
				try {
					BValue translatedValue = newValue.translate().getValue();
					valueCache.put(formula, translatedValue);
				} catch (TranslationException e) {
					LOGGER.error("Error while translating the value of the formula \"{}\".", formula, e);
				}
			} else {
				LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the new trace." +
						" Considering its value has not changed.", formula);
				continue;
			}
			AbstractEvalResult oldAbstractValue = oldAbstractValues.get(i);
			EvalResult oldValue = null;
			if (oldAbstractValue instanceof EvalResult) {
				oldValue = (EvalResult) oldAbstractValue;
			} else {
				LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the old trace, but in the new trace." +
						" Considering its value has changed.", formula);
				changedFormulas.add(formula);
				continue;
			}
			if (!oldValue.getValue().equals(newValue.getValue())) {
				LOGGER.debug("The value of formula \"{}\" has changed.", formula);
				changedFormulas.add(formula);
			}
		}
		return changedFormulas;
	}

	/**
	 * Gets the expanded and translated value of the given formula.
	 *
	 * @param formula Formula to evaluate.
	 * @return Returns the expanded and translated value of the formula. If the formula can't be evaluated the method
	 *         will return {@code null}.
	 */
	public Object getValue(String formula) {
		LOGGER.debug("Get value for formula \"{}\".", formula);
		if (valueCache.containsKey(formula)) {
			LOGGER.debug("Using cache to get value of formula \"{}\".", formula);
			return valueCache.get(formula);
		}
		LOGGER.debug("Eval trace to get value of formula \"{}\".", formula);
		return evalCurrent(formula);
	}

	/**
	 * Evaluates the given formula, but not on the current state.
	 * It tries to evaluate it on the previous state of the current trace.
	 *
	 * @param formula Formula to evaluate.
	 * @return The value of the formula or {@code null}.
	 */
	public Object getPreviousValue(String formula) {
		LOGGER.debug("Try to get previous value of formula \"{}\".", formula);
		if (newTrace.getPreviousState() != null) {
			try {
				EvalResult value = evalState(newTrace.getPreviousState(), formula);
				LOGGER.debug("Evaluated previous value of formula \"{}\" and got the result: {}", formula, value);
				return value != null ? value.translate().getValue() : null;
			} catch (Exception e) {
				LOGGER.debug("Exception while trying to get the value for formula \"{}\" out of the map. Returning null.", formula);
				return null;
			}
		}
		LOGGER.debug("The previous state is null. Returning null.");
		return null;
	}

	/**
	 * Tries to execute the given event with the given predicates.
	 *
	 * @param event Name of the event
	 * @param predicates Optional predicates of the event
	 * @return Returns a boolean indicating whether the event was executed or not.
	 */
	public boolean executeEvent(String event, String... predicates) {
		Trace currentTrace = this.currentTrace.get();
		LOGGER.debug("Try to execute event \"{}\" with predicates: {}", event, predicates);
		if (currentTrace.canExecuteEvent(event, predicates)) {
			LOGGER.debug("Event \"{}\" is executable. Execute it.", event);
			Trace resultTrace = currentTrace.execute(event, predicates);
			this.currentTrace.set(resultTrace);
			return true;
		}
		LOGGER.debug("Event \"{}\" is not executable.", event);
		return false;
	}

	/**
	 * Tries to randomly execute the given number of events.
	 *
	 * @param number Number of events
	 * @param time time in millis between two event-executions
	 * @param stopOnInvariantViolation indicates if further events should be executed when an invariant is violated
	 * @param afterExecution handler to react when the task ended (if task has succeeded, failed or was canceled)
	 */
	public void executeRandomEvents(int number, long time, boolean stopOnInvariantViolation,
									EventHandler<WorkerStateEvent> afterExecution) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				randomExecution = true;
				boolean outgoingTransitions = true;
				boolean invariantViolated = false;
				for (int i = 0; i < number && outgoingTransitions && !invariantViolated && randomExecution; i++) {
					currentTrace.set(currentTrace.get().randomAnimation(1));
					outgoingTransitions = !currentTrace.getCurrentState().getOutTransitions().isEmpty();
					if (stopOnInvariantViolation) {
						invariantViolated = !currentTrace.getCurrentState().isInvariantOk();
					}
					Thread.sleep(time);
				}
				return null;
			}
		};
		if (afterExecution != null) {
			task.setOnCancelled(afterExecution);
			task.setOnFailed(afterExecution);
			task.setOnSucceeded(afterExecution);
		}
		Thread thread = new Thread(task, "Random Event Execution Thread");
		thread.start();
	}

	/**
	 * Stops the random execution of events.
	 */
	public void stopRandomExecution() {
		randomExecution = false;
	}

	private IEvalElement getParsedFormula(String formula) throws VisualisationParseException {
		LOGGER.debug("Try to parse formula \"{}\"", formula);
		try {
			if (!newTrace.getModel().equals(model)) {
				LOGGER.debug("The madel has been changed, so clear the map of parsed formulas!");
				model = newTrace.getModel();
				parsedFormulas.clear();
			}
			if (parsedFormulas.containsKey(formula)) {
				LOGGER.debug("The formula is already parsed.");
				return parsedFormulas.get(formula);
			}
			IEvalElement parsedFormula = model.parseFormula(formula, FormulaExpand.EXPAND);
			parsedFormulas.put(formula, parsedFormula);
			return parsedFormula;
		} catch (Exception e) {
			LOGGER.warn("Could not parse formula \"{}\".", formula, e);
			throw new VisualisationParseException(formula, e);
		}
	}

	private Object evalCurrent(String formula) {
		EvalResult value = evalState(newTrace.getCurrentState(), formula);
		if (value == null) {
			return null;
		} else {
			try {
				return value.translate().getValue();
			} catch (Exception e) {
				LOGGER.warn("Eval current failed, returning null.", e);
				return null;
			}
		}
	}

	private EvalResult evalState(State state, String formula) {
		LOGGER.debug("Try to evaluate formula {}.", formula);
		try {
			IEvalElement parsedFormula = getParsedFormula(formula);
			AbstractEvalResult evalResult = state.eval(parsedFormula);
			LOGGER.debug("Evaluated formula \"{}\" and got the result: {}", formula, evalResult);
			if (evalResult instanceof EvalResult) {
				return (EvalResult) evalResult;
			}
			return null;
		} catch (EvaluationException | VisualisationParseException evalException) {
			Alert alert = stageManager.makeExceptionAlert(evalException, "", "visualisation.fx.model.alerts.evaluationException.content", formula);
			alert.show();
			LOGGER.warn("EvaluationException while evaluating the formula \"" + formula +"\".", evalException);
			return null;
		}
	}
}
