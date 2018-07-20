package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob.statespace.Transition;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecuteByPredicateStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteByPredicateStage.class);
	
	@FXML private Label operationLabel;
	@FXML private Label paramsLabel;
	@FXML private GridPane paramsGrid;
	@FXML private TextField predicateTextField;
	@FXML private Button executeButton;
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentTrace currentTrace;
	private final ObjectProperty<OperationItem> item;
	private final List<String> identifiers;
	private final List<TextField> valueTextFields;
	
	@Inject
	private ExecuteByPredicateStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super();
		
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.item = new SimpleObjectProperty<>(this, "item", null);
		this.identifiers = new ArrayList<>();
		this.valueTextFields = new ArrayList<>();
		
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager.loadFXML(this, "execute_by_predicate_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.itemProperty().addListener((o, from, to) -> {
			this.paramsGrid.getChildren().clear();
			this.identifiers.clear();
			this.valueTextFields.clear();
			if (to == null) {
				this.operationLabel.setText(null);
			} else {
				this.operationLabel.setText(String.format(bundle.getString("operations.executeByPredicate.operation"), this.getItem().getName()));
				
				assert to.getParameterNames().size() == to.getParameterValues().size();
				for (int i = 0; i < to.getParameterNames().size(); i++) {
					this.identifiers.add(to.getParameterNames().get(i));
					this.valueTextFields.add(new TextField(to.getParameterValues().get(i)));
				}
				
				to.getConstants().forEach((name, value) -> {
					this.identifiers.add(name);
					this.valueTextFields.add(new TextField(value));
				});
				
				to.getVariables().forEach((name, value) -> {
					this.identifiers.add(name);
					this.valueTextFields.add(new TextField(value));
				});
				
				assert this.identifiers.size() == this.valueTextFields.size();
				for (int i = 0; i < this.identifiers.size(); i++) {
					final Label label = new Label(this.identifiers.get(i));
					GridPane.setRowIndex(label, i);
					GridPane.setColumnIndex(label, 0);
					GridPane.setHgrow(label, Priority.NEVER);
					
					final TextField textField = this.valueTextFields.get(i);
					label.setLabelFor(textField);
					GridPane.setRowIndex(textField, i);
					GridPane.setColumnIndex(textField, 1);
					GridPane.setHgrow(textField, Priority.ALWAYS);
					
					this.paramsGrid.getChildren().addAll(label, textField);
				}
			}
			if (this.paramsGrid.getChildren().isEmpty()) {
				this.paramsLabel.setText(bundle.getString("operations.executeByPredicate.noParameters"));
			} else {
				this.paramsLabel.setText(bundle.getString("operations.executeByPredicate.parameters"));
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
		final StringBuilder predicate = new StringBuilder();
		assert this.identifiers.size() == this.valueTextFields.size();
		for (int i = 0; i < this.identifiers.size(); i++) {
			predicate.append(this.identifiers.get(i));
			predicate.append(" = (");
			predicate.append(this.valueTextFields.get(i).getText());
			predicate.append(") & ");
		}
		predicate.append('(');
		predicate.append(this.predicateTextField.getText());
		predicate.append(')');
		
		final List<Transition> transitions;
		try {
			transitions = this.currentTrace.getStateSpace().transitionFromPredicate(
				this.currentTrace.getCurrentState(),
				this.getItem().getName(),
				predicate.toString(),
				1
			);
		} catch (IllegalArgumentException | ProBError e) {
			LOGGER.info("Execute by predicate failed", e);
			stageManager.makeExceptionAlert(bundle.getString("operations.executeByPredicate.alerts.failedToExecuteOperation.content"), e).show();
			return;
		}
		assert transitions.size() == 1;
		this.currentTrace.set(this.currentTrace.get().add(transitions.get(0)));
		this.hide();
	}
}
