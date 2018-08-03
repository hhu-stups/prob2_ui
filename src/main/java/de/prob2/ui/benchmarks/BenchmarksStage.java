package de.prob2.ui.benchmarks;


import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;



@Singleton
public class BenchmarksStage extends Stage {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksStage.class);

    @FXML
    private ChoiceBox<BenchmarkChoiceItem> cbOperations;

    @FXML
    private GridPane paramsGrid;

    @FXML
    private Label paramsLabel;

    @FXML
    private TableView<BenchmarkItem> benchmarksChoiceTableView;

    @FXML
    private TableColumn<BenchmarkItem, String> choicePositionColumn;

    @FXML
    private TableColumn<BenchmarkItem, String> choiceOperationColumn;

    @FXML
    private TableView<BenchmarkItem> benchmarksTableView;

    @FXML
    private TableColumn<BenchmarkItem, String> positionColumn;

    @FXML
    private TableColumn<BenchmarkItem, String> operationColumn;

    @FXML
    private TableColumn<BenchmarkItem, String> timeColumn;

    @FXML
    private TextField tfAddFrequency;

    @FXML
    private TextField tfExecFrequency;

    private final List<String> identifiers;

    private final List<TextField> valueTextFields;

    private final CurrentTrace currentTrace;

    private final CurrentProject currentProject;

    private final ResourceBundle bundle;

    @Inject
    private BenchmarksStage(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle, final CurrentProject currentProject) {
        this.currentTrace = currentTrace;
        this.currentProject = currentProject;
        this.bundle = bundle;
        this.identifiers = new ArrayList<>();
        this.valueTextFields = new ArrayList<>();
        stageManager.loadFXML(this, "benchmarks_view.fxml");
    }

    @FXML
    private void initialize() {
        choicePositionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        choiceOperationColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        currentProject.currentMachineProperty().addListener((observable, from, to) -> reset());
        benchmarksChoiceTableView.setRowFactory(table -> {
            final TableRow<BenchmarkItem> row = new TableRow<>();
            MenuItem removeItem = new MenuItem(bundle.getString("benchmarks.remove"));
            removeItem.setOnAction(e -> {
                BenchmarkItem currentItem = benchmarksChoiceTableView.getSelectionModel().getSelectedItem();
                benchmarksChoiceTableView.getItems().remove(currentItem);
            });
            removeItem.disableProperty().bind(row.emptyProperty());
            row.setContextMenu(new ContextMenu(removeItem));
            return row;
        });
        currentTrace.addListener((observable, from, to) -> {
            if(to == null) {
                return;
            }
            cbOperations.getItems().clear();
            final Set<Transition> operations = to.getNextTransitions(true, FormulaExpand.TRUNCATE);
            for (Transition transition : operations) {
                cbOperations.getItems().add(new BenchmarkChoiceItem(OperationItem.forTransition(to, transition)));
            }
        });
        cbOperations.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            this.paramsGrid.getChildren().clear();
            this.identifiers.clear();
            this.valueTextFields.clear();
            if(to == null) {
                return;
            }

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
        });
    }

    @FXML
    private void handleAdd() {
        int size = benchmarksChoiceTableView.getItems().size();
        BenchmarkChoiceItem item = cbOperations.getSelectionModel().getSelectedItem();
        if(item == null) {
            return;
        }
        int frequency = Integer.parseInt(tfAddFrequency.getText());
        for(int i = 0; i < frequency; i++) {
            benchmarksChoiceTableView.getItems().add(new BenchmarkItem(size + i + 1, item.getOperation()));
        }
    }

    @FXML
    private void handleExecute() {
        benchmarksTableView.getItems().clear();
        int frequency = Integer.parseInt(tfExecFrequency.getText());
        for(int i = 0; i < frequency; i++) {
            int size = benchmarksChoiceTableView.getItems().size();
            for(int j = 0; j < size; j++) {
                benchmarksTableView.getItems().add(new BenchmarkItem(i * size + j + 1, benchmarksChoiceTableView.getItems().get(j).getOperation()));
            }
        }
        for(BenchmarkItem item : benchmarksTableView.getItems()) {
            List<Transition> nextTransitions = currentTrace.getCurrentState().getOutTransitions().stream()
                    .filter(out -> item.getOperation().getTransition().getName().equals(out.getName()))
                    .collect(Collectors.toList());
            if(nextTransitions.size() > 0) {
                Transition nextTransition = nextTransitions.get(0);
                String name = nextTransition.getName();
                List<String> parameters = nextTransition.getParameterValues();
                Trace trace = currentTrace.get();
                long start = System.nanoTime();
                trace.addTransitionWith(name, parameters);
                long end = System.nanoTime();
                item.setTime(String.valueOf(end - start));
                currentTrace.set(trace.addTransitionWith(name, parameters));
            } else {
                item.setTime("-");
            }
        }
    }

    @FXML
    private void save() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("benchmarks.csv"));
        File file = fileChooser.showSaveDialog(new Stage());
        if(file == null) {
            return;
        }
        try {
            Files.write(toCSV(benchmarksTableView.getItems()).getBytes(), file);
        } catch (IOException e) {
            LOGGER.error("Saving as CSV failed", e);
        }
    }

    private String toCSV(List<BenchmarkItem> benchmarks) {
        return String.join("\n", benchmarks.stream()
                .map(item -> item.getPosition() + "," + item.getName() + "," + item.getTime())
                .collect(Collectors.toList()));
    }

    public void reset() {
        cbOperations.getItems().clear();
        benchmarksChoiceTableView.getItems().clear();
        benchmarksTableView.getItems().clear();
    }

}
