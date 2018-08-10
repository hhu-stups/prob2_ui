package de.prob2.ui.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.formula.PredicateBuilder;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
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
	
	public void setItem(SymbolicAnimationFormulaItem item) {
		items.clear();
		String name = item.getName();
		String[] predicates = name.split(" & ");
		for(int i = 0; i < predicates.length-1; i++) {
			String predicate = predicates[i];
			String[] assignment = predicate.split("=");
			String lhs = assignment[0];
			String rhs = assignment[1];
			items.put(lhs, rhs);
		}
		this.predicateField.setText(predicates[predicates.length-1]);
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
}
