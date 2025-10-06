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
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.ui.RulesView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Christoph Heinzen
 * @since 20.12.17
 */
@Singleton
public final class RulesController {
	private static final Logger LOGGER = LoggerFactory.getLogger(RulesController.class);

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliTaskExecutor;
	private RulesModel rulesModel;
	private RulesChecker rulesChecker;

	private final ChangeListener<Trace> traceListener;
	private RulesView rulesView;
	private final RulesDataModel model;

	@Inject
	RulesController(final StageManager stageManager, final CurrentTrace currentTrace,
	                final CurrentProject currentProject, final CliTaskExecutor cliTaskExecutor) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.cliTaskExecutor = cliTaskExecutor;
		this.model = new RulesDataModel();

		traceListener = (observable, oldTrace, newTrace) -> {
			if (rulesView != null) {
				if (newTrace == null || !(newTrace.getModel() instanceof RulesModel)) {
					LOGGER.trace("No rules model in new trace!");
					rulesView.clear();
					model.clear();
				} else if (oldTrace == null || !newTrace.getModel().equals(oldTrace.getModel())) {
					// the model changed -> rebuild view
					LOGGER.debug("New rules model in new trace!");
					rulesModel = (RulesModel) newTrace.getModel();
					initialize(rulesModel);
					model.update(newTrace);
					cliTaskExecutor.execute(() -> rulesChecker = new RulesChecker(newTrace));
				} else {
					// model didn't change -> update view with same collapsed items
					LOGGER.debug("Update rules view to new trace!");
					model.update(newTrace);
					cliTaskExecutor.execute(() -> rulesChecker.setTrace(newTrace)); // also update RulesChecker; relevant for correct validation report export
					rulesView.executeAllButton.setDisable(newTrace.getNextTransitions().isEmpty());
				}
			}
		};

		currentTrace.addListener(traceListener);
		currentProject.currentMachineProperty().addListener(obs -> traceListener.changed(null, null, currentTrace.get()));
	}

	private void initialize(RulesModel newModel) {
		model.initialize(newModel);
		rulesView.build();
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
				rulesChecker.setTrace(currentTrace.get());
				int totalNrOfOperations = rulesModel.getRulesProject().getOperationsMap().get(operationName)
								.getTransitiveDependencies().size();
				updateProgressBar(0, totalNrOfOperations, "");
				rulesChecker.executeOperationAndDependencies(updateProgressBar(totalNrOfOperations), operationName);
				rulesView.progressBox.setVisible(false);
				return null;
			}
		}, operationName);
	}

	public void executeAllOperations() {
		execute(new Task<>() {
			@Override
			protected Void call() {
				rulesChecker.setTrace(currentTrace.get());
				int totalNrOfOperations = rulesModel.getRulesProject().getOperationsMap().values().
						stream().filter(op -> !(op instanceof FunctionOperation)).toList().size();
				updateProgressBar(0, totalNrOfOperations, "");
				rulesChecker.executeAllOperationsDirect(updateProgressBar(totalNrOfOperations), 1);
				return null;
			}
		}, null);
	}

	private RulesChecker.RulesCheckListener updateProgressBar(int totalNr) {
		return (nr, name) -> updateProgressBar(nr, totalNr, name);
	}

	private void updateProgressBar(int nr, int totalNr, String name) {
		Platform.runLater(() -> {
			ProgressBar progressBar = rulesView.progressBar;
			progressBar.setProgress((double) nr / totalNr);
			rulesView.progressOperation.setText(name);
			rulesView.progressLabel.setText(" (" + nr + "/" + totalNr + ")");
		});
	}

	private void execute(Task<Void> task, String operation) {
		task.setOnSucceeded(event -> {
			LOGGER.debug("Task for execution of rule {} succeeded!", operation);
			cliTaskExecutor.execute(() -> currentTrace.set(rulesChecker.getCurrentTrace()));
			rulesView.progressBox.setVisible(false);
		});
		task.setOnFailed(event -> {
			cliTaskExecutor.execute(() -> currentTrace.set(rulesChecker.getCurrentTrace()));
			handleFailedExecution(event, operation);
		});
		task.setOnCancelled(event -> handleFailedExecution(event, operation));
		cliTaskExecutor.execute(task);
	}

	private void handleFailedExecution(WorkerStateEvent event, String operation) {
		rulesView.executeAllButton.setDisable(false);
		rulesView.progressBox.setVisible(false);
		rulesChecker.stop();
		if (!event.getSource().getException().getMessage().equals("ProB was interrupted")) { // was not a regular interruption via cancel button
			if (operation != null) {
				stageManager.makeAlert(Alert.AlertType.ERROR, "rulevalidation.execute.error.header", "rulevalidation.execute.error.content.singleRule", operation).showAndWait();
			} else {
				stageManager.makeAlert(Alert.AlertType.ERROR, "rulevalidation.execute.error.header", "rulevalidation.execute.error.content.allRules").showAndWait();
			}
		}
	}

	public String getPartialDependencyGraphExpression(final Collection<AbstractOperation> operations) {
		return RulesDependencyGraph.getGraphExpressionAsString(currentTrace.get(), operations);
	}

	public void saveValidationReport(final Path path) {
		// don't use RuleValidationReport.saveReport directly: duration of checks is measured by RulesChecker
		rulesChecker.saveValidationReport(path);
	}
}
