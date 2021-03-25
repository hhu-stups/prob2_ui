package de.prob2.ui.internal;

import com.google.inject.Inject;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class ProBFileHandler {

    protected final CurrentProject currentProject;
    protected final StageManager stageManager;
    protected final FileChooserManager fileChooserManager;
    protected final ResourceBundle bundle;
    protected final VersionInfo versionInfo;

    @Inject
    public ProBFileHandler(VersionInfo versionInfo, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
        this.versionInfo = versionInfo;
        this.currentProject = currentProject;
        this.stageManager = stageManager;
        this.fileChooserManager = fileChooserManager;
        this.bundle = bundle;
    }

    public boolean checkIfPathAlreadyContainsFiles(Path path, String prefix, String contentBundleKey) throws IOException {
        try (final Stream<Path> children = Files.list(path)) {
            if (children.anyMatch(p -> p.getFileName().toString().startsWith(prefix))) {
                // Directory already contains test case trace - ask if the user really wants to save here.
                final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", contentBundleKey, path).showAndWait();
                if (!selected.isPresent() || selected.get() != ButtonType.YES) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Path chooseDirectory(FileChooserManager.Kind kind, String titleKey){
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString(titleKey));
        return this.fileChooserManager.showDirectoryChooser(directoryChooser, kind, stageManager.getCurrent());
    }

    protected Path openSaveFileChooser(String titleKey, String extensionKey, FileChooserManager.Kind kind, String extension) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString(titleKey));
        fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + extension);
        fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter(extensionKey, extension));
        return this.fileChooserManager.showSaveFileChooser(fileChooser, kind, stageManager.getCurrent());
    }

}
