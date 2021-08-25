package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

	private final CurrentTrace currentTrace;

	@FXML
	private TableView<OperationTableItem> tvOperations;

	@FXML
	private TableColumn<OperationTableItem, CheckBox> selectedColumn;
	@FXML
	private TableColumn<OperationTableItem, String> operationColumn;

	@FXML
	private Spinner<Integer> depthSpinner;

	@Inject
	private OperationCoverageInputView(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "test_case_generation_operation_coverage.fxml");
	}

	@FXML
	private void initialize() {
		selectedColumn.setCellValueFactory(new OperationSelectedProperty());
		operationColumn.setCellValueFactory(new PropertyValueFactory<>("operation"));
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> this.update(to));
		this.update(currentTrace.getStateSpace());
		depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 5));
		depthSpinner.getEditor().textProperty().addListener((observable, from, to) -> {
			if(to.isEmpty()) {
				depthSpinner.getEditor().setText("1");
			} else  if(!to.matches("[1-9]+")){
				depthSpinner.getEditor().setText(from);
			}
		});
	}

	private void update(final StateSpace to) {
		tvOperations.getItems().clear();
		if (to != null) {
			// operations are already collected to tvOperations, that is why the result is ignored
			to.getLoadedMachine().getOperationNames().stream()
				.map(operation -> new OperationTableItem(operation, true))
				.collect(Collectors.toCollection(tvOperations::getItems));
		}
	}

	public List<String> getOperations() {
		return tvOperations.getItems().stream()
				.filter(OperationTableItem::selected)
				.map(OperationTableItem::getOperation)
				.collect(Collectors.toList());
	}

	public int getDepth() {
		return depthSpinner.getValue();
	}

	public void setItem(OperationCoverageItem item) {
		depthSpinner.getValueFactory().setValue(item.getMaxDepth());
	}
}
