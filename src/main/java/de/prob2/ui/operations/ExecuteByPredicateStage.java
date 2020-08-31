package de.prob2.ui.operations;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob.model.classicalb.ClassicalBConstant;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.classicalb.Operation;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.Guard;
import de.prob.model.representation.ModelElementList;
import de.prob.statespace.Transition;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.PredicateBuilderView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
	private Button visualizeButton;

	@FXML
	private HBox executeFailedBox;

	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentTrace currentTrace;
	private final ObjectProperty<OperationItem> item;

	private String lastFailedPredicate;
	
	@Inject
	private ExecuteByPredicateStage(final Injector injector, final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super();

		this.injector = injector;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.item = new SimpleObjectProperty<>(this, "item", null);
		
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager.loadFXML(this, "execute_by_predicate_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.predicateBuilderView.setPlaceholder(new Label(bundle.getString("operations.executeByPredicate.noParameters")));
		this.itemProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.operationLabel.setText(null);
				this.predicateBuilderView.setItems(Collections.emptyMap());
			} else {
				this.operationLabel.setText(String.format(bundle.getString("operations.executeByPredicate.operation"), this.getItem().getPrettyName()));
				
				final Map<String, String> items = new LinkedHashMap<>();
				if (to.getParameterValues().isEmpty()) {
					for (final String name : to.getParameterNames()) {
						items.put(name, "");
					}
				} else {
					assert to.getParameterNames().size() == to.getParameterValues().size();
					for (int i = 0; i < to.getParameterNames().size(); i++) {
						items.put(to.getParameterNames().get(i), to.getParameterValues().get(i));
					}
				}
				items.putAll(to.getConstants());
				items.putAll(to.getVariables());
				this.predicateBuilderView.setItems(items);
			}
		});
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
	
	@FXML
	private void handleExecute() {
		final List<Transition> transitions;
		try {
			transitions = this.currentTrace.getStateSpace().transitionFromPredicate(
				this.currentTrace.getCurrentState(),
				this.getItem().getName(),
				this.predicateBuilderView.getPredicate(),
				1
			);
		} catch (IllegalArgumentException | ProBError | EvaluationException e) {
			LOGGER.info("Execute by predicate failed", e);
			lastFailedPredicate = this.predicateBuilderView.getPredicate();
			executeFailedBox.setVisible(true);
			String opName = this.getItem().getName();
			if(Transition.INITIALISE_MACHINE_NAME.equals(opName)) {
				visualizeButton.setDisable(true);
			}
			return;
		}
		assert transitions.size() == 1;
		this.currentTrace.set(this.currentTrace.get().add(transitions.get(0)));
		this.hide();
	}

	@FXML
	public void visualizePredicate() {
		try {
			DotView formulaStage = injector.getInstance(DotView.class);
			formulaStage.visualizeFormula(buildVisualizationPredicate());
			this.close();
			formulaStage.show();
		} catch (EvaluationException | ProBError e) {
			LOGGER.error("Could not visualize formula", e);
			stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content").showAndWait();
		}
	}

	private String buildInnerPredicate() {
		StringBuilder predicate = new StringBuilder();
		predicate.append("(");

		String opName = this.getItem().getName();
		switch (opName) {
			case Transition.SETUP_CONSTANTS_NAME:
				ModelElementList<Property> properties = ((ClassicalBModel) currentTrace.getModel()).getMainMachine().getProperties();
				if(!properties.isEmpty()) {
					predicate.append(properties.stream().map(prop -> "(" + prop.toString() + ")").collect(Collectors.joining(" & ")));
					predicate.append(" & ");
				}
				break;
			case "$initialise_machine":
				// TODO: Implement visualization with before/after predicate
				break;
			default:
				// TODO: Implement visualization with before/after predicate
				Operation operation = ((ClassicalBModel) currentTrace.getModel()).getMainMachine().getOperation(opName);
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
				ModelElementList<ClassicalBConstant> constants = ((ClassicalBModel) currentTrace.getModel()).getMainMachine().getConstants();
				if(!constants.isEmpty()) {
					freeVariables.append("#(");
					freeVariables.append(constants.stream().map(ClassicalBConstant::getName).collect(Collectors.joining(", ")));
					freeVariables.append(").");
				}
				break;
			case "$initialise_machine":
				// TODO: Implement visualization with before/after predicate
				break;
			default:
				// TODO: Implement visualization with before/after predicate -> also add some variables
				List<String> parameterNames = this.getItem().getParameterNames();
				if(!parameterNames.isEmpty()) {
					freeVariables.append("#(");
					freeVariables.append(String.join(", ", parameterNames));
					freeVariables.append(").");
				}
				break;
		}
		return freeVariables.toString();
	}

	private String buildVisualizationPredicate() {
		StringBuilder predicate = new StringBuilder();
		predicate.append(buildFreeVariables());
		predicate.append(buildInnerPredicate());
		return predicate.toString();
	}

}
