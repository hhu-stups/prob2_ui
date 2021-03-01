package de.prob2.ui.simulation.simulators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.ObjectWithMetadata;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

@Singleton
public class SimulationSaver {

    public static final String SIMULATION_EXTENSION = "json";

    public static final String SIMULATION_TRACE_PREFIX = "Timed_Simulation_";

    private final StageManager stageManager;

    private final FileChooserManager fileChooserManager;

    private final JsonManager<SimulationConfiguration> jsonManager;

    private final SimulationCreator simulationCreator;

    private final CurrentProject currentProject;

    private final ResourceBundle bundle;

    @Inject
    public SimulationSaver(final StageManager stageManager, final FileChooserManager fileChooserManager, final JsonManager<SimulationConfiguration> jsonManager, final SimulationCreator simulationCreator,
                           final CurrentProject currentProject, final ResourceBundle bundle) {
        this.stageManager = stageManager;
        this.fileChooserManager = fileChooserManager;
        this.jsonManager = jsonManager;
        this.simulationCreator = simulationCreator;
        this.currentProject = currentProject;
        this.bundle = bundle;

        final Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        jsonManager.initContext(new JsonManager.Context<SimulationConfiguration>(gson, SimulationConfiguration.class, "Timed_Trace", 1) {
            @Override
            public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
                return new ObjectWithMetadata<>(oldObject, oldMetadata);
            }
        });
    }

    public void saveConfiguration(Trace trace, List<Integer> timestamps) throws IOException {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"));
        fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + SIMULATION_EXTENSION);
        fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Simulation", SIMULATION_EXTENSION));
        final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
        if (path != null) {
            SimulationConfiguration configuration = simulationCreator.createConfiguration(trace, timestamps, true);
            this.jsonManager.writeToFile(path, configuration);
        }
    }


    public void saveConfiguration(Trace trace, List<Integer> timestamps, Path location) throws IOException {
        SimulationConfiguration configuration = simulationCreator.createConfiguration(trace, timestamps, true);
        this.jsonManager.writeToFile(location, configuration);
    }

    public void saveConfigurations(SimulationItem item) {
        List<Trace> traces = item.getTraces();
        List<List<Integer>> timestamps = item.getTimestamps();

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.savePaths.title"));
        final Path path = this.fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
        if (path == null) {
            return;
        }

        try {
            try (final Stream<Path> children = Files.list(path)) {
                if (children.anyMatch(p -> p.getFileName().toString().startsWith(SIMULATION_TRACE_PREFIX))) {
                    // Directory already contains test case trace - ask if the user really wants to save here.
                    final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", "animation.testcase.save.directoryAlreadyContainsTestCases", path).showAndWait();
                    if (!selected.isPresent() || selected.get() != ButtonType.YES) {
                        return;
                    }
                }
            }

            int numberGeneratedTraces = traces.size();
            //Starts counting with 1 in the file name
            for(int i = 1; i <= numberGeneratedTraces; i++) {
                final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + i + "." + SIMULATION_EXTENSION);
                //String createdBy = "Simulation: " + item.getTypeAsName() + "; " + item.getConfiguration();
                // TODO: Metadata
                this.saveConfiguration(traces.get(i-1), timestamps.get(i-1), traceFilePath);
            }
        } catch (IOException e) {
            stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
            return;
        }
    }

}
