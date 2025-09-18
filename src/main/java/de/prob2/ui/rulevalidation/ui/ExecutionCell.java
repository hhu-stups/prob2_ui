package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.rulevalidation.RulesController;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

/**
 * @author Christoph Heinzen
 * @since 20.12.17
 */
public class ExecutionCell extends TreeTableCell<Object, Object> {

	private final RulesController controller;
	private final I18n i18n;

	ExecutionCell(RulesController controller, I18n i18n) {
		this.controller = controller;
		this.i18n = i18n;
		setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		if (getTableRow().getTreeItem() instanceof OperationItem operationItem
				&& operationItem.getValue() instanceof AbstractOperation operation) {
			setGraphic(operationItem.isExecutable() ? createLabel(operation.getName()) : null);
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
