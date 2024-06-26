package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import de.prob2.ui.internal.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.util.Duration;

/**
 * @author Christoph Heinzen
 * @since 14.12.17
 */
public class NameCell extends TreeTableCell<Object, Object>{

	private final I18n i18n;

	NameCell(I18n i18n) {
		this.i18n = i18n;
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
			updateContent(Integer.toString(counterExample.getErrorType()),
				i18n.translate("rulevalidation.table.violations.errorType", counterExample.getErrorType()));
		else if (item instanceof RuleResult.SuccessMessage successMessage)
			updateContent(Integer.toString(successMessage.getRuleBodyCount()),
				i18n.translate("rulevalidation.table.successful.ruleBody", successMessage.getRuleBodyCount()));
		setGraphic(null);
	}

	private void updateContent(String content) {
		this.updateContent(content, content);
	}

	private void updateContent(String content, String hover) {
		setText(content);
		if (hover != null && !hover.isEmpty()) {
			Tooltip tooltip = new Tooltip(hover);
			tooltip.setShowDuration(Duration.INDEFINITE);
			setTooltip(tooltip);
		} else {
			setTooltip(null);
		}
	}
}
