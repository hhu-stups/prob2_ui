package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FXMLInjected
public class SimulationAbstractMonteCarloChoice extends SimulationMonteCarloChoice {

	protected final List<SimulationCheckingType> PREDICATE_TYPES = Arrays.asList(SimulationCheckingType.PREDICATE_INVARIANT, SimulationCheckingType.PREDICATE_FINAL, SimulationCheckingType.PREDICATE_EVENTUALLY);

	@FXML
	protected Label lbMonteCarloTime;

	@FXML
	protected TextField tfMonteCarloTime;

	@FXML
	protected Label lbPredicate;

	@FXML
	protected TextField tfPredicate;

	@FXML
	protected ChoiceBox<SimulationPropertyItem> checkingChoice;

	protected SimulationAbstractMonteCarloChoice(final StageManager stageManager, final String fxmlResource) {
		super();
		stageManager.loadFXML(this, fxmlResource);
	}

	@FXML
	protected void initialize() {
		super.initialize();
		checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().remove(lbMonteCarloTime);
			this.getChildren().remove(tfMonteCarloTime);
			this.getChildren().remove(lbPredicate);
			this.getChildren().remove(tfPredicate);
			if(to != null) {
				if(to.getCheckingType() == SimulationCheckingType.TIMING) {
					this.add(lbMonteCarloTime, 1, 7);
					this.add(tfMonteCarloTime, 2, 7);
				}
				if(PREDICATE_TYPES.contains(to.getCheckingType())) {
					this.add(lbPredicate, 1, 7);
					this.add(tfPredicate, 2, 7);
				}
			}
			choosingStage.sizeToScene();
		});
	}

	@Override
	public Map<String, Object> extractInformation() {
		Map<String, Object> information = super.extractInformation();
		SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
		information.put("CHECKING_TYPE", checkingChoiceItem.getCheckingType());
		if(PREDICATE_TYPES.contains(checkingChoiceItem.getCheckingType())) {
			information.put("PREDICATE", tfPredicate.getText());
		}

		if(checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
			information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
		}
		return information;
	}

	public void bindMonteCarloTimeProperty(SimpleStringProperty monteCarloTimeProperty) {
		tfMonteCarloTime.textProperty().bindBidirectional(monteCarloTimeProperty);
	}

	public void bindPredicateProperty(SimpleStringProperty predicateProperty) {
		tfPredicate.textProperty().bindBidirectional(predicateProperty);
	}

	public void bindCheckingProperty(SimpleObjectProperty<SimulationPropertyItem> property) {
		// Bind bidirectional does not work on ReadOnlyObjectProperty
		checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				property.set(to);
			}
		});
		property.addListener((observable, from, to) -> {
			if(!Objects.equals(from, to)) {
				checkingChoice.getSelectionModel().select(to);
			}
		});
	}

}
