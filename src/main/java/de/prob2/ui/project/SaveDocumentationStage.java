package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.beditor.BLexerSyntaxHighlighting;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.documentation.DocumentationProcessHandler;
import de.prob2.ui.documentation.MachineDocumentationItem;
import de.prob2.ui.documentation.ProjectDocumenter;
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

	@FXML
	private TableView<MachineDocumentationItem> tvDocumentation;

	@FXML
	private TableColumn<MachineDocumentationItem, Boolean> tvChecked;

	@FXML
	private TableColumn<MachineDocumentationItem, String> tvMachines;

	@FXML
	private CheckBox documentLTL;
	@FXML
	private CheckBox documentModelchecking;
	@FXML
	private CheckBox documentSymbolic;
	@FXML
	private CheckBox makePdf;
	@FXML
	private CheckBox printHtmlCode;
	private final ObservableList<MachineDocumentationItem> machineDocumentationItems = FXCollections.observableArrayList();
	private final Injector injector;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;
	private final StageManager stageManager;

	@Inject
	private SaveDocumentationStage(final FileChooserManager fileChooserManager, CurrentProject currentProject, final StageManager stageManager, I18n i18n, Injector injector) {
		this.fileChooserManager = fileChooserManager;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.injector = injector;
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "save_project_documentation_stage.fxml");
		currentProject.getMachines().forEach(machine -> machineDocumentationItems.add(new MachineDocumentationItem(Boolean.TRUE, machine)));
	}

	@FXML
	public void initialize() throws IOException {
		disableMakePdfIfPackageNotInstalled();
		finishButton.disableProperty().bind(filename.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(this.currentProject.getDefaultLocation().toString());
		filename.setText(this.currentProject.getName());
		// connect machines with checkboxes this helped: https://stackoverflow.com/questions/7217625/how-to-add-checkboxs-to-a-tableview-in-javafx
		tvMachines.setCellValueFactory(cell -> cell.getValue().getMachineItem().nameProperty());
		tvChecked.setCellFactory(tc -> new CheckBoxTableCell<>());
		tvChecked.setCellValueFactory(c -> {
			SimpleBooleanProperty property = new SimpleBooleanProperty(c.getValue().getDocument());
			property.addListener((observable, oldValue, newValue) -> c.getValue().setDocument(newValue));
			return property;
		});
		tvDocumentation.setItems(machineDocumentationItems);
	}

	private void disableMakePdfIfPackageNotInstalled() throws IOException {
		DocumentationProcessHandler.OS os = DocumentationProcessHandler.getOS();
		//Windows Script uses Powershell which is installed defaultly, so no check needed
		if(os == DocumentationProcessHandler.OS.LINUX || os == DocumentationProcessHandler.OS.MAC ){
			if(!DocumentationProcessHandler.packageInstalled("pdflatex")){
				makePdf.setText(i18n.translate("verifications.documentation.saveStage.pdfPackageNotInstalled"));
				makePdf.setDisable(true);
			}
		}
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
	void finish(){
		Path dir = Paths.get(locationField.getText());
		if (!dir.toFile().isDirectory()) {
			stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.newProjectStage.invalidLocationError").show();
			return;
		}
		List<Machine> checkedMachines = machineDocumentationItems.stream()
																.filter(MachineDocumentationItem::getDocument)
																.map(MachineDocumentationItem::getMachineItem)
																.collect(Collectors.toList());
		ProjectDocumenter documenter = new ProjectDocumenter(currentProject, i18n,
															 documentModelchecking.isSelected(),
															 documentLTL.isSelected(),
														     documentSymbolic.isSelected(),
														     makePdf.isSelected(),
															 printHtmlCode.isSelected(),
														     checkedMachines, dir, filename.getText(),injector);
		documenter.documentVelocity();
		//only proof of concept for bachelor thesis. can be deleted later
		//documenter.documentModelcheckingTableMarkdown();
		this.close();
	}
}
