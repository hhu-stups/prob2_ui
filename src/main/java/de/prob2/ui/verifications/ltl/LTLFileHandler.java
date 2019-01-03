package de.prob2.ui.verifications.ltl;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.google.inject.Inject;
import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LTLFileHandler {

    private static final Charset LTL_CHARSET = StandardCharsets.UTF_8;
    private static final Logger LOGGER = LoggerFactory.getLogger(LTLFileHandler.class);
    private static final String LTL_FILE_ENDING = "*.ltl";


    private final Gson gson;
    private final CurrentProject currentProject;
    private final StageManager stageManager;
    private final ResourceBundle bundle;
    private final VersionInfo versionInfo;

    @Inject
    public LTLFileHandler(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo) {
        this.gson = gson;
        this.currentProject = currentProject;
        this.stageManager = stageManager;
        this.bundle = bundle;
        this.versionInfo = versionInfo;
    }

    public void saveLTL() {
        Machine machine = currentProject.getCurrentMachine();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("verifications.ltl.ltlView.fileChooser.saveLTL.title"));
        fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
        fileChooser.setInitialFileName(machine.getName() + LTL_FILE_ENDING.substring(1));
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        String.format(bundle.getString("common.fileChooser.fileTypes.ltl"), LTL_FILE_ENDING),
                        LTL_FILE_ENDING));
        File file = fileChooser.showSaveDialog(stageManager.getCurrent());

        if(file != null) {
            try (final Writer writer = Files.newBufferedWriter(file.toPath(), LTL_CHARSET)) {
                gson.toJson(new LTLData(machine.getLTLFormulas(), machine.getLTLPatterns()), writer);
                JsonObject metadata = new JsonObject();
                metadata.addProperty("Creation Date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy hh:mm:ssa O")));
                metadata.addProperty("ProB 2.0 kernel Version", versionInfo.getKernelVersion());
                metadata.addProperty("ProB CLI Version", versionInfo.getFormattedCliVersion());
                gson.toJson(metadata, writer);
            } catch (FileNotFoundException exc) {
                LOGGER.warn("Failed to create LTL file", exc);
                return;
            } catch (IOException exc) {
                LOGGER.warn("Failed to save LTL file", exc);
                return;
            }
        }
    }

    public LTLData loadLTL(Path path) throws InvalidFileFormatException, IOException {
        path = currentProject.get().getLocation().resolve(path);
        final Reader reader = Files.newBufferedReader(path, LTL_CHARSET);
        JsonStreamParser parser = new JsonStreamParser(reader);
        JsonElement element = parser.next();
        if (element.isJsonObject()) {
            LTLData data = gson.fromJson(element, LTLData.class);
            if(isValidData(data)) {
                return data;
            }
        }
        throw new InvalidFileFormatException("The file does not contain a LTL data set.");
    }

    private boolean isValidData(LTLData data) {
        return data.getFormulas() != null && data.getPatterns() != null;
    }

}
