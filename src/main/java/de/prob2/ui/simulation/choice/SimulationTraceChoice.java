package de.prob2.ui.simulation.choice;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.TraceViewHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FXMLInjected
public class SimulationTraceChoice extends GridPane {

    private SimulationChoosingStage choosingStage;

    @FXML
    private Label lbTime;

    @FXML
    private TextField tfTime;

    @FXML
    private ChoiceBox<ReplayTrace> cbTraces;

    @FXML
    private CheckBox cbTime;


    @Inject
    private SimulationTraceChoice(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "simulation_trace_choice.fxml");
    }

    @FXML
    private void initialize() {
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

        cbTime.selectedProperty().addListener((observable, from, to) -> {
            this.getChildren().removeAll(lbTime, tfTime);
            if(to) {
                this.add(lbTime, 1, 3);
                this.add(tfTime, 2, 3);
            }
            choosingStage.sizeToScene();
        });
    }

    public boolean checkSelection() {
        boolean timeIsNatural;
        try {
            int time = Integer.parseInt(tfTime.getText());
            timeIsNatural = time > 0;
        } catch (NumberFormatException e) {
            // TODO: Log Message
            timeIsNatural = false;
        }
        return cbTraces.getSelectionModel().getSelectedItem() != null && (!cbTime.isSelected() || timeIsNatural);
    }

    public Map<String, Object> extractInformation() {
        Map<String, Object> information = new HashMap<>();
        information.put("TRACE", cbTraces.getValue());
        if(cbTime.isSelected()) {
            information.put("TIME", Integer.parseInt(tfTime.getText()));
        }
        return information;
    }


    public void clear() {
        tfTime.clear();
        cbTraces.getSelectionModel().clearSelection();
        cbTraces.getItems().clear();
    }

    public void updateTraces(List<ReplayTrace> traces) {
        cbTraces.getItems().addAll(traces);
    }

    public void setSimulationChoosingStage(SimulationChoosingStage choosingStage) {
        this.choosingStage = choosingStage;
    }
}
