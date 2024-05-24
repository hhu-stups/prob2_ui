package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.ConstantsComponent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Machine;
import de.prob.model.representation.ModelElementList;
import de.prob.statespace.Transition;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.InterruptIfRunningButton;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.PredicateBuilderView;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public final class ExecuteByPredicateStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteByPredicateStage.class);
	
	@FXML 
	private Label operationLabel;
	
	@FXML 
	private Label paramsLabel;
	
	@FXML 
	private PredicateBuilderView predicateBuilderView;
	
	@FXML 
	private TextField predicateTextField;
	
	@FXML 
	private Button executeButton;
	
	@FXML
	private InterruptIfRunningButton interruptButton;

	@FXML
	private Button visualizeButton;

	@FXML
	private HBox executeFailedBox;

	private final Injector injector;
	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;
	private final ObjectProperty<OperationItem> item;

	private String lastFailedPredicate;
	
	@Inject
	private ExecuteByPredicateStage(final Injector injector, final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace, final CliTaskExecutor cliExecutor) {
		super();

		this.injector = injector;
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
		this.item = new SimpleObjectProperty<>(this, "item", null);
		
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager.loadFXML(this, "execute_by_predicate_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.predicateBuilderView.setPlaceholder(new Label(i18n.translate("operations.executeByPredicate.noParameters")));
		this.itemProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.operationLabel.setText(null);
				this.predicateBuilderView.setItems(new ArrayList<>());
			} else {
				this.operationLabel.setText(i18n.translate("operations.executeByPredicate.operation", this.getItem().getPrettyName()));
				
				final List<PredicateBuilderTableItem> items = new ArrayList<>();
				buildParameters(to.getParameterValues(), to.getParameterNames(), items, PredicateBuilderTableItem.VariableType.INPUT);
				buildParameters(to.getReturnParameterValues(), to.getReturnParameterNames(), items, PredicateBuilderTableItem.VariableType.OUTPUT);

				for(Map.Entry<String, String> entry : to.getConstants().entrySet()) {
					items.add(new PredicateBuilderTableItem(entry.getKey(), entry.getValue(), PredicateBuilderTableItem.VariableType.CONSTANT));
				}

				for(Map.Entry<String, String> entry : to.getVariables().entrySet()) {
					items.add(new PredicateBuilderTableItem(entry.getKey(), entry.getValue(), PredicateBuilderTableItem.VariableType.VARIABLE));
				}
				this.predicateBuilderView.setItems(items);
			}
		});
		this.interruptButton.getInterruptButton().getStyleClass().add("button-blue");
		this.interruptButton.getInterruptButton().setText(i18n.translate("common.buttons.cancel"));
	}

	private void buildParameters(List<String> parameterValues, List<String> parameterNames, List<PredicateBuilderTableItem> items, PredicateBuilderTableItem.VariableType type) {
		if (parameterValues.isEmpty()) {
			for (final String name : parameterNames) {
				items.add(new PredicateBuilderTableItem(name, "", type));
			}
		} else {
			assert parameterNames.size() == parameterValues.size();
			for (int i = 0; i < parameterNames.size(); i++) {
				items.add(new PredicateBuilderTableItem(parameterNames.get(i), parameterValues.get(i), type));
			}
		}
	}
	
	public ObjectProperty<OperationItem> itemProperty() {
		return item;
	}
	
	public OperationItem getItem() {
		return this.itemProperty().get();
	}
	
	public void setItem(final OperationItem item) {
		this.itemProperty().set(item);
	}
	
	private void executeSync() {
		final List<Transition> transitions;
		try {
			transitions = this.currentTrace.getStateSpace().transitionFromPredicate(
					this.currentTrace.getCurrentState(),
					this.getItem().getName(),
					this.predicateBuilderView.getPredicate(),
					1
			);
		} catch (ExecuteOperationException e) {
			if(e.getErrors().stream()
					.noneMatch(error -> error.getType() == GetOperationByPredicateCommand.GetOperationErrorType.PARSE_ERROR)) {
				LOGGER.info("Execute by predicate failed", e);
				lastFailedPredicate = this.predicateBuilderView.getPredicate();
				executeFailedBox.setVisible(true);
				String opName = this.getItem().getName();
				if (Transition.INITIALISE_MACHINE_NAME.equals(opName)) {
					visualizeButton.setDisable(true);
				}
			} else {
				LOGGER.error("Execute by predicate failed", e);
				Platform.runLater(() -> {
					Alert alert = stageManager.makeExceptionAlert(e, "operations.executeByPredicate.alerts.parseError.header", "operations.executeByPredicate.alerts.parseError.content");
					alert.initOwner(this);
					alert.showAndWait();
				});
			}
			return;
		} catch (IllegalArgumentException | ProBError | EvaluationException e) {
			LOGGER.error("Execute by predicate failed", e);
			Platform.runLater(() -> {
				Alert alert = stageManager.makeExceptionAlert(e, "operations.executeByPredicate.alerts.parseError.header", "operations.executeByPredicate.alerts.parseError.content");
				alert.initOwner(this);
				alert.showAndWait();
			});
			return;
		}
		assert transitions.size() == 1;
		Transition transition = transitions.get(0);
		RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
		injector.getInstance(UIInteractionHandler.class).addUserInteraction(realTimeSimulator, transition);
		this.currentTrace.set(this.currentTrace.get().add(transition));
		Platform.runLater(this::hide);
	}

	@FXML
	private void handleExecute() {
		cliExecutor.execute(this::executeSync);
	}

	@FXML
	public void visualizePredicate() {
		try {
			DynamicVisualizationStage formulaStage = injector.getInstance(DynamicVisualizationStage.class);
			formulaStage.show();
			formulaStage.visualizeFormulaAsTree(buildVisualizationPredicate());
			this.close();
		} catch (EvaluationException | ProBError e) {
			LOGGER.error("Could not visualize formula", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
			alert.initOwner(this);
			alert.showAndWait();
		}
	}

	private String buildInnerPredicate() {
		StringBuilder predicate = new StringBuilder();
		predicate.append("(");

		String opName = this.getItem().getName();
		final AbstractElement mainComponent = currentTrace.getStateSpace().getMainComponent();
		switch (opName) {
			case Transition.SETUP_CONSTANTS_NAME:
				// FIXME Doesn't work for properties/axioms outside the main machine
				// FIXME getAxioms doesn't work for .eventb files, only Rodin projects
				ModelElementList<? extends Axiom> properties = ((ConstantsComponent)mainComponent).getAxioms();
				if(!properties.isEmpty()) {
					predicate.append(properties.stream().map(prop -> "(" + prop.toString() + ")").collect(Collectors.joining(" & ")));
					predicate.append(" & ");
				}
				break;
			case Transition.INITIALISE_MACHINE_NAME:
				// TODO: Implement visualization with before/after predicate
				break;
			default:
				// TODO: Implement visualization with before/after predicate
				BEvent operation = ((Machine)mainComponent).getEvent(opName);
				// FIXME getChildrenOfType doesn't work for .eventb files, only Rodin projects
				ModelElementList<Guard> guards = operation.getChildrenOfType(Guard.class);
				if(!guards.isEmpty()) {
					predicate.append(guards.stream().map(guard -> "(" + guard.toString() + ")").collect(Collectors.joining(" & ")));
					predicate.append(" & ");
				}
				break;
		}
		predicate.append(lastFailedPredicate);
		predicate.append(")");
		return predicate.toString();
	}

	private String buildFreeVariables() {
		String opName = this.getItem().getName();
		StringBuilder freeVariables = new StringBuilder();
		switch (opName) {
			case Transition.SETUP_CONSTANTS_NAME:
				final Set<String> constantNames = this.getItem().getConstants().keySet();
				if(!constantNames.isEmpty()) {
					freeVariables.append("#");
					freeVariables.append(String.join(", ", constantNames));
					freeVariables.append(".");
				}
				break;
			case Transition.INITIALISE_MACHINE_NAME:
				// TODO: Implement visualization with before/after predicate
				break;
			default:
				// TODO: Implement visualization with before/after predicate -> also add some variables
				List<String> parameterNames = this.getItem().getParameterNames();
				if(!parameterNames.isEmpty()) {
					freeVariables.append("#");
					freeVariables.append(String.join(", ", parameterNames));
					freeVariables.append(".");
				}
				break;
		}
		return freeVariables.toString();
	}

	private String buildVisualizationPredicate() {
		return buildFreeVariables() + buildInnerPredicate();
	}

}
