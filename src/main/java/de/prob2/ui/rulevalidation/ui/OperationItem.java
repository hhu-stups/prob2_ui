package de.prob2.ui.rulevalidation.ui;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.prob.model.brules.ComputationStatus;
import de.prob.model.brules.RuleResult;
import de.prob2.ui.rulevalidation.RulesDataModel;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 16.12.17
 */
class OperationItem extends TreeItem<Object> {

	private final RulesDataModel model;
	private final String operation;
	private boolean executable = true;

	OperationItem(AbstractOperation operation, SimpleObjectProperty<Object> resultProperty, RulesDataModel model) {
		super(operation);
		this.operation = operation.getName();
		this.model = model;
		resultProperty.addListener((observable, oldValue, newValue) -> {
			OperationItem.this.getChildren().clear();
			if (newValue instanceof RuleResult ruleResult) {
				executable = true;
				switch (ruleResult.getRuleState()) {
					case FAIL:
					case NOT_CHECKED:
						createRuleChildren(ruleResult);
						break;
					case DISABLED:
					case SUCCESS:
						OperationItem.this.getChildren().clear();
						executable = false;
						break;
				}
			} else if (newValue instanceof Map.Entry<?, ?> && operation instanceof ComputationOperation) {
				createComputationChildren((Map.Entry<?, ?>)newValue, (ComputationOperation) operation);
			}
		});
	}

	private void createComputationChildren(Map.Entry<?, ?> result, ComputationOperation op) {
		ComputationStatus state = (ComputationStatus) result.getValue();
		if (state == ComputationStatus.NOT_EXECUTED) {
			List<String> failedDependencies = model.getFailedDependenciesOfComputation(op.getName());
			List<String> notCheckedDependencies = model.getNotCheckedDependenciesOfComputation(op.getName());
			// create children for unchecked dependencies
			if (!notCheckedDependencies.isEmpty()) {
				TreeItem<Object> notCheckedItem = new TreeItem<>("UNCHECKED DEPENDENCIES");
				Collections.sort(notCheckedDependencies);
				for (String notChecked : notCheckedDependencies) {
					notCheckedItem.getChildren().add(new TreeItem<>(notChecked));
				}
				this.getChildren().add(notCheckedItem);
			}
			// create children for failed dependencies
			if (!failedDependencies.isEmpty()) {
				TreeItem<Object> failedItem = new TreeItem<>("FAILED DEPENDENCIES");
				Collections.sort(failedDependencies);
				for (String failed : failedDependencies) {
					failedItem.getChildren().add(new TreeItem<>(failed));
				}
				this.getChildren().add(failedItem);
				executable = false;
			}
			// create children for disabled dependencies
			List<String> disabledDependencies = model.getDisabledDependencies(operation);
			addDisabledDependencies(disabledDependencies);
		}
	}

	private void createRuleChildren(RuleResult result) {
		switch(result.getRuleState()) {
			case FAIL:
				// create child items to show why the rule failed
				TreeItem<Object> violationItem = new TreeItem<>("VIOLATIONS");
				result.getCounterExamples().sort(Comparator.comparingInt(RuleResult.CounterExample::getErrorType));
				for (RuleResult.CounterExample example : result.getCounterExamples()) {
					violationItem.getChildren().add(new TreeItem<>(example));
				}
				this.getChildren().add(violationItem);
				executable = false;
				break;
			case NOT_CHECKED:
				// create child items for unchecked dependencies
				if (!result.getNotCheckedDependencies().isEmpty()) {
					TreeItem<Object> notCheckedItem = new TreeItem<>("UNCHECKED DEPENDENCIES");
					Collections.sort(result.getNotCheckedDependencies());
					for (String notChecked : result.getNotCheckedDependencies()) {
						notCheckedItem.getChildren().add(new TreeItem<>(notChecked));
					}
					this.getChildren().add(notCheckedItem);
				}

				// create child items for failed dependencies
				if (!result.getFailedDependencies().isEmpty()) {
					TreeItem<Object> failedItem = new TreeItem<>("FAILED DEPENDENCIES");
					Collections.sort(result.getFailedDependencies());
					for (String failed : result.getFailedDependencies()) {
						failedItem.getChildren().add(new TreeItem<>(failed));
					}
					this.getChildren().add(failedItem);
					executable = false;
				}

				// create child items for disabled dependencies
				List<String> disabledDependencies = model.getDisabledDependencies(operation);
				addDisabledDependencies(disabledDependencies);
				break;
		}
	}

	private void addDisabledDependencies(List<String> disabledDependencies) {
		if (!disabledDependencies.isEmpty()) {
			TreeItem<Object> disabledItem = new TreeItem<>("DISABLED DEPENDENCIES");
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
