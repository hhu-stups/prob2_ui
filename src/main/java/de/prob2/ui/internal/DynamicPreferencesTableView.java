package de.prob2.ui.internal;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DynamicPreferencesTableView extends TableView<DynamicPreferencesItem> {
	
	@FXML
	private TableColumn<DynamicCommandItem, String> tvName;
	
	@FXML
	private TableColumn<DynamicCommandItem, String> tvChanged;
	
	@FXML
	private TableColumn<DynamicCommandItem, String> tvValue;
	
	@FXML
	private TableColumn<DynamicCommandItem, String> tvDefaultValue;
	
	@FXML
	private TableColumn<DynamicCommandItem, String> tvDescription;
	
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
		tvValue.setCellFactory(col -> {
			TableCell<DynamicCommandItem, String> cell = new DynamicTableCell(proBPreferences);
			return cell;
		});
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("prefValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
	}

}
