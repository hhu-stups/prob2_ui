package de.prob2.ui.animation.symbolic.testcasegeneration;


import com.google.inject.Inject;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;


@FXMLInjected
public class OperationCoverageInputView extends VBox {

    @FXML
    private TableView<OperationTableItem> tvOperations;

    @FXML
    private TableColumn<OperationTableItem, CheckBox> selectedColumn;
    @FXML
    private TableColumn<OperationTableItem, String> operationColumn;

    @FXML
    private TextField depthField;

    @Inject
    private OperationCoverageInputView(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "test_case_generation_operation_coverage.fxml");
    }

    @FXML
    private void initialize() {
        selectedColumn.setCellValueFactory(new OperationSelectedProperty());
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operation"));
    }

    public List<String> getOperations() {
        return tvOperations.getItems().stream()
                .filter(OperationTableItem::selected)
                .map(OperationTableItem::getOperation)
                .collect(Collectors.toList());
    }

    public String getDepth() {
        return depthField.getText();
    }

    public void reset() {
        depthField.clear();
    }

    public void setItem(SymbolicAnimationFormulaItem item) {
        depthField.setText(String.valueOf(item.getAdditionalInformation("maxDepth")));
    }

    public void setTable(List<String> operations) {
        tvOperations.getItems().clear();
        tvOperations.getItems().addAll(operations.stream()
                .map(operation -> new OperationTableItem(operation, true))
                .collect(Collectors.toList()));
    }
}
