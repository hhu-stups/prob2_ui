import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class DefaultFXTest extends GuiTest{
    public Parent getRootNode(){
        /*RuntimeOptions runtimeOptions = new RuntimeOptions(null, null, false, false);
        Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module(runtimeOptions));
        return injector.getInstance(TestClass.class);*/
        return new TextArea();
    }

    @Test
    public void defaultTestMethod(){
        //click, drag, type, press, etc..
    }
}
