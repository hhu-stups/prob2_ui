package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

@FXMLInjected
public class OperationCoverageInputView extends VBox {
	
	public static class OperationTableItem {

		private String operation;

		private BooleanProperty selected;

		public OperationTableItem(String operation, boolean selected) {
			this.operation = operation;
			this.selected = new SimpleBooleanProperty(selected);
		}

		public String getOperation() {
			return operation;
		}

		public BooleanProperty selectedProperty() {
			return selected;
		}

		public boolean selected() {
			return selected.get();
		}
	}
	
	private static class OperationSelectedProperty implements Callback<TableColumn.CellDataFeatures<OperationTableItem, CheckBox>, ObservableValue<CheckBox>> {

		@Override
		public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<OperationTableItem, CheckBox> param) {
			OperationTableItem item = param.getValue();
			CheckBox checkBox = new CheckBox();
			checkBox.selectedProperty().setValue(item.selected());
			checkBox.selectedProperty().addListener((observable, from, to) -> item.selectedProperty().set(to));
			return new SimpleObjectProperty<>(checkBox);
		}

	}

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

	public void setItem(TestCaseGenerationItem item) {
		depthField.setText(String.valueOf(item.getMaxDepth()));
	}

	public void setTable(Collection<String> operations) {
		tvOperations.getItems().clear();
		tvOperations.getItems().addAll(operations.stream()
				.map(operation -> new OperationTableItem(operation, true))
				.collect(Collectors.toList()));
	}
}
