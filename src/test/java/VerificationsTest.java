import java.io.File;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;
import javafx.scene.Node;
import javafx.scene.Parent;

import org.junit.Test;

import org.loadui.testfx.GuiTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerificationsTest extends GuiTest{
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationsTest.class);

	private boolean mainStage = true;

	@Override
	public Parent getRootNode(){
		RuntimeOptions runtimeOptions = new RuntimeOptions("src/test/resources/Lift/Lift0.json", "lift0", "default", false, false);
		Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProB2Module(runtimeOptions));
		CurrentProject currentProject = injector.getInstance(CurrentProject.class);

		injector.getInstance(ProjectManager.class).openProject(new File(runtimeOptions.getProject()));
		currentProject.startAnimation(new Runconfiguration(
			currentProject.get().getMachine(runtimeOptions.getMachine()),
			currentProject.get().getPreference(runtimeOptions.getPreference())
		));

		if(!mainStage){
			return injector.getInstance(ModelcheckingController.class);
		}
		return injector.getInstance(MainController.class);
	}

	@Test
	public void verificationsTest() throws Exception{
		sleep(6000);
		click("#verificationsTP");
		click((Node) find("SETUP_CONSTANTS"));
		sleep(500);
		click((Node) find("INITIALISATION(level=L0)"));
		click("#tabModelchecking");
		click("#addModelCheckButton");
		mainStage = false;
		click("#findDeadlocks");
		click("#startButton");
		mainStage = true;
		sleep(500);
		click("#tabLTLFormula");
		//Add test when fully implemented
	}
}
