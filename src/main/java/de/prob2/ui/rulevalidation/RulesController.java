package de.prob2.ui.rulevalidation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.model.brules.RulesChecker;
import de.prob.model.brules.RulesModel;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.ui.RulesView;
import groovy.lang.Singleton;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 20.12.17
 */
@Singleton
public class RulesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesController.class);

	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private RulesModel ruleModel;
	private RulesChecker rulesChecker;

	private final ChangeListener<Trace> traceListener;
	private RulesView rulesView;
	private final RulesDataModel model;

	@Inject
	RulesController(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		this.model = new RulesDataModel();
		this.stageManager = stageManager;

		traceListener = (observable, oldTrace, newTrace) -> {
			LOGGER.debug("Trace changed!");
				if (rulesView != null) {
					if (newTrace == null || !(newTrace.getModel() instanceof RulesModel)) {
						LOGGER.debug("No rules model in new trace!");
						rulesView.clear();
						model.clear();
						//plugin.restoreOperationsView(true);
					} else if (oldTrace == null || !newTrace.getModel().equals(oldTrace.getModel())) {
						// the model changed -> rebuild view
						LOGGER.debug("New rules model in new trace!");
						ruleModel = (RulesModel) newTrace.getModel();
						rulesChecker = new RulesChecker(newTrace);
						rulesChecker.init();
						initialize(ruleModel);
						model.update(rulesChecker.getCurrentTrace());
						//plugin.removeOperationsView();
					} else {
						// model didn't change -> update view
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
		execute(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				rulesChecker = new RulesChecker(currentTrace.get());
				rulesChecker.executeOperationAndDependencies(operationName);
				return null;
			}
		}, operationName);
	}

	public void executeAllOperations() {
		execute(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				rulesChecker = new RulesChecker(currentTrace.get());
				rulesChecker.executeAllOperations();
				return null;
			}
		}, null);

	}

	private void execute(Task<Void> task, String operation) {
		final Stage progressAlert = createProgressAlert(operation);
		progressAlert.setOnCloseRequest(event -> {
			if (task.isRunning()) {
				event.consume();
			}
		});
		progressAlert.show();
		task.setOnSucceeded(event -> {
			LOGGER.debug("Task succeeded!");
			currentTrace.set(rulesChecker.getCurrentTrace());
			progressAlert.close();
		});
		task.setOnFailed(event -> {
			LOGGER.debug("Task failed or cancelled!");
			currentTrace.set(currentTrace.get());
			progressAlert.close();
		});
		task.setOnCancelled(task.getOnFailed());
		new Thread(task).start();

	}

	private Stage createProgressAlert(String rule) {
		String text = rule == null ? "Executing Rules..." : "Executing Rule " + rule;
		VBox content = new VBox(20, new ProgressIndicator(), new Label(text));
		content.setAlignment(Pos.CENTER);
		content.setPadding(new Insets(20,40,20,40));
		content.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		Stage stage = new Stage();
		stage.setScene(new Scene(content));
		stage.setTitle("Execute");
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(stageManager.getCurrent());
		stageManager.register(stage, null);
		return stage;
	}
}
