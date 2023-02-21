package de.prob2.ui.simulation.choice;

import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@FXMLInjected
public class SimulationPropertyChoice extends GridPane {

	private final List<SimulationCheckingType> PREDICATE_TYPES = Arrays.asList(SimulationCheckingType.PREDICATE_INVARIANT, SimulationCheckingType.PREDICATE_FINAL, SimulationCheckingType.PREDICATE_EVENTUALLY);

	@FXML
	private Label lbMonteCarloTime;

	@FXML
	private TextField tfMonteCarloTime;

	@FXML
	private Label lbPredicate;

	@FXML
	private TextField tfPredicate;

	@FXML
	private ChoiceBox<SimulationMonteCarloChoice.SimulationPropertyItem> checkingChoice;

	@FXML
	private ChoiceBox<SimulationType> simulationChoice;

	private SimulationChoosingStage choosingStage;

	@Inject
	private SimulationPropertyChoice(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "simulation_property_choice.fxml");
	}

	@FXML
	public void initialize() {
		checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().remove(lbMonteCarloTime);
			this.getChildren().remove(tfMonteCarloTime);
			this.getChildren().remove(lbPredicate);
			this.getChildren().remove(tfPredicate);
			if(to != null) {
				if(to.getCheckingType() == SimulationCheckingType.TIMING) {
					this.add(lbMonteCarloTime, 1, 3);
					this.add(tfMonteCarloTime, 2, 3);
				}
				if(PREDICATE_TYPES.contains(to.getCheckingType())) {
					this.add(lbPredicate, 1, 3);
					this.add(tfPredicate, 2, 3);
				}
			}
			choosingStage.sizeToScene();
		});
	}

	public Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		SimulationMonteCarloChoice.SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
		information.put("CHECKING_TYPE", checkingChoiceItem.getCheckingType());
		if(PREDICATE_TYPES.contains(checkingChoiceItem.getCheckingType())) {
			information.put("PREDICATE", tfPredicate.getText());
		}

		if(checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
			information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
		}
		return information;
	}

	public ChoiceBox<SimulationType> simulationChoice() {
		return simulationChoice;
	}

	public void setChoosingStage(SimulationChoosingStage choosingStage) {
		this.choosingStage = choosingStage;
	}
}
