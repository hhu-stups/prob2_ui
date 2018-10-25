package de.prob2.ui.dynamic;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
	
	private final ObjectProperty<ProBPreferences> proBPreferences;
	
	private final Injector injector;
	
	@Inject
	public DynamicPreferencesTableView(final StageManager stageManager, final ProBPreferences probPreferences,
									   final MachineLoader machineLoader, final Injector injector) {
		super();
		this.proBPreferences = new SimpleObjectProperty<>(this, "preferences", probPreferences);
		this.injector = injector;
		stageManager.loadFXML(this, "dynamic_preferences_table_view.fxml");
	}
	
	
	@FXML
	private void initialize() {
		tvName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tvChanged.setCellValueFactory(new PropertyValueFactory<>("changed"));
		tvValue.setCellFactory(col -> new DynamicTableCell(proBPreferences, injector));
		tvValue.setCellValueFactory(new PropertyValueFactory<>("value"));
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
	}
	
	@Override
	public void refresh() {
		for (PrefItem item : this.getItems()) {
			String value = proBPreferences.get().getPreferenceValue(item.getName());
			item.setValue(value);
			item.setChanged(value.equals(item.getDefaultValue()) ? "" : "*");
		}
	}

}
