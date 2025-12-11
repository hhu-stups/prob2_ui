package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.command.ExportStateAsMachineCommand;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Singleton
@FXMLInjected
public final class ExportStateAsMachineStage extends Stage {
	private class IdentifierEntry {
		String id;
		SimpleBooleanProperty selected = new SimpleBooleanProperty(true);

		IdentifierEntry(String id) {
			this.id = id;
			this.selected.addListener((obs, ov, nv) -> identifiers.getSelectionModel().getSelectedItems().forEach(selected -> selected.setSelected(nv)));
		}

		void setSelected(boolean selected) {
			this.selected.set(selected);
		}

		@Override
		public String toString() {
			return id;
		}
	}

	@FXML
	private ListView<IdentifierEntry> identifiers;
	@FXML
	private CheckBox cbSelectAll;
	@FXML
	private Button btCreateExport;

	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final FileChooserManager fileChooserManager;
	private final StageManager stageManager;

	@Inject
	public ExportStateAsMachineStage(CurrentProject currentProject, CurrentTrace currentTrace,
	                                 FileChooserManager fileChooserManagerFileMenu, StageManager stageManager) {
		super();
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.fileChooserManager = fileChooserManagerFileMenu;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "export_state_as_machine_stage.fxml");
	}

	@FXML
	public void initialize() {
		initModality(Modality.APPLICATION_MODAL);
		initialiseForCurrentState();
		identifiers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		identifiers.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
		cbSelectAll.selectedProperty().addListener((obs, ov, nv) -> {
			identifiers.getItems().forEach(item -> item.setSelected(nv));
			identifiers.refresh();
		});
		btCreateExport.setOnAction(e -> this.handleExportCurrentState());
	}

	void initialiseForCurrentState() {
		this.identifiers.getItems().clear();
		List<String> allIdentifiers = new ArrayList<>();
		allIdentifiers.addAll(currentTrace.getStateSpace().getLoadedMachine().getConstantNames());
		allIdentifiers.addAll(currentTrace.getStateSpace().getLoadedMachine().getVariableNames());
		for (String identifier : allIdentifiers) {
			identifiers.getItems().add(new IdentifierEntry(identifier));
		}
		cbSelectAll.setSelected(true);
	}

	private void handleExportCurrentState() {
		if (!currentTrace.getCurrentState().isInitialised()) {
			stageManager.makeAlert(Alert.AlertType.ERROR, "menu.file.items.exportEntireModelAs.classicalBState.exception.header",
					"menu.file.items.exportEntireModelAs.classicalBState.exception.content").showAndWait();
			this.close();
			return;
		}

		String currentId = currentTrace.getCurrentState().getId();
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", "mch"),
				fileChooserManager.getAllExtensionsFilter()
		);
		chooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "_state_" + currentId);
		final Path path = fileChooserManager.showSaveFileChooser(chooser, FileChooserManager.Kind.NEW_MACHINE, this);
		if (path == null) {
			return; // don't close to keep selection
		}

		List<String> selectedIdentifiers = new ArrayList<>();
		identifiers.getItems().forEach(item -> {
			if (item.selected.getValue()) {
				selectedIdentifiers.add(item.id);
			}
		});
		ExportStateAsMachineCommand cmd = new ExportStateAsMachineCommand(path.toFile(), currentId, selectedIdentifiers);
		currentTrace.getStateSpace().execute(cmd);
		this.close();
	}
}
