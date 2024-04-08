package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import javafx.geometry.Pos;
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
			setText(null);
		else if (item instanceof String string)
			setText(string);
		else if (item instanceof RuleOperation ruleOperation && ruleOperation.getRuleIdString() != null)
			setText(ruleOperation.getName() + " [" + ruleOperation.getRuleIdString() + "]");
		else if (item instanceof AbstractOperation abstractOperation)
			setText(abstractOperation.getName());
		else if (item instanceof RuleResult.CounterExample counterExample)
			setText(counterExample.getErrorType() + "");
		setGraphic(null);
	}
}
