package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import de.prob.formula.PredicateBuilder;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
public final class PredicateBuilderView extends VBox {
	private final class ValueCell extends TableCell<PredicateBuilderTableItem, String> {
		private ValueCell() {
			super();
		}
		
		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null || this.getTableRow() == null || this.getTableRow().getItem() == null) {
				this.setGraphic(null);
			} else {
				PredicateBuilderTableItem tableItem = this.getTableRow().getItem();
				final TextField textField = new TextField(tableItem.getValue());
				textField.textProperty().addListener((o, from, to) -> tableItem.setValue(to));
				this.setGraphic(textField);
			}
		}
	}
	
	@FXML private TableView<PredicateBuilderTableItem> table;
	@FXML private TableColumn<PredicateBuilderTableItem, String> nameColumn;
	@FXML private TableColumn<PredicateBuilderTableItem, String> valueColumn;
	@FXML private TableColumn<PredicateBuilderTableItem, String> typeColumn;
	@FXML private TextField predicateField;

	private final ResourceBundle bundle;
	private final ObjectProperty<Node> placeholder;
	private final List<PredicateBuilderTableItem> items;
	
	@Inject
	private PredicateBuilderView(final StageManager stageManager, final ResourceBundle bundle) {
		super();

		this.bundle = bundle;
		this.placeholder = new SimpleObjectProperty<>(this, "placeholder", null);
		this.items = new ArrayList<>();
		
		stageManager.loadFXML(this, "predicate_builder_view.fxml");
	}
	
	@FXML
	private void initialize() {
		this.table.placeholderProperty().bind(this.placeholderProperty());
		this.nameColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getName()));
		this.valueColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getValue()));
		this.valueColumn.setCellFactory(param -> this.new ValueCell());
		this.typeColumn.setCellValueFactory(features -> new SimpleStringProperty(bundle.getString(features.getValue().getType().getBundleKey())));
	}
	
	public ObjectProperty<Node> placeholderProperty() {
		return this.placeholder;
	}
	
	public Node getPlaceholder() {
		return this.placeholderProperty().get();
	}
	
	public void setPlaceholder(final Node placeholder) {
		this.placeholderProperty().set(placeholder);
	}
	
	public List<PredicateBuilderTableItem> getItems() {
		return new ArrayList<>(this.items);
	}
	
	public void setItems(final List<PredicateBuilderTableItem> items) {
		this.items.clear();
		this.items.addAll(items);
		this.table.getItems().setAll(this.items);
	}
	
	public void setFromPredicate(final String predicate) {
		// Clear the values of all items without removing them from the map.
		for (final PredicateBuilderTableItem entry : this.items) {
			entry.setValue("");
		}
		
		final String[] predicates = predicate.split(" & ");
		int firstNonAssignment = predicates.length;
		for (int i = 0; i < predicates.length; i++) {
			final String[] assignment = predicates[i].split("=");
			final String lhs = assignment[0];
			// Stop if this is not an assignment, the variable is unknown, or the variable already has a value.
			if (assignment.length <= 1 || this.items.stream().anyMatch(item -> !item.getName().equals(lhs) || !item.getValue().isEmpty())) {
				firstNonAssignment = i;
				break;
			}
			final String rhs = assignment[1];
			items.stream().filter(item -> item.getName().equals(lhs)).collect(Collectors.toList()).get(0).setValue(rhs);
		}
		table.refresh();
		// Store all remaining parts of the conjunction in the predicate field.
		this.predicateField.setText(String.join(" & ", Arrays.asList(predicates).subList(firstNonAssignment, predicates.length)));
	}
	
	public String getPredicate() {
		final PredicateBuilder builder = new PredicateBuilder();
		final Map<String, String> filteredItems = new LinkedHashMap<>();
		this.items.forEach(item -> {
			if(!item.getValue().isEmpty()) {
				filteredItems.put(item.getName(), item.getValue());
			}
		});
		builder.addMap(filteredItems);
		if (!this.predicateField.getText().isEmpty()) {
			builder.addList(Collections.singletonList(this.predicateField.getText()));
		}
		return builder.toString();
	}
	
	public void reset() {
		this.setFromPredicate("");
	}
}
