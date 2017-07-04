import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.MainController;
import de.prob2.ui.internal.ProB2Module;
import javafx.scene.Parent;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class TitlePaneClickTest extends GuiTest{

    @Override
    public Parent getRootNode(){
        Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module());
        return injector.getInstance(MainController.class);
    }

    @Test
    public void clickTest() throws Exception{
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
