package de.prob2.ui.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.formula.PredicateBuilder;

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

@FXMLInjected
public final class PredicateBuilderView extends VBox {
	private final class ValueCell extends TableCell<String, String> {
		private ValueCell() {
			super();
		}
		
		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.setGraphic(null);
			} else {
				final TextField textField = new TextField(items.get(item));
				textField.textProperty().addListener((o, from, to) -> items.put(item, to));
				this.setGraphic(textField);
			}
		}
	}
	
	@FXML private TableView<String> table;
	@FXML private TableColumn<String, String> nameColumn;
	@FXML private TableColumn<String, String> valueColumn;
	@FXML private TextField predicateField;
	
	private final ObjectProperty<Node> placeholder;
	private final Map<String, String> items;
	
	@Inject
	private PredicateBuilderView(final StageManager stageManager) {
		super();
		
		this.placeholder = new SimpleObjectProperty<>(this, "placeholder", null);
		this.items = new LinkedHashMap<>();
		
		stageManager.loadFXML(this, "predicate_builder_view.fxml");
	}
	
	@FXML
	private void initialize() {
		this.table.placeholderProperty().bind(this.placeholderProperty());
		this.nameColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue()));
		this.valueColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue()));
		this.valueColumn.setCellFactory(col -> this.new ValueCell());
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
	
	public Map<String, String> getItems() {
		return new LinkedHashMap<>(this.items);
	}
	
	public void setItems(final Map<String, String> items) {
		this.items.clear();
		this.items.putAll(items);
		this.table.getItems().setAll(this.items.keySet());
	}
	
	public void setFromPredicate(final String predicate) {
		final String[] predicates = predicate.split(" & ");
		//Copy elements of items.keySet(), otherwise they will be lost due to items.clear()
		Set<String> keys = items.keySet().stream().collect(Collectors.toSet());
		int size = Math.min(items.size(), predicates.length);
		List<String> restPredicates = new ArrayList<>();
		items.clear();
		for (int i = 0; i < size; i++) {
			final String part = predicates[i];
			final String[] assignment = part.split("=");
			final String lhs = assignment[0];
			if(assignment.length <= 1 || !keys.contains(lhs)) {
				restPredicates.add(part);
				continue;
			}
			final String rhs = assignment[1];
			items.put(lhs, rhs);
		}
		table.refresh();
		for (int i = size; i < predicates.length; i++) {
			restPredicates.add(predicates[i]);
		}
		this.predicateField.setText(String.join(" & ", restPredicates));
	}
	
	public String getPredicate() {
		final PredicateBuilder builder = new PredicateBuilder();
		final Map<String, String> filteredItems = new LinkedHashMap<>();
		this.items.forEach((k, v) -> {
			if (!v.isEmpty()) {
				filteredItems.put(k, v);
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
