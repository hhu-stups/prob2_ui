package de.prob2.ui.operations;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

class OperationDetailsStage extends Stage {
	private static final class ValueItem {
		private final String name;
		private final String value;
		
		ValueItem(final String name, final String value) {
			super();
			
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(value, "value");
			
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private final class ValueCell extends TreeCell<ValueItem> {
		private ValueCell() {
			super();
		}
		
		@Override
		protected void updateItem(final ValueItem item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (item == null || empty) {
				this.setText(null);
			} else {
				this.setText(item.getName());
			}
		}
	}
	
	@FXML private TreeView<ValueItem> valuesTreeView;
	@FXML private TreeItem<ValueItem> rootItem;
	@FXML private TreeItem<ValueItem> parametersItem;
	@FXML private TreeItem<ValueItem> returnValuesItem;
	@FXML private TreeItem<ValueItem> constantsItem;
	@FXML private TreeItem<ValueItem> variablesItem;
	@FXML private TextArea textArea;
	
	private final ResourceBundle bundle;
	
	private final ObjectProperty<OperationItem> item;
	
	@Inject
	OperationDetailsStage(final StageManager stageManager, final ResourceBundle bundle) {
		super();
		
		this.bundle = bundle;
		
		this.item = new SimpleObjectProperty<>(this, "item", null);
		
		stageManager.loadFXML(this, "operation_details_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.valuesTreeView.setCellFactory(lv -> new ValueCell());
		this.valuesTreeView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.textArea.setText(to == null ? null : to.getValue().getValue()));
		
		this.rootItem.setValue(new ValueItem("Values (this root item should be invisible)", ""));
		this.parametersItem.setValue(new ValueItem(bundle.getString("operationDetails.groups.parameters"), ""));
		this.returnValuesItem.setValue(new ValueItem(bundle.getString("operationDetails.groups.returnValues"), ""));
		this.constantsItem.setValue(new ValueItem(bundle.getString("operationDetails.groups.constants"), ""));
		this.variablesItem.setValue(new ValueItem(bundle.getString("operationDetails.groups.variables"), ""));
		
		this.item.addListener((observable, from, to) -> {
			this.rootItem.getChildren().clear();
			this.rootItem.getChildren().add(this.parametersItem);
			this.rootItem.getChildren().add(this.returnValuesItem);
			this.rootItem.getChildren().add(this.constantsItem);
			this.rootItem.getChildren().add(this.variablesItem);
			this.rootItem.getChildren().forEach(ti -> ti.getChildren().clear());
			if (to == null) {
				this.setTitle(bundle.getString("operationDetails.title"));
			} else {
				this.setTitle(String.format(bundle.getString("operationDetails.titleWithName"), to.getName()));
				
				final List<String> paramNames = to.getParameterNames();
				final List<String> paramValues = to.getParameterValues();
				for (int i = 0; i < paramValues.size(); i++) {
					String name = i < paramNames.size() ? paramNames.get(i) : "#" + (i + 1);
					final String param = paramValues.get(i);
					this.parametersItem.getChildren().add(new TreeItem<>(new ValueItem(name, param)));
				}
				
				final List<String> outputParamNames = to.getOutputParameterNames();
				final List<String> returnValues = to.getReturnValues();
				for (int i = 0; i < returnValues.size(); i++) {
					final String name = i < outputParamNames.size() ? outputParamNames.get(i) : "#" + (i + 1);
					final String retval = returnValues.get(i);
					this.returnValuesItem.getChildren().add(new TreeItem<>(new ValueItem(name, retval)));
				}
				
				to.getConstants().forEach((key, value) ->
					this.constantsItem.getChildren().add(new TreeItem<>(new ValueItem(key, value)))
				);
				
				to.getVariables().forEach((key, value) ->
					this.variablesItem.getChildren().add(new TreeItem<>(new ValueItem(key, value)))
				);
			}
			this.rootItem.getChildren().removeIf(ti -> ti.getChildren().isEmpty());
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
}
