import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import javafx.scene.Parent;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class MachineTest extends GuiTest{

    @Override
    public Parent getRootNode(){
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        runtimeOptions.setProject(null);
        runtimeOptions.setRunconfig(null);
        runtimeOptions.setResetPreferences(true);
        Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module(runtimeOptions));
        return injector.getInstance(MainController.class);
    }

    @Test
    public void newProjectTest() throws Exception{
        click("#helpButton");
    }
}
