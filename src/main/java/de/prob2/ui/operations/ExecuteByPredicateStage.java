package de.prob2.ui.operations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob.statespace.Transition;
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
	private Label warningLabel;
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentTrace currentTrace;
	private final ObjectProperty<OperationItem> item;
	
	@Inject
	private ExecuteByPredicateStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super();
		
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
			warningLabel.setText(String.format(bundle.getString("operations.executeByPredicate.alerts.failedToExecuteOperation.content")));
			return;
		}
		assert transitions.size() == 1;
		this.currentTrace.set(this.currentTrace.get().add(transitions.get(0)));
		this.hide();
	}
}
