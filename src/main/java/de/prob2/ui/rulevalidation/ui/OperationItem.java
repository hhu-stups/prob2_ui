package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.prob.model.brules.ComputationStatus;
import de.prob.model.brules.RuleResult;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.rulevalidation.RulesDataModel;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import java.util.Collections;
import java.util.List;

/**
 * @author Christoph Heinzen
 * @since 16.12.17
 */
class OperationItem extends TreeItem<Object> {

	private final I18n i18n;

	private final RulesDataModel model;
	private final String operation;
	private boolean executable = true;

	OperationItem(I18n i18n, AbstractOperation operation, SimpleObjectProperty<Object> resultProperty, RulesDataModel model) {
		super(operation);
		this.i18n = i18n;
		this.operation = operation.getName();
		this.model = model;
		resultProperty.addListener((observable, oldValue, newValue) -> {
			executable = true;
			OperationItem.this.getChildren().clear();
			if (newValue instanceof RuleResult ruleResult) {
				switch (ruleResult.getRuleState()) {
					case FAIL, NOT_CHECKED -> createRuleChildren(ruleResult);
					case SUCCESS -> {
						OperationItem.this.getChildren().clear();
						addSuccessMessages(ruleResult);
						executable = false;
					}
					case DISABLED -> {
						OperationItem.this.getChildren().clear();
						executable = false;
					}
				}
			} else if (newValue instanceof ComputationStatus status && operation instanceof ComputationOperation computationOperation) {
				createComputationChildren(status, computationOperation);
			}
		});
	}

	private void createComputationChildren(ComputationStatus status, ComputationOperation op) {
		if (status.isNotExecuted()) {
			// create children for unchecked dependencies
			List<String> notCheckedDependencies = model.getNotCheckedDependenciesOfComputation(op.getName());
			if (!notCheckedDependencies.isEmpty()) {
				TreeItem<Object> notCheckedItem = new TreeItem<>(i18n.translate("rulevalidation.table.dependencies.unchecked"));
				for (String notChecked : notCheckedDependencies) {
					notCheckedItem.getChildren().add(new TreeItem<>(notChecked));
				}
				this.getChildren().add(notCheckedItem);
			}

			// create children for failed dependencies
			List<String> failedDependencies = model.getFailedDependenciesOfComputation(op.getName());
			if (!failedDependencies.isEmpty()) {
				TreeItem<Object> failedItem = new TreeItem<>(i18n.translate("rulevalidation.table.dependencies.failed"));
				for (String failed : failedDependencies) {
					failedItem.getChildren().add(new TreeItem<>(failed));
				}
				this.getChildren().add(failedItem);
				executable = false;
			}

			// create children for disabled dependencies
			List<String> disabledDependencies = model.getDisabledDependencies(operation);
			if (!disabledDependencies.isEmpty()) {
				addDisabledDependencies(disabledDependencies);
				executable = false;
			}
		} else {
			executable = false;
		}
	}

	private void createRuleChildren(RuleResult result) {
		switch (result.getRuleState()) {
			case FAIL:
				// create child items to show why the rule failed
				addCounterExamples(result);
				executable = false;
				break;
			case NOT_CHECKED:
				// create child items for unchecked dependencies
				if (!result.getNotCheckedDependencies().isEmpty()) {
					TreeItem<Object> notCheckedItem = new TreeItem<>(i18n.translate("rulevalidation.table.dependencies.unchecked"));
					Collections.sort(result.getNotCheckedDependencies());
					for (String notChecked : result.getNotCheckedDependencies()) {
						notCheckedItem.getChildren().add(new TreeItem<>(notChecked));
					}
					this.getChildren().add(notCheckedItem);
				}

				// create child items for failed dependencies
				if (!result.getFailedDependencies().isEmpty()) {
					TreeItem<Object> failedItem = new TreeItem<>(i18n.translate("rulevalidation.table.dependencies.failed"));
					Collections.sort(result.getFailedDependencies());
					for (String failed : result.getFailedDependencies()) {
						failedItem.getChildren().add(new TreeItem<>(failed));
					}
					this.getChildren().add(failedItem);
					executable = false;
				}

				// create child items for disabled dependencies
				List<String> disabledDependencies = model.getDisabledDependencies(operation);
				if (!disabledDependencies.isEmpty()) {
					addDisabledDependencies(disabledDependencies);
					executable = false;
				}
				break;
			case DISABLED:
				executable = false;
				break;
		}
		addSuccessMessages(result);
		addUncheckedMessages(result);
	}

	private void addCounterExamples(RuleResult result) {
		if (result.getNumberOfViolations() == -1) {
			this.getChildren().add(new TreeItem<>(i18n.translate("rulevalidation.table.violations.infinitelyMany")));
		} else {
			addMessages(result.getCounterExamples(), i18n.translate("rulevalidation.table.violations"));
		}
	}

	private void addSuccessMessages(RuleResult result) {
		if (result.getNumberOfSuccesses() == -1) {
			this.getChildren().add(new TreeItem<>(i18n.translate("rulevalidation.table.successful.infinitelyMany")));
		} else {
			addMessages(result.getSuccessMessages(), i18n.translate("rulevalidation.table.successful"));
		}
	}

	private void addUncheckedMessages(RuleResult result) {
		if (result.getNumberOfUnchecked() == -1) {
			this.getChildren().add(new TreeItem<>(i18n.translate("rulevalidation.table.unchecked.infinitelyMany")));
		} else {
			addMessages(result.getUncheckedMessages(), i18n.translate("rulevalidation.table.unchecked"));
		}
	}

	private <T> void addMessages(List<T> messages, String displayedParentName) {
		int size = messages.size();
		if (size == 0) {
			return;
		}
		TreeItem<Object> messageItem = new TreeItem<>(displayedParentName + " (" + size + ")");
		if (size > 10) {
			TreeItem<Object> collapsedMessages = new TreeItem<>(i18n.translate("rulevalidation.table.violations.showAll") + " (" + (size - 10) + ")");
			// display the first ten messages and collapse the others
			for (int i = 0; i < 10; i++) {
				messageItem.getChildren().add(new TreeItem<>(messages.get(i)));
			}
			for (int i = 10; i < size; i++) {
				collapsedMessages.getChildren().add(new TreeItem<>(messages.get(i)));
			}
			messageItem.getChildren().add(collapsedMessages);
		} else {
			for (T message : messages) {
				messageItem.getChildren().add(new TreeItem<>(message));
			}
		}
		this.getChildren().add(messageItem);
	}

	private void addDisabledDependencies(List<String> disabledDependencies) {
		if (!disabledDependencies.isEmpty()) {
			TreeItem<Object> disabledItem = new TreeItem<>(i18n.translate("rulevalidation.table.dependencies.disabled"));
			for (String disabled : disabledDependencies) {
				disabledItem.getChildren().add(new TreeItem<>(disabled));
			}
			this.getChildren().add(disabledItem);
			executable = false;
		}
	}

	boolean isExecutable() {
		return executable;
	}
}
