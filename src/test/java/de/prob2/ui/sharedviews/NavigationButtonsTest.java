package de.prob2.ui.sharedviews;

import de.prob2.ui.MainController;
import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.testfx.assertions.api.Assertions.assertThat;

@Disabled
public class NavigationButtonsTest extends TestBase {
	@Override
	public void start(Stage stage) {
		super.start(stage);
		MainController main = super.injector.getInstance(MainController.class);
		stage.setScene(new Scene(main));
		stage.show();
	}
	@DisplayName("When no project is open, the navigation-buttons are disabled")
	@Test
	void test1() {
		assertThat(lookup("#fastBackButton").queryButton()).isDisabled();
		assertThat(lookup("#backButton").queryButton()).isDisabled();
		assertThat(lookup("#fastForwardButton").queryButton()).isDisabled();
		assertThat(lookup("#forwardButton").queryButton()).isDisabled();
		assertThat(lookup("#reloadButton").queryButton()).isDisabled();
	}

	@Disabled
	@DisplayName("When a project is open but no trace is loaded, only the forward and reload button is enabled")
	@Test
	void test2() {
		Platform.runLater(() -> {
			try {
				new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.mch")
						.withAnimatedMachine("Lift")
						.build();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		assertThat(lookup("#fastBackButton").queryButton()).isDisabled();
		assertThat(lookup("#backButton").queryButton()).isDisabled();
		assertThat(lookup("#fastForwardButton").queryButton()).isDisabled();
		assertThat(lookup("#forwardButton").queryButton()).isEnabled();
		assertThat(lookup("#reloadButton").queryButton()).isEnabled();
	}



}
