package de.prob2.ui.operations;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.List;

class OperationDetailsStage extends Stage {
	private enum ValueType {
		PARAM, RETURN_VALUE
	}
	
	private static final class ValueItem {
		private final ValueType type;
		private final int index;
		private final String value;
		
		ValueItem(final ValueType type, final int index, final String value) {
			super();
			
			this.type = type;
			this.index = index;
			this.value = value;
		}
		
		public ValueType getType() {
			return type;
		}
		
		private int getIndex() {
			return index;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private static final class ValueCell extends ListCell<ValueItem> {
		private ValueCell() {
			super();
		}
		
		@Override
		protected void updateItem(final ValueItem item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (item == null || empty) {
				this.setText(null);
			} else {
				final String type;
				switch (item.getType()) {
					case PARAM:
						type = "Parameter";
						break;
					
					case RETURN_VALUE:
						type = "Return value";
						break;
					
					default:
						throw new IllegalArgumentException("Unhandled item type: " + item.getType());
				}
				this.setText(String.format("%s #%d: %s", type, item.getIndex(), item.getValue()));
			}
		}
	}
	
	@FXML private ListView<ValueItem> valuesListView;
	@FXML private TextArea textArea;
	
	private final ObjectProperty<OperationItem> item;
	
	@Inject
	OperationDetailsStage(final StageManager stageManager) {
		super();
		
		this.item = new SimpleObjectProperty<>(this, "item", null);
		
		stageManager.loadFXML(this, "operation_details_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.valuesListView.setCellFactory(lv -> new ValueCell());
		this.valuesListView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.textArea.setText(to == null ? null : to.getValue()));
		
		this.item.addListener((observable, from, to) -> {
			this.valuesListView.getItems().clear();
			if (to == null) {
				this.setTitle("Operation Details");
			} else {
				this.setTitle("Operation Details: " + to.getName());
				final List<String> params = to.getParams();
				for (int i = 0; i < params.size(); i++) {
					final String param = params.get(i);
					this.valuesListView.getItems().add(new ValueItem(ValueType.PARAM, i+1, param));
				}
				final List<String> returnValues = to.getReturnValues();
				for (int i = 0; i < returnValues.size(); i++) {
					final String retval = returnValues.get(i);
					this.valuesListView.getItems().add(new ValueItem(ValueType.RETURN_VALUE, i+1, retval));
				}
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
}
