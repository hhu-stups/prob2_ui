package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesView;
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
	
	private final PreferencesView prefView;
	
	@Inject
	public DynamicPreferencesTableView(final StageManager stageManager, final ProBPreferences probPreferences,
									   final PreferencesView prefView, final MachineLoader machineLoader) {
		super();
		this.proBPreferences = new SimpleObjectProperty<>(this, "preferences", probPreferences);
		this.prefView = prefView;
		stageManager.loadFXML(this, "dynamic_preferences_table_view.fxml");
	}
	
	
	@FXML
	private void initialize() {
		tvName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tvChanged.setCellValueFactory(new PropertyValueFactory<>("changed"));
		tvValue.setCellFactory(col -> new DynamicTableCell(proBPreferences));
		tvValue.setCellValueFactory(new PropertyValueFactory<>("value"));
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
		tvValue.setOnEditCommit(event -> {
			proBPreferences.get().setPreferenceValue(event.getRowValue().getValue(), event.getNewValue());
			this.prefView.refresh();
		});
	}
	
	@Override
	public void refresh() {
		for (PrefItem item : this.getItems()) {
			String value = proBPreferences.get().getPreferenceValue(item.getName());
			item.setValue(value);
		}
	}

}
