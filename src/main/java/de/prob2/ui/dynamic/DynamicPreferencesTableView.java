package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DynamicPreferencesTableView extends TableView<PrefItem> {
	
	@FXML
	private TableColumn<PrefItem, String> tvName;
	
	@FXML
	private TableColumn<PrefItem, String> tvChanged;
	
	@FXML
	private TableColumn<PrefItem, String> tvValue;
	
	@FXML
	private TableColumn<PrefItem, String> tvDefaultValue;
	
	@FXML
	private TableColumn<PrefItem, String> tvDescription;
	
	private final ProBPreferences proBPreferences;
	
	@Inject
	public DynamicPreferencesTableView(final StageManager stageManager, final ProBPreferences probPreferences,
									   final MachineLoader machineLoader) {
		super();
		this.proBPreferences = probPreferences;
		stageManager.loadFXML(this, "dynamic_preferences_table_view.fxml");
	}
	
	
	@FXML
	private void initialize() {
		tvName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tvChanged.setCellValueFactory(new PropertyValueFactory<>("changed"));
		tvValue.setCellFactory(col -> new DynamicTableCell(proBPreferences));
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
	}

}
