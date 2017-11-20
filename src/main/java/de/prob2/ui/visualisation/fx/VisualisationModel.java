package de.prob2.ui.visualisation.fx;

import de.prob.animator.domainobjects.*;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.codehaus.groovy.util.HashCodeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Christoph Heinzen
 * @since 25.09.17
 */
public class VisualisationModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationModel.class);

	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	private Trace oldTrace;
	private Trace newTrace;
	private Map<String, EvalResult> oldStringToResult;
	private Map<String, EvalResult> newStringToResult;

	public VisualisationModel(CurrentTrace currentTrace, StageManager stageManager, ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	/**
	 * Sets the new and the old trace.
	 *
	 * @param oldTrace Trace before the last executed transition.
	 * @param newTrace Current trace.
	 */
	public void setTraces(Trace oldTrace, Trace newTrace) {
		this.oldTrace = oldTrace;
		this.newTrace = newTrace;
		if (oldTrace != null) {
			oldStringToResult = translateValuesMap( oldTrace.getCurrentState().getValues());
		} else {
			oldStringToResult = new HashMap<>();
		}
		if (newTrace != null) {
			newStringToResult = translateValuesMap( newTrace.getCurrentState().getValues());
		} else {
			newStringToResult = new HashMap<>();
		}
	}

	/**
	 * The methods checks if the value of the given formula was changed through the last transition.
	 * @param formula Formula that could have changed.
	 * @return Returns true if the value has changed.
	 */
	public boolean hasChanged(String formula) {
		LOGGER.debug("Look up if the formula \"{}\" has changed its value.", formula);
		if (oldTrace == null) {
			LOGGER.debug("The old trace is null, so the value of formula \"{}\" has changed.", formula);
			return newTrace != null;
		}

		EvalResult oldValue;
		EvalResult newValue;
		boolean oldValueFromMap = false;
		boolean newValueFromMap = false;

		if (oldStringToResult.containsKey(formula)) {
			oldValue = oldStringToResult.get(formula);
			oldValueFromMap = true;
		} else {
			oldValue = evalState(oldTrace.getCurrentState(), formula);
		}

		if (newStringToResult.containsKey(formula)) {
			newValue = newStringToResult.get(formula);
			newValueFromMap = true;
		} else {
			newValue = evalState(newTrace.getCurrentState(), formula);
		}

		if (newValue == null) {
			LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the new trace. Returning false.", formula);
			return false;
		}
		if (oldValue == null) {
			LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the old trace, but in the new trace. Returning true.", formula);
			return true;
		}

		LOGGER.debug("The value of formula \"{}\" could be evaluated for the new and the old trace.", formula);
		if ((oldValue.getValue().equals(newValue.getValue()) && newValueFromMap && oldValueFromMap) ||
				(newValueFromMap ^ oldValueFromMap)) {
			/*first case: if the new and the oldValue are both from the maps and equal, it could happen that they are only
						  equal because their short representations are equal and their expanded may be unequal
			  second case: if only one value is from a map and the other one is evaluated, we would compare a short
			               representation to an expanded one*/
			oldValue = evalState(oldTrace.getCurrentState(), formula);
			newValue = evalState(newTrace.getCurrentState(), formula);
			if (newValue == null) {
				LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the new trace. Returning false.", formula);
				return false;
			}
			if (oldValue == null) {
				LOGGER.debug("The value of formula \"{}\" couldn't be evaluated in the old trace, but in the new trace. Returning true.", formula);
				return true;
			}
		}

		return !oldValue.getValue().equals(newValue.getValue());
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
		if (newStringToResult.containsKey(formula)) {
			LOGGER.debug("Using map to get value of formula \"{}\".", formula);
			try {
				EvalResult value = newStringToResult.get(formula);
				TranslatedEvalResult translatedValue = value.translate();
				return translatedValue.getValue();
			} catch (Exception  e) {
				LOGGER.debug("Exception while trying to get the value for formula \"{}\" out of the map. Try to eval it.", formula);
				return evalCurrent(formula);
			}
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
		LOGGER.debug("The previous state is null. Returning null");
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
		LOGGER.debug("Try to execute event \"{}\" with predicates: {}.", event, String.join(" ", predicates));
		if (currentTrace.canExecuteEvent(event, predicates)) {
			LOGGER.debug("Event \"{}\" is executable. Execute it.");
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
	 */
	public void executeRandomEvents(int number) {
		currentTrace.set(currentTrace.get().randomAnimation(number));
	}

	private Map<String, EvalResult> translateValuesMap(Map<IEvalElement, AbstractEvalResult> values) {
		return values.keySet().stream()
				.filter(element -> element instanceof AbstractEvalElement)
				.map(element -> (AbstractEvalElement) element)
				.filter(element -> values.get(element) instanceof EvalResult)
				.collect(Collectors.toMap(AbstractEvalElement::getCode, element -> (EvalResult) values.get(element)));
	}

	private Object evalCurrent(String formula) {
		EvalResult value = evalState(newTrace.getCurrentState(), formula);
		if (value == null) {
			return null;
		} else {
			try {
				return value.translate().getValue();
			} catch (Exception e) {
				LOGGER.debug("Eval current failed, returning null.", e);
				return null;
			}
		}
	}

	private EvalResult evalState(State state, String formula) {
		LOGGER.debug("Try to evaluate formula {}.", formula);
		try {
			AbstractEvalResult evalResult = state.eval(new EventB(formula, Collections.emptySet(), FormulaExpand.EXPAND));
			LOGGER.debug("Evaluated formula \"{}\" and got the result: {}", formula, evalResult);
			if (evalResult instanceof EvalResult) {
				return (EvalResult) evalResult;
			}
			return null;
		} catch (EvaluationException evalException) {
			Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
					String.format(bundle.getString("visualisation.model.eval"), formula, evalException.getMessage()),
					ButtonType.OK);
			alert.initOwner(stageManager.getCurrent());
			alert.show();
			LOGGER.warn("EvaluationException while evaluating the formula \"" + formula +"\".", evalException);
			return null;
		}
	}
}
