package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;

/**
 * @author Christoph Heinzen
 * @since 14.12.17
 */
public class NameCell extends TreeTableCell<Object, Object>{

	NameCell() {
		setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null || empty)
			updateContent(null);
		else if (item instanceof String string)
			updateContent(string);
		else if (item instanceof RuleOperation ruleOperation && ruleOperation.getRuleIdString() != null)
			updateContent(ruleOperation.getName() + " [" + ruleOperation.getRuleIdString() + "]");
		else if (item instanceof AbstractOperation abstractOperation)
			updateContent(abstractOperation.getName());
		else if (item instanceof RuleResult.CounterExample counterExample)
			updateContent(counterExample.getErrorType() + "");
		setGraphic(null);
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
