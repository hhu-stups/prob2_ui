import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MachineTest extends GuiTest{
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineTest.class);


	@Override
	public Parent getRootNode(){
		final RuntimeOptions runtimeOptions = new RuntimeOptions("src/test/resources/Lift/Lift0.json", "lift0.default", false, false);
		Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProB2Module(runtimeOptions));
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
}
