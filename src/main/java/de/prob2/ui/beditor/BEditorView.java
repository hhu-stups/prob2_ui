package de.prob2.ui.beditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Singleton
public class BEditorView extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
    private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");

    @FXML
    private BEditor beditor;

    private Path path;

    @Inject
    private BEditorView(StageManager stageManager) {
        stageManager.loadFXML(this, "beditorView.fxml");
    }

    public void clearEditorText(){
        this.path = null;
        beditor.clear();
        beditor.stopHighlighting();
    }


    public void setEditorText(String text, Path path) {
        this.path = path;
        beditor.clear();
        beditor.appendText(text);
        beditor.getStyleClass().add("editor");
        beditor.startHighlighting();
    }

    @FXML
    public void handleSave() {
        //Maybe add something for the user, that reloads the machine automatically?
        if(path != null) {
            try {
                Files.write(path, beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                LOGGER.error("File not found", e);
            }
        }
    }

    @FXML
    public void handleSaveAs() {
        if(path != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Location");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));
            File openFile = fileChooser.showOpenDialog(getScene().getWindow());
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
                    LOGGER.error("File not found", e);
                }
            }
        }
    }

}
