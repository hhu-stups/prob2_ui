package de.prob2.ui.groovy;

import java.io.IOException;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class GroovyObjectStage extends Stage {

	@FXML
	private TableView<GroovyObjectItem> tv_objects;

	@FXML
	private TableColumn<GroovyObjectItem, String> objects;

	@FXML
	private TableColumn<GroovyObjectItem, String> classes;

	@FXML
	private TableColumn<GroovyObjectItem, String> values;

	private ObservableList<GroovyObjectItem> items = FXCollections.observableArrayList();

	private FXMLLoader loader;
	private CurrentStage currentStage;
	private MetaPropertiesHandler groovyHandler;

	private Logger logger = LoggerFactory.getLogger(GroovyObjectStage.class);

	@Inject
	private GroovyObjectStage(FXMLLoader loader, CurrentStage currentStage, MetaPropertiesHandler groovyHandler) {
		this.loader = loader;
		try {
			loader.setLocation(getClass().getResource("groovy_object_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.setOnCloseRequest(e -> {
			close();
		});
		this.groovyHandler = groovyHandler;
		this.currentStage = currentStage;

		currentStage.register(this);
	}

	@Override
	public void close() {
		for (GroovyObjectItem item : items) {
			item.close();
		}
		super.close();
	}

	public void showObjects(ScriptEngine engine) {
		items.clear();
		fillList(engine.getBindings(ScriptContext.GLOBAL_SCOPE));
		fillList(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		tv_objects.refresh();
		this.show();
	}

	private void fillList(Bindings binding) {
		for (final Map.Entry<String, Object> entry : binding.entrySet()) {
			GroovyClassStage stage = new GroovyClassStage(loader, groovyHandler);
			currentStage.register(stage);
			items.add(new GroovyObjectItem(entry.getKey(), entry.getValue(), stage));
		}
	}

	@FXML
	public void initialize() {
		objects.setCellValueFactory(new PropertyValueFactory<>("name"));
		classes.setCellValueFactory(new PropertyValueFactory<>("clazzname"));
		values.setCellValueFactory(new PropertyValueFactory<>("value"));

		tv_objects.setItems(items);

		tv_objects.setOnMouseClicked(e -> {
			int currentPos = tv_objects.getSelectionModel().getSelectedIndex();
			if (currentPos >= 0) {
				items.get(currentPos).show();
			}
			tv_objects.getSelectionModel().clearSelection();
		});
	}

}
