import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import javafx.scene.Parent;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class TitlePaneClickTest extends GuiTest{

    @Override
    public Parent getRootNode(){
        RuntimeOptions runtimeOptions = new RuntimeOptions(null, null, false, false);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new ProB2Module(runtimeOptions));
        return injector.getInstance(MainController.class);
    }

    @Test
    public void clickTest() throws Exception{
        sleep(3000);
        click("#statsTP");
        click("#historyTP");
        click("#projectTP");
        click("#operationsTP");
        click("#verificationsTP");
        click("#operationsTP");
        click("#projectTP");
        click("#statsTP");
        click("#operationsTP");
        click("#verificationsTP");
        click("#projectTP");
        click("#verticalSP");
    }
}
