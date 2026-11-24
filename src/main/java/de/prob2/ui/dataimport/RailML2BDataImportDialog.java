package de.prob2.ui.dataimport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.hhu.stups.railml2b.RailML2B;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

import java.nio.file.Path;

@Singleton
public class RailML2BDataImportDialog extends DataImportDialog {

	private final RailML2BDataImportOptions options;

	@Inject
	public RailML2BDataImportDialog(final StageManager stageManager, final I18n i18n,
	                                final FileChooserManager fileChooserManager, final RailML2BDataImportOptions options) {
		super(fileChooserManager, i18n, stageManager, ImportType.RAILML);
		this.options = options;
		stageManager.loadFXML(this, "data_import_dialog.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		options.setController(this);
		directory.addListener((obs, o, n) -> {
			if (n != null) {
				options.setDirectory(n);
			}
		});
		dialogOptions.getChildren().addAll(options.getChildren());
		version.setText("railML2B " + RailML2B.getVersion());

		this.setOnShown(e -> options.sizeToScene());
		this.setOnCloseRequest(e -> options.cancel());
	}

	@Override
	public void selectFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.filechooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter(true));
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent());
		setPath(path);
	}

	public void setPath(Path path) {
		if (path != null) {
			file.set(path.toFile());
			options.initializeForPath(path);
			if (directory.get() != null) {
				options.setDirectory(directory.get());
			} else {
				directory.set(path.toAbsolutePath().getParent());
			}
			checkAndUpdateMachineName(false);
		}
	}

	@Override
	public void importImplementation() {
		if (options.importFinished()) {
			options.generateAndFinish();
		} else {
			options.startImport();
		}
	}

	@Override
	boolean confirmMachineReplace() {
		return false;
	}

}
