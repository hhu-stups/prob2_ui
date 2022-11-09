package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.documentation.MachineDocumentationItem;
import de.prob2.ui.documentation.VelocityDocumenter;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@FXMLInjected
public class SaveDocumentationStage extends Stage {

	@FXML
	private Button finishButton;
	@FXML
	private TextField filename;
	@FXML
	private final CurrentProject currentProject;
	@FXML
	private TextField locationField;
	@FXML
	private Label errorExplanationLabel;

	private final FileChooserManager fileChooserManager;
	private final I18n i18n;
	private final StageManager stageManager;
	@FXML
	private TableView<MachineDocumentationItem> tvDocumentation;

	@FXML
	private TableColumn<MachineDocumentationItem, Boolean> tvChecked;

	@FXML
	private TableColumn<MachineDocumentationItem, String> tvMachines;

	@FXML
	private CheckBox checkLTL;
	@FXML
	private CheckBox checkModelchecking;
	@FXML
	private CheckBox checkSymbolic;
	@FXML
	private CheckBox makePdf;
	private final ObservableList<MachineDocumentationItem> machineDocumentationItems = FXCollections.observableArrayList();
	private final Injector injector;

	@Inject
	private SaveDocumentationStage(final FileChooserManager fileChooserManager, CurrentProject currentProject, final StageManager stageManager, I18n i18n, Injector injector) {
		this.fileChooserManager = fileChooserManager;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.injector = injector;
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "save_modelchecking_stage.fxml");
		currentProject.getMachines().forEach(machine -> machineDocumentationItems.add(new MachineDocumentationItem(Boolean.TRUE, machine)));
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(filename.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(this.currentProject.getDefaultLocation().toString());
		filename.setText(this.currentProject.getName());
		tvMachines.setCellValueFactory(cell -> cell.getValue().getMachineItem().nameProperty());
		tvChecked.setCellFactory(tc -> new CheckBoxTableCell<>());
		tvChecked.setCellValueFactory(c -> {
			SimpleBooleanProperty property = new SimpleBooleanProperty(c.getValue().getDocument());
			property.addListener((observable, oldValue, newValue) -> c.getValue().setDocument(newValue));
			return property;
		});
		tvDocumentation.setItems(machineDocumentationItems);
	}
	@FXML
	void selectLocation() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle(i18n.translate("verifications.documentation.saveStage.label.title"));
		final Path path = fileChooserManager.showDirectoryChooser(dirChooser, null, this.getOwner());
		if (path != null) {
			locationField.setText(path.toString());
		}
	}

	@FXML
	void cancel() {
		this.close();
	}

	@FXML
	void finish() throws IOException, IllegalAccessException{
		Path dir = Paths.get(locationField.getText());
		if (!dir.toFile().isDirectory()) {
			stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.newProjectStage.invalidLocationError").show();
			return;
		}
		List<Machine> toDocumentMachines = machineDocumentationItems.stream()
																.filter(MachineDocumentationItem::getDocument)
																.map(MachineDocumentationItem::getMachineItem)
																.collect(Collectors.toList());
		VelocityDocumenter documenter = new VelocityDocumenter(currentProject,i18n, checkModelchecking.isSelected(), checkLTL.isSelected(), checkSymbolic.isSelected(),makePdf.isSelected(), toDocumentMachines, dir, filename.getText(),injector);
		documenter.documentVelocity();
		this.close();
	}
}
