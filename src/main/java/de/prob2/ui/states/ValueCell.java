package de.prob2.ui.states;

import java.util.ResourceBundle;

import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.ExpandedFormula;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeTableCell;

final class ValueCell extends TreeTableCell<StateItem, ExpandedFormula> {
	private final ResourceBundle bundle;
	
	ValueCell(final ResourceBundle bundle) {
		super();
		
		this.bundle = bundle;
		
		this.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
	}
	
	@Override
	protected void updateItem(final ExpandedFormula item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("false", "true", "not-initialized", "error");
		
		if (item == null || empty) {
			this.setText(null);
		} else {
			checkResult(item.getValue());
		}
	}

	private void checkResult(final BVisual2Value result) {
		if (result == null) {
			this.setText(null);
		} else if (result instanceof BVisual2Value.PredicateValue) {
			final BVisual2Value.PredicateValue predValue = (BVisual2Value.PredicateValue)result;
			this.setText(String.valueOf(predValue.getValue()));
			switch (predValue) {
				case FALSE:
					this.getStyleClass().add("false");
					break;
				
				case TRUE:
					this.getStyleClass().add("true");
					break;
				
				default:
					throw new AssertionError(predValue);
			}
		} else if (result instanceof BVisual2Value.ExpressionValue) {
			this.setText(((BVisual2Value.ExpressionValue)result).getValue());
		} else if (result instanceof BVisual2Value.Inactive) {
			this.setText("");
			this.getStyleClass().add("not-initialized");
		} else if (result instanceof BVisual2Value.Error) {
			final String firstErrorMessageLine = ((BVisual2Value.Error)result).getMessage().split("\\n", 2)[0];
			this.setText(String.format(bundle.getString("states.valueCell.error"), firstErrorMessageLine));
			this.getStyleClass().add("error");
		} else {
			throw new IllegalArgumentException("Don't know how to show the value of a " + result.getClass() + " instance");
		}
	}
}
