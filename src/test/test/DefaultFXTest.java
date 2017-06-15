import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class DefaultFXTest extends GuiTest{
    public Parent getRootNode(){
        //This is a default parent
        return new TextArea();
    }

    @Test
    public void defaultTestMethod(){

    }
}
