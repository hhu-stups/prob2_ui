package de.prob2.ui.rulevalidation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.FunctionOperation;
import de.prob.model.brules.RulesChecker;
import de.prob.model.brules.output.RulesDependencyGraph;
import de.prob.model.brules.RulesModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.ui.RulesView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * @author Christoph Heinzen
 * @since 20.12.17
 */
@Singleton
public final class RulesController {
	private static final Logger LOGGER = LoggerFactory.getLogger(RulesController.class);

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private RulesModel rulesModel;
	private RulesChecker rulesChecker;

	private final ChangeListener<Trace> traceListener;
	private RulesView rulesView;
	private final RulesDataModel model;
	private int nrExecutedOperations = 0;

	@Inject
	RulesController(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.model = new RulesDataModel();

		traceListener = (observable, oldTrace, newTrace) -> {
			if (rulesView != null) {
				if (newTrace == null || !(newTrace.getModel() instanceof RulesModel)) {
					LOGGER.debug("No rules model in new trace!");
					rulesView.clear();
					model.clear();
				} else if (oldTrace == null || !newTrace.getModel().equals(oldTrace.getModel())) {
					// the model changed -> rebuild view
					LOGGER.debug("New rules model in new trace!");
					rulesModel = (RulesModel) newTrace.getModel();
					rulesChecker = new RulesChecker(newTrace);
					rulesChecker.init();
					initialize(rulesModel);
					model.update(rulesChecker.getCurrentTrace());
				} else {
					// model didn't change -> update view with same collapsed items
					LOGGER.debug("Update rules view to new trace!");
					model.update(newTrace);
				}
			}
		};

		currentTrace.addListener(traceListener);
	}

	private void initialize(RulesModel newModel) {
		model.initialize(newModel);
		rulesView.build();
		this.nrExecutedOperations = 0;
	}

	void stop() {
		currentTrace.removeListener(traceListener);
	}

	public void setView(RulesView view) {
		this.rulesView = view;
		traceListener.changed(null, null, currentTrace.get());
	}

	public RulesDataModel getModel() {
		return model;
	}

	public void executeOperation(final String operationName) {
		execute(new Task<>() {
			@Override
			protected Void call() {
				rulesView.progressBox.setVisible(true);
				Platform.runLater(() -> {
					ProgressBar progressBar = rulesView.progressBar;
					progressBar.setProgress(-1);
					rulesView.progressOperation.setText(operationName);
					rulesView.progressLabel.setText("");
				});
				rulesChecker = new RulesChecker(currentTrace.get());
				rulesChecker.executeOperationAndDependencies(operationName);
				rulesView.progressBox.setVisible(false);
				return null;
			}
		}, operationName);
	}

	public void executeAllOperations() {
		execute(new Task<>() {
			@Override
			protected Void call() {
				rulesChecker = new RulesChecker(currentTrace.get());
				rulesChecker.init();
				int totalNrOfOperations = rulesModel.getRulesProject().getOperationsMap().values().
					stream().filter(op -> !(op instanceof FunctionOperation)).toList().size();
				// determine all operations that can be executed in this state
				Set<AbstractOperation> executableOperations = rulesChecker.getExecutableOperations();
				while (!executableOperations.isEmpty()) {
					for (AbstractOperation op : executableOperations) {
						rulesChecker.executeOperation(op);
						nrExecutedOperations++;
						Platform.runLater(() -> {
							ProgressBar progressBar = rulesView.progressBar;
							progressBar.setProgress((double) nrExecutedOperations / totalNrOfOperations);
							rulesView.progressOperation.setText(op.getName());
							rulesView.progressLabel.setText(" (" + nrExecutedOperations + "/" + totalNrOfOperations + ")");
						});
					}
					executableOperations = rulesChecker.getExecutableOperations();
				}
				return null;
			}
		}, null);
	}

	private void execute(Task<Void> task, String operation) {
		task.setOnSucceeded(event -> {
			LOGGER.debug("Task for execution of rule {} succeeded!", operation);
			int before = currentTrace.get().size();
			// don't count setup_constants and initialization
			if (operation != null && nrExecutedOperations == 0) before += 2;
			currentTrace.set(rulesChecker.getCurrentTrace());
			if (operation != null) nrExecutedOperations += currentTrace.get().size() - before;
			rulesView.progressBox.setVisible(false);
		});
		task.setOnFailed(event -> {
			if (operation != null) {
				stageManager.makeExceptionAlert(task.getException(), "rulevalidation.execute.error.header", "rulevalidation.execute.error.content.singleRule", operation).showAndWait();
				LOGGER.debug("Task for execution of rule {} failed or cancelled!", operation);
			} else {
				stageManager.makeExceptionAlert(task.getException(),"rulevalidation.execute.error.header", "rulevalidation.execute.error.content.allRules").showAndWait();
				LOGGER.debug("Task for execution of all rules failed or cancelled!");
			}
			currentTrace.set(currentTrace.get());
			rulesView.executeAllButton.setDisable(false);
			rulesView.progressBox.setVisible(false);
		});
		task.setOnCancelled(event -> {
			if (operation != null) {
				stageManager.makeAlert(Alert.AlertType.ERROR, "rulevalidation.execute.error.header", "rulevalidation.execute.error.content.singleRule", operation).showAndWait();
			} else {
				stageManager.makeAlert(Alert.AlertType.ERROR, "rulevalidation.execute.error.header", "rulevalidation.execute.error.content.allRules").showAndWait();
			}
			rulesView.executeAllButton.setDisable(false);
			rulesView.progressBox.setVisible(false);
		});
		new Thread(task).start();

	}

	public String getPartialDependencyGraphExpression(final Collection<AbstractOperation> operations) {
		return RulesDependencyGraph.getGraphExpressionAsString(currentTrace.get(), operations);
	}

	public String getCompleteDependencyGraphExpression() {
		return RulesDependencyGraph.getGraphExpressionAsString(currentTrace.get(), rulesModel.getRulesProject().getOperationsMap().values());
	}

	public void saveValidationReport(final Path path, final Locale language) throws IOException {
		// don't use RuleValidationReport.saveReport directly: duration of checks is measured by RulesChecker
		rulesChecker.saveValidationReport(path, language);
	}
}
