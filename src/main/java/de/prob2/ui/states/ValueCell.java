package de.prob2.ui.states;

import java.util.Map;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.StateError;
import de.prob.animator.domainobjects.WDError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;

import javafx.scene.control.TreeTableCell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ValueCell extends TreeTableCell<Object, Object> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ValueCell.class);
	
	private final Map<IEvalElement, AbstractEvalResult> values;
	private final boolean isCurrent;
	
	ValueCell(final Map<IEvalElement, AbstractEvalResult> values, final boolean isCurrent) {
		super();
		
		this.values = values;
		this.isCurrent = isCurrent;
	}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("false", "true", "errorresult");
		
		if (item == null || empty) {
			super.setText(null);
			super.setGraphic(null);
		} else {
			if (item instanceof String || item instanceof Class<?>) {
				this.setText(null);
			} else if (item instanceof AbstractFormulaElement) {
				final AbstractEvalResult result = this.values.get(((AbstractFormulaElement)item).getFormula());
				if (result == null) {
					this.setText(null);
				} else if (result instanceof EvalResult) {
					final EvalResult eresult = (EvalResult)result;
					this.setText(eresult.getValue());
					if ("FALSE".equals(eresult.getValue())) {
						this.getStyleClass().add("false");
					} else if ("TRUE".equals(eresult.getValue())) {
						this.getStyleClass().add("true");
					}
				} else if (result instanceof IdentifierNotInitialised) {
					this.setText("(not initialized)");
					this.getStyleClass().add("errorresult");
				} else if (result instanceof WDError) {
					this.setText("(not well-defined)");
					this.getStyleClass().add("errorresult");
				} else if (result instanceof EvaluationErrorResult) {
					this.setText("Error: " + ((EvaluationErrorResult) result).getResult());
					this.getStyleClass().add("errorresult");
				} else if (result instanceof EnumerationWarning) {
					this.setText("(enumeration warning)");
					this.getStyleClass().add("errorresult");
				} else {
					LOGGER.warn("Unknown result type, falling back to toString: {}", result.getClass());
					// noinspection ObjectToString
					this.setText(result.getClass() + " toString: " + result);
				}
			} else if (item instanceof AbstractElement) {
				this.setText(null);
			} else if (item instanceof StateError) {
				this.setText(this.isCurrent ? ((StateError)item).getShortDescription() : null);
			} else {
				throw new IllegalArgumentException("Don't know how to show the value of a " + item.getClass() + " instance");
			}
			super.setGraphic(null);
		}
	}
}
