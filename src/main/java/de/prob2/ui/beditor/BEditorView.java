package de.prob2.ui.beditor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");

	@FXML
	private BEditor beditor;

	private Path path;
	private ResourceBundle bundle;

	@Inject
	private BEditorView(final StageManager stageManager, final ResourceBundle bundle, final StopActions stopActions) {
		stageManager.loadFXML(this, "beditorView.fxml");
		this.bundle = bundle;
		setHint();
		stopActions.add(beditor::stopHighlighting);
	}

	public void setHint(){
		this.path = null;
		beditor.clear();
		beditor.appendText(bundle.getString("beditor.hint"));
		beditor.getStyleClass().add("editor");
		beditor.startHighlighting();
		beditor.setEditable(false);
	}

	public void setEditorText(String text, Path path) {
		this.path = path;
		beditor.clear();
		beditor.appendText(text);
		beditor.getStyleClass().add("editor");
		beditor.startHighlighting();
		beditor.setEditable(true);
	}

	@FXML
	private void handleSave() {
		//Maybe add something for the user, that reloads the machine automatically?
		if(path != null) {
			try {
				Files.write(path, beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				LOGGER.error(bundle.getString("beditor.couldNotSaveFile"), e);
			}
		}
	}

	@FXML
	private void handleSaveAs() {
		if(path != null) {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("preferences.stage.tabs.general.selectLocation"));
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));
			File openFile = fileChooser.showSaveDialog(getScene().getWindow());
			if (openFile != null) {
				File newFile = new File(openFile.getAbsolutePath() + (openFile.getName().contains(".") ? "" : ".mch"));
				StandardOpenOption option = StandardOpenOption.CREATE;
				if (newFile.exists()) {
					option = StandardOpenOption.TRUNCATE_EXISTING;
				}
				try {
					Files.write(newFile.toPath(), beditor.getText().getBytes(EDITOR_CHARSET), option);
					path = newFile.toPath();
				} catch (IOException e) {
					LOGGER.error(bundle.getString("beditor.couldNotSaveFile"), e);
				}
			}
		}
	}

}
