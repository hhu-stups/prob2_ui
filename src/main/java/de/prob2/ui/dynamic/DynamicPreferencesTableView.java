package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DynamicPreferencesTableView extends TableView<DynamicPreferencesItem> {
	
	@FXML
	private TableColumn<DynamicPreferencesItem, String> tvName;
	
	@FXML
	private TableColumn<DynamicPreferencesItem, String> tvChanged;
	
	@FXML
	private TableColumn<DynamicPreferencesItem, String> tvValue;
	
	@FXML
	private TableColumn<DynamicPreferencesItem, String> tvDefaultValue;
	
	@FXML
	private TableColumn<DynamicPreferencesItem, String> tvDescription;
	
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
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("prefValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
	}

}
