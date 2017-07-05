import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.preferences.PreferencesStage;
import javafx.scene.Parent;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class PreferencesTest extends GuiTest{

    boolean mainStage = true;

    @Override
    public Parent getRootNode(){
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        runtimeOptions.setProject(null);
        runtimeOptions.setRunconfig(null);
        runtimeOptions.setResetPreferences(true);
        Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module(runtimeOptions));
        if(mainStage) {
            return injector.getInstance(MainController.class);
        } else{
            final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
            preferencesStage.showAndWait();
            preferencesStage.toFront();
            return preferencesStage.getScene().getRoot();
        }
    }

    @Test
    public void fileMenuTest() throws Exception{
        click("#editMenu");
        click("#preferencesItem");
        mainStage = false;
        //Give Preferences Stage time to set up
        sleep(3000);
        click("#defaultLocationField");
        type("FXMLTestingxyz");
        Spinner spinner = find("#recentProjectsCountSpinner");
        int recentProjectsCount = Integer.parseInt(spinner.getValue().toString());
        spinner.decrement(30);
        spinner.increment(100);
        if(recentProjectsCount < 0 || recentProjectsCount > 50){
            throw new Exception("Spinner value should be between 0 and 50.");
        }
        click("#recentProjectsCountSpinner");
        click("#preferences");
        click("#general");
        click("#preferences");
        click("#tvValue");
        click("#undoButton");
        click("#resetButton");
        click("#applyButton");
        click("#general");
        click("#defaultLocationButton");
    }

}
