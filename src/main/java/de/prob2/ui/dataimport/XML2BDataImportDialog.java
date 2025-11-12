package de.prob2.ui.dataimport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.hhu.stups.xml2b.XML2B;
import de.hhu.stups.xml2b.XML2BOptions;
import de.prob.exception.ProBError;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.ProjectManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public final class XML2BDataImportDialog extends DataImportDialog {

	private HBox xsdBox;
	private CheckBox cbXSDSchema;
	private TextField xsdLocationField;
	private Tooltip xsdLocationTooltip;
	private CheckBox cbFastRw, cbOnlyDataUpdate;

	private final SimpleObjectProperty<File> xsdFile = new SimpleObjectProperty<>();

	private final ProjectManager projectManager;

	@Inject
	public XML2BDataImportDialog(FileChooserManager fileChooserManager, I18n i18n, StageManager stageManager,
	                             ProjectManager projectManager) {
		super(fileChooserManager, i18n, stageManager, ImportType.XML);
		this.projectManager = projectManager;
		stageManager.loadFXML(this, "data_import_dialog.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		this.addAdditionalOptions();

		this.version.setText("XML2B " + XML2B.getVersion());

		this.xsdBox.disableProperty().bind(cbXSDSchema.selectedProperty().not());
		this.xsdFile.addListener((obs, ov, nv) -> {
			if (nv != null) {
				xsdLocationField.setText(nv.getAbsolutePath());
				xsdLocationField.end();
			}
		});
		this.xsdLocationTooltip.textProperty().bind(xsdLocationField.textProperty());

		this.cbFastRw.setSelected(true);
		this.cbOnlyDataUpdate.setSelected(false);
	}

	private void addAdditionalOptions() {
		this.xsdBox = new HBox();
		this.xsdBox.setSpacing(10);

		this.cbXSDSchema = new CheckBox(i18n.translate("dataimport.dialog.options.xml.xsdSchema"));
		this.xsdLocationField = new TextField();
		this.xsdLocationField.setAlignment(Pos.CENTER_RIGHT);
		this.xsdLocationField.setEditable(false);
		HBox.setHgrow(xsdLocationField, Priority.ALWAYS);

		this.xsdLocationTooltip = new Tooltip();
		this.xsdLocationTooltip.setShowDuration(Duration.INDEFINITE);
		this.xsdLocationField.setTooltip(xsdLocationTooltip);

		Button selectXsd = new Button(i18n.translate("dataimport.dialog.fileChooser.title"));
		selectXsd.getStyleClass().add("button-dark2");
		selectXsd.setOnAction(e -> selectXSDFile());

		VBox.setMargin(xsdBox, new Insets(5,0,12.5,0));
		this.xsdBox.getChildren().addAll(this.xsdLocationField, selectXsd);

		this.cbFastRw = new CheckBox(i18n.translate("dataimport.dialog.options.xml.fastRw"));
		this.cbOnlyDataUpdate = new CheckBox(i18n.translate("dataimport.dialog.options.xml.onlyDataUpdate"));

		this.dialogOptions.getChildren().addAll(cbXSDSchema, xsdBox, cbFastRw, cbOnlyDataUpdate);
	}

	@Override
	void importImplementation() {
		try {
			XML2B xml2B = getXML2BWithOptions();
			if (cbOnlyDataUpdate.isSelected()) {
				xml2B.translate();
				Platform.runLater(this::close);
			} else {
				Path generated = xml2B.generateMachine();
				Platform.runLater(() -> {
					projectManager.openFile(generated);
					this.close();
				});
			}
		} catch (BCompoundException | IOException ex) {
			Platform.runLater(() -> {
				Alert alert = stageManager.makeExceptionAlert(new ProBError(ex), "dataimport.dialog.error.failed", "dataimport.dialog.error.failed.content");
				alert.initOwner(this);
				alert.showAndWait();
			});
		}
	}

	public void selectXSDFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("dataimport.dialog.fileChooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("dataimport.dialog.fileChooser.xsd", "xsd"));
		Path xsdPath = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent());
		if (xsdPath != null) {
			xsdFile.set(xsdPath.toFile());
		}
	}

	private XML2B getXML2BWithOptions() throws BCompoundException {
		XML2BOptions options = XML2BOptions.defaultOptions(file.get())
				.withDirectory(directory.get())
				.withMachineName(machineName.get())
				.withPrologSystem(cbFastRw.isSelected() ? XML2BOptions.SICSTUS_NAME : XML2BOptions.NONE_NAME);
		File xsdFileCB = cbXSDSchema.isSelected() ? xsdFile.get() : null;
		return new XML2B(file.get(), xsdFileCB, options);
	}

	@Override
	protected boolean confirmMachineReplace() {
		return !cbOnlyDataUpdate.isSelected();
	}

	@Override
	protected boolean additionalFileChecks() {
		Path dataPath = directory.get().resolve(machineName.get() + ".probdata");
		if (!cbOnlyDataUpdate.isSelected() && Files.exists(dataPath) && !super.confirmReplace(dataPath.toString())) {
			this.toFront();
			return false;
		}
		return true;
	}
}
