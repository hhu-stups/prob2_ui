package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.ProBPreferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
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
	
	private final ObjectProperty<ProBPreferences> preferences;
	
	@Inject
	public DynamicPreferencesTableView(final StageManager stageManager) {
		super();
		this.preferences = new SimpleObjectProperty<>(this, "preferences");
		stageManager.loadFXML(this, "dynamic_preferences_table_view.fxml");
	}
	
	
	@FXML
	private void initialize() {
		tvName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tvChanged.setCellValueFactory(new PropertyValueFactory<>("changed"));
		tvValue.setCellFactory(col -> new DynamicTableCell(preferences));
		tvValue.setCellValueFactory(new PropertyValueFactory<>("value"));
		tvDefaultValue.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
		tvDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
	}
	
	public ObjectProperty<ProBPreferences> preferencesProperty() {
		return this.preferences;
	}
	
	public ProBPreferences getPreferences() {
		return this.preferencesProperty().get();
	}
	
	public void setPreferences(final ProBPreferences preferences) {
		this.preferencesProperty().set(preferences);
	}
	
	@Override
	public void refresh() {
		for (PrefItem item : this.getItems()) {
			String value = preferences.get().getPreferenceValue(item.getName());
			item.setValue(value);
			item.setChanged(value.equals(item.getDefaultValue()) ? "" : "*");
		}
	}

}
