package de.prob2.ui.simulation.choice;

import com.google.inject.Injector;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SimulationChoosingStage extends Stage {
	@FXML
	private Button btAdd;

	@FXML
	private Button btCheck;

	@FXML
    private HBox timeBox;

	@FXML
	private GridPane monteCarloBox;

	@FXML
	private HBox tracesBox;

	@FXML
	private ChoiceBox<ReplayTrace> cbTraces;

	@FXML
	private Label lbTime;

	@FXML
	private TextField tfTime;

	@FXML
	private Label lbMonteCarloTime;

	@FXML
	private TextField tfMonteCarloTime;

	@FXML
    private TextField tfSimulations;

	@FXML
	private Label lbSteps;

	@FXML
	private TextField tfSteps;

	@FXML
	private Label lbEndingPredicate;

	@FXML
	private TextField tfEndingPredicate;

	@FXML
	private Label lbEndingTime;

	@FXML
	private TextField tfEndingTime;

	@FXML
	private VBox inputBox;

	@FXML
	private ChoiceBox<SimulationChoiceItem> simulationChoice;

	@FXML
	private ChoiceBox<SimulationPropertyItem> checkingChoice;

	@FXML
	private ChoiceBox<SimulationEndingItem> endingChoice;

	@FXML
	private ChoiceBox<SimulationHypothesisChoiceItem> hypothesisCheckingChoice;

	@FXML
	private TextField tfProbability;

	private final Injector injector;

	private final ResourceBundle bundle;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final SimulationItemHandler simulationItemHandler;

	private Path path;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final Injector injector, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace,
								   final SimulationItemHandler simulationItemHandler) {
		this.injector = injector;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.simulationItemHandler = simulationItemHandler;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	private void initialize() {
        inputBox.visibleProperty().bind(simulationChoice.getSelectionModel().selectedItemProperty().isNotNull());
        setCheckListeners();
        simulationChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            if(to == null) {
                return;
            }
            changeGUIType(to.getSimulationType());
            this.sizeToScene();
        });

		endingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			monteCarloBox.getChildren().removeAll(lbSteps, tfSteps, lbEndingPredicate, tfEndingPredicate, lbEndingTime, tfEndingTime);
			if(to != null) {
				switch (to.getEndingType()) {
					case NUMBER_STEPS:
						monteCarloBox.add(lbSteps, 1, 3);
						monteCarloBox.add(tfSteps, 2, 3);
						break;
					case ENDING_PREDICATE:
						monteCarloBox.add(lbEndingPredicate, 1, 3);
						monteCarloBox.add(tfEndingPredicate, 2, 3);
						break;
					case ENDING_TIME:
						monteCarloBox.add(lbEndingTime, 1, 3);
						monteCarloBox.add(tfEndingTime, 2, 3);
						break;
					default:
						break;
				}
			}
			this.sizeToScene();
		});

        checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
        	if(to != null && to.getCheckingType() == SimulationCheckingType.TIMING) {
				monteCarloBox.add(lbMonteCarloTime, 1, 8);
				monteCarloBox.add(tfMonteCarloTime, 2, 8);
			} else {
        		monteCarloBox.getChildren().remove(lbMonteCarloTime);
        		monteCarloBox.getChildren().remove(tfMonteCarloTime);
			}
			this.sizeToScene();
		});

		cbTraces.setConverter(new StringConverter<ReplayTrace>() {
			@Override
			public String toString(ReplayTrace replayTrace) {
				return replayTrace.getName();
			}

			@Override
			public ReplayTrace fromString(String s) {
				return cbTraces.getItems().stream()
						.filter(t -> s.equals(t.getName()))
						.collect(Collectors.toList()).get(0);
			}
		});
	}

	private void setCheckListeners() {
        btAdd.setOnAction(e -> {
			boolean validChoice = checkSelection();
			if(!validChoice) {
				return;
			}
			// TODO: Inform user
            this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), this.extractItem());
            this.close();
        });
        btCheck.setOnAction(e -> {
        	boolean validChoice = checkSelection();
        	if(!validChoice) {
        		return;
			}
        	// TODO: Inform user
            final SimulationItem newItem = this.extractItem();
            final Optional<SimulationItem> existingItem = this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), newItem);
            this.close();
            this.simulationItemHandler.checkItem(existingItem.orElse(newItem), false);
        });
    }

    private boolean checkSelection() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		SimulationType type = item.getSimulationType();
		switch (type) {
			case TIMING:
				return !tfTime.getText().isEmpty();
			case TRACE_REPLAY:
				return cbTraces.getSelectionModel().getSelectedItem() != null;
			case ESTIMATION:
				// TODO
			case HYPOTHESIS_TEST:
				// TODO: Check integer
				return !(tfSteps.getText().isEmpty() && tfSimulations.getText().isEmpty());
			default:
				break;
		}
		return true;
	}


	private SimulationItem extractItem() {
		return new SimulationItem(new SimulationCheckingConfiguration(this.extractType(), this.extractInformation()), "");
	}

	private SimulationType extractType() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		return item.getSimulationType();
	}

	private Map<String, Object> extractInformation() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		SimulationType simulationType = item.getSimulationType();
		Map<String, Object> information = new HashMap<>();
		switch (simulationType) {
			case TIMING:
				information.put("TIME", Integer.parseInt(tfTime.getText()));
				break;
			case ESTIMATION:
				// TODO
			case HYPOTHESIS_TEST:
                information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));
                information.put("STEPS_PER_EXECUTION", Integer.parseInt(tfSteps.getText()));
                information.put("CHECKING_TYPE", checkingChoice.getSelectionModel().getSelectedItem().getCheckingType());
                information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType());
                information.put("PROBABILITY", Double.parseDouble(tfProbability.getText()));
				SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
                if(checkingChoiceItem != null && checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
					information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
				}
				break;
			case TRACE_REPLAY:
				information.put("TRACE", cbTraces.getValue());
				break;
		}
		return information;
	}

    private void changeGUIType(final SimulationType type) {
        inputBox.getChildren().removeAll(timeBox, monteCarloBox, tracesBox);
        tfTime.clear();
        tfSimulations.clear();
        tfSteps.clear();
        cbTraces.getItems().clear();
        switch (type) {
            case TIMING:
                inputBox.getChildren().add(0, timeBox);
                break;
			case ESTIMATION:
				// TODO
			case HYPOTHESIS_TEST:
                inputBox.getChildren().add(0, monteCarloBox);
                break;
            case TRACE_REPLAY:
            	cbTraces.getItems().addAll(injector.getInstance(TraceViewHandler.class).getTraces());
            	inputBox.getChildren().add(0, tracesBox);
                break;
        }
    }

	@FXML
	public void cancel() {
		this.close();
	}

	public void reset() {
        btAdd.setText(bundle.getString("common.buttons.add"));
        btCheck.setText(bundle.getString("simulation.buttons.addAndCheck"));
        tfTime.clear();
        tfSimulations.clear();
        tfSteps.clear();
        cbTraces.getItems().clear();
	}

	public void setPath(Path path) {
		this.path = path;
		simulationItemHandler.setPath(path);
	}

}
