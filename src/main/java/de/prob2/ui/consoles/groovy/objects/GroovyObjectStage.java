package de.prob2.ui.consoles.groovy.objects;

import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

@Singleton
public final class GroovyObjectStage extends Stage {
	@FXML private TableView<GroovyObjectItem> tvObjects;
	@FXML private TableColumn<GroovyObjectItem, String> objects;
	@FXML private TableColumn<GroovyObjectItem, String> classes;
	@FXML private TableColumn<GroovyObjectItem, String> values;

	private ObservableList<GroovyObjectItem> items = FXCollections.observableArrayList();

	private final StageManager stageManager;
	private final UIState uiState;

	@Inject
	private GroovyObjectStage(StageManager stageManager, UIState uiState) {
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "groovy_object_stage.fxml", this.getClass().getName());
		this.uiState = uiState;
	}

	@Override
	public void close() {
		items.forEach(GroovyObjectItem::close);
		uiState.getGroovyObjectTabs().clear();
		super.close();
	}

	public void showObjects(ScriptEngine engine) {
		items.clear();
		fillList(engine.getBindings(ScriptContext.GLOBAL_SCOPE));
		fillList(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		tvObjects.refresh();
		this.show();
	}

	private void fillList(Bindings binding) {
		for (final Map.Entry<String, Object> entry : binding.entrySet()) {
			if (entry == null || entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			GroovyClassStage stage = new GroovyClassStage(stageManager, uiState, entry.getKey());
			items.add(new GroovyObjectItem(entry.getKey(), entry.getValue(), stage, uiState));
		}
	}

	@FXML
	public void initialize() {
		objects.setCellValueFactory(new PropertyValueFactory<>("name"));
		classes.setCellValueFactory(new PropertyValueFactory<>("clazzname"));
		values.setCellValueFactory(new PropertyValueFactory<>("value"));

		tvObjects.setItems(items);

		tvObjects.setOnMouseClicked(e -> {
			int currentPos = tvObjects.getSelectionModel().getSelectedIndex();
			if (currentPos >= 0) {
				items.get(currentPos).show(GroovyObjectItem.ShowEnum.DEFAULT,0);
			}
			tvObjects.getSelectionModel().clearSelection();
		});
	}
	
	public List<GroovyObjectItem> getItems() {
		return items;
	}
}
