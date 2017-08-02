import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.apache.commons.cli.*;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MachineTest extends GuiTest{
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineTest.class);


    @Override
    public Parent getRootNode(){
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        //Set this to a lift0.json file..
        //Set this to default
        String args[] = new String[]{"--project", "src/test/res/Lift/Lift0.json", "--runconfig", "lift0.default", "--reset-preferences"};
        try{
            runtimeOptions = parseRuntimeOptions(args);
        } catch (Exception e){
            e.printStackTrace();
        }

        Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module(runtimeOptions));
        CurrentProject currentProject = injector.getInstance(CurrentProject.class);
        if (runtimeOptions.getProject() != null) {
            injector.getInstance(ProjectManager.class).openProject(new File(runtimeOptions.getProject()));
        }

        if (runtimeOptions.getRunconfig() != null) {
            Runconfiguration found = null;
            for (final Runconfiguration r : currentProject.getRunconfigurations()) {
                if (r.getName().equals(runtimeOptions.getRunconfig())) {
                    found = r;
                    break;
                }
            }

            if (found == null) {
                return injector.getInstance(MainController.class);
            } else {
                currentProject.startAnimation(found);
            }
        }

        return injector.getInstance(MainController.class);
    }

    @Test
    public void randomEventsInMachineTest() throws Exception{
        sleep(6000);
        click("#projectTP");
        click("#projectTab");
        click("#machinesTab");
        doubleClick("#machines-item-name");
        click("#closeDescriptionButton");
        click("#preferencesTab");
        click("#preferencesListView");
        doubleClick((Node) find("Design"));
        click("#closePreferenceViewButton");
        click("#verificationsTP");

        //triggering relatively random events
        click((Node) find("SETUP_CONSTANTS"));
        sleep(500);
        click((Node) find("INITIALISATION(level=L0)"));
        sleep(500);
        doubleClick((Node) find("CONSTANTS"));
        for(int i = 0; i < 5; i++) {
            click((Node) find("up"));
            sleep(500);
        }
        for(int i = 0; i < 4; i++) {
            click((Node) find("randomCrazyJump(L" + i + ")"));
            sleep(500);
        }
        click((Node) find("down"));

        //FIXME: Cannot be tested if window is too small because button is not visible
        //click("#runconfigurationsTab");
        //click("#runconfigurationsListView");
        //click("#addRunconfigButton");
        //press(KeyCode.ENTER);
    }

    /**
     * Copied from ProB2
     * @throws Exception if runconfig is not valid
     */
    private static RuntimeOptions parseRuntimeOptions(final String[] args) throws Exception{
        LOGGER.info("Parsing arguments: {}", (Object)args);

        final Options options = new Options();

        options.addOption(null, "project", true, "Open the specified project on startup.");
        options.addOption(null, "runconfig", true, "Run the specified run configuration on startup. Requires a project to be loaded first (using --open-project).");
        options.addOption(null, "reset-preferences", false, "Reset all preferences to their defaults.");

        final CommandLineParser clParser = new PosixParser();
        final CommandLine cl;
        try {
            cl = clParser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Failed to parse command line", e);
            throw new Exception(e.getLocalizedMessage());
        }
        LOGGER.info("Parsed command line: args {}, options {}", cl.getArgs(), cl.getOptions());

        if (!cl.getArgList().isEmpty()) {
            throw new Exception("Positional arguments are not allowed: " + cl.getArgList());
        }

        if (cl.hasOption("runconfig") && !cl.hasOption("project")) {
            throw new Exception("Invalid combination of options: --runconfig requires --project");
        }

        final RuntimeOptions runtimeOpts = new RuntimeOptions();
        runtimeOpts.setProject(cl.getOptionValue("project"));
        runtimeOpts.setRunconfig(cl.getOptionValue("runconfig"));
        runtimeOpts.setResetPreferences(cl.hasOption("reset-preferences"));
        LOGGER.info("Created runtime options: {}", runtimeOpts);

        return runtimeOpts;
    }
}
