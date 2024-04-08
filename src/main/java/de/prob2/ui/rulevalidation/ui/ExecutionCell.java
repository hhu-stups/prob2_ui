package de.prob2.ui.rulevalidation.ui;

import de.prob.model.brules.ComputationStatus;
import de.prob.model.brules.RuleResult;
import de.prob.model.brules.RuleStatus;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.rulevalidation.RulesController;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * @author Christoph Heinzen
 * @since 20.12.17
 */
public class ExecutionCell extends TreeTableCell<Object, Object> {

	private final RulesController controller;
	private boolean executable;
	private final I18n i18n;

	ExecutionCell(RulesController controller, I18n i18n) {
		this.controller = controller;
		this.i18n = i18n;
		setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		TreeItem<Object> treeItem = getTableRow().getTreeItem();
		if (treeItem instanceof OperationItem operationItem) {
			executable = operationItem.isExecutable();
		}

		if (item instanceof RuleResult ruleResult) {
			configureForRule(ruleResult);
		} else if (item instanceof Map.Entry) {
			configureForComputation((Map.Entry<String, ComputationStatus>) item);
		} else {
			setGraphic(null);
		}
	}

	private void configureForComputation(Map.Entry<String, ComputationStatus> resultEntry) {
		ComputationStatus result = resultEntry.getValue();
		String computation = resultEntry.getKey();
		if (result == ComputationStatus.NOT_EXECUTED && executable) {
			setGraphic(createLabel(computation));
		} else {
			setGraphic(null);
		}
	}

	private void configureForRule(RuleResult result) {
		if (result.getRuleState() == RuleStatus.NOT_CHECKED && executable) {
			setGraphic(createLabel(result.getRuleName()));
		} else {
			setGraphic(null);
		}
	}

	private Label createLabel(String operation) {
		Label label = new Label(i18n.translate("common.buttons.execute"));
		label.setUnderline(true);
		label.setTextFill(Color.valueOf("#037875"));
		label.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				controller.executeOperation(operation);
			}
		});
		return label;
	}
}
