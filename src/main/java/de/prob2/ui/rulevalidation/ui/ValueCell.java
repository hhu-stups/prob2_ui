package de.prob2.ui.rulevalidation.ui;

import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.model.brules.ComputationStatus;
import de.prob.model.brules.RuleResult;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;

import java.util.Map;

/**
 * @author Christoph Heinzen
 * @since 14.12.17
 */
public class ValueCell extends TreeTableCell<Object, Object>{

	private boolean executable;

	ValueCell() {
		setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		TreeItem<Object> treeItem = getTableRow().getTreeItem();
		if (treeItem instanceof OperationItem) {
			executable = ((OperationItem) treeItem).isExecutable();
		}
		configureEmptyCell();
		if (item instanceof RuleResult ruleResult)
			configureForRuleResult(ruleResult);
		else if (item instanceof RuleResult.CounterExample counterExample)
			updateContent(counterExample.getMessage());
		else if (item instanceof Map.Entry<?,?> entry)
			configureForComputationResult((ComputationStatus) entry.getValue());
		else if (item instanceof IdentifierNotInitialised notInitialised)
			configureForNotInitialised(notInitialised);
		setGraphic(null);
	}

	private void configureForComputationResult(ComputationStatus result) {
		getTableRow().getTreeItem();
		updateContent(result.toString());
		switch (result) {
			case EXECUTED:
				getStyleClass().add("true");
				break;
			case DISABLED:
				setStyle("-fx-background-color:lightgray");
				break;
			case NOT_EXECUTED:
				if (!executable) {
					// should not be translated? Appears next to rule states SUCCESS, FAIL, …
					updateContent("NOT EXECUTABLE");
				}
				setStyle(null);
				break;
		}
	}

	private void configureEmptyCell() {
		updateContent(null);
		setStyle(null);
		getStyleClass().removeAll("true","false");
	}

	private void configureForRuleResult(RuleResult result) {
		updateContent(result.getRuleState().name());
		switch (result.getRuleState()) {
			case FAIL:
				getStyleClass().add("false");
				break;
			case SUCCESS:
				getStyleClass().add("true");
				break;
			case NOT_CHECKED:
				if (!executable) {
					updateContent("NOT CHECKABLE");
				}
				setStyle(null);
				break;
			case DISABLED:
				setStyle("-fx-background-color:lightgray");
				break;
		}
	}

	private void configureForNotInitialised(IdentifierNotInitialised item) {
		updateContent(item.getResult());
		setStyle(null);
	}

	private void updateContent(String content) {
		setText(content);
		if (content != null && !content.isEmpty()) {
			setTooltip(new Tooltip(content));
		} else {
			setTooltip(null);
		}
	}
}
