import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import javafx.scene.Parent;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class FontSizeClickTest extends GuiTest {

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
    public void fontSizeTest() throws Exception{
        click("#viewMenu");
        click("#viewMenu_bigger");
        click("#viewMenu");
        click("#viewMenu_smaller");
        click("#viewMenu");
        click("#viewMenu_default");
        //This clicks right next to the actual menu, if the view is getting too big
        for(int i = 0; i < 10; i++){
            click("#viewMenu").click("#viewMenu_bigger");
        }
        for(int i = 0; i < 10; i++){
            click("#viewMenu").click("#viewMenu_smaller");
        }
        click("#viewMenu_default");
    }
}