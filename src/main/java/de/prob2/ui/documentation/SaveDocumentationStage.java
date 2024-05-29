package de.prob2.ui.documentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
	public void initialize() throws IOException, InterruptedException {
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

	//this method is from  https://stackoverflow.com/questions/8488118/how-to-programatically-check-if-a-software-utility-is-installed-on-ubuntu-using
	//it checks if a command line package is installed
	private static boolean packageInstalled(String binaryName) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/which", binaryName);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		process.waitFor();
		String line = reader.readLine();
		return (line != null && !line.isEmpty());
	}

	private void disableMakePdfIfPackageNotInstalled() throws IOException, InterruptedException {
		//Windows Script uses Powershell which is installed defaultly, so no check needed
		if (!ProB2Module.IS_WINDOWS) {
			if (!packageInstalled("pdflatex")) {
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
		ProjectDocumenter documenter = new ProjectDocumenter(
			currentProject,
			i18n,
			documentModelchecking.isSelected(),
			documentLTL.isSelected(),
			documentSymbolic.isSelected(),
			makePdf.isSelected(),
			checkedMachines,
			dir,
			filename.getText(),
			injector
		);
		try {
			documenter.documentVelocity();
		} catch (IOException | RuntimeException exc) {
			stageManager.makeExceptionAlert(exc, "verifications.documentation.error").showAndWait();
		}
		this.close();
	}
}
