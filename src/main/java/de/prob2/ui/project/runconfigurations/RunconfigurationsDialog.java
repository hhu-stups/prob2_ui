package de.prob2.ui.project.runconfigurations;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;

public class RunconfigurationsDialog extends Dialog<Runconfiguration> {

	@FXML
	private ChoiceBox<Machine> machinesBox;
	@FXML
	private ComboBox<Preference> preferencesBox;
	@FXML
	private ButtonType okButtonType;
	private CurrentProject currentProject;

	@Inject
	private RunconfigurationsDialog(final StageManager stageManager, final CurrentProject currentProject) {
		super();

		this.currentProject = currentProject;

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new Runconfiguration(machinesBox.getValue().getName(), preferencesBox.getValue().getName());
			}
		});

		stageManager.loadFXML(this, "runconfigurations_dialog.fxml");
	}

	@FXML
	private void initialize() {
		this.setTitle("Add Runconfiguration");
		machinesBox.itemsProperty().bind(currentProject.machinesProperty());
		preferencesBox.getItems().add(new Preference("default", null));
		preferencesBox.getItems().addAll(currentProject.getPreferences());
		machinesBox.valueProperty().addListener((observable, from, to) -> {
			if (to != null) {
				List<String> prefs = new ArrayList<>();
				for (Runconfiguration runconfig : currentProject.getRunconfigurations(to)) {
					prefs.add(runconfig.getPreference());
				}			
				preferencesBox.getItems().clear();
				preferencesBox.getItems().add(new Preference("default", null));
				preferencesBox.getItems().addAll(currentProject.getPreferences());			
				List<Preference> remove = new ArrayList<>();
				for (Preference p : preferencesBox.getItems()) {
					if (prefs.contains(p.getName())) {
						remove.add(p);
					}
				}
				preferencesBox.getItems().removeAll(remove);
				preferencesBox.getSelectionModel().selectFirst();
			}
		});
		machinesBox.getSelectionModel().selectFirst();
		preferencesBox.getSelectionModel().selectFirst();
		this.getDialogPane().lookupButton(okButtonType).disableProperty()
				.bind(machinesBox.valueProperty().isNotNull().and(preferencesBox.valueProperty().isNotNull()).not());
	}
}
