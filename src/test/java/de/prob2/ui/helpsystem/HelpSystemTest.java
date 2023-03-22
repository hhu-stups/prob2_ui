package de.prob2.ui.helpsystem;

import static org.assertj.core.api.Assertions.assertThat;

import de.prob2.ui.MainController;
import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.matcher.base.WindowMatchers;

@DisplayName("The HelpButtons in the mainview lead to the correct Helppages")
public class HelpSystemTest extends TestBase {

	Stage stage;
	HelpSystem helpSystem;

	@Override
	public void start(Stage stage) {
		this.stage = stage;
		super.start(stage);
		helpSystem = injector.getInstance(HelpSystem.class);
		MainController main = injector.getInstance(MainController.class);
		stage.setScene(new Scene(main));
		stage.show();
	}

	@Test
	void history() {
		clickOn(lookup("#historyView").lookup("#helpButton").queryButton());
		assertThat(checkHelpPage("History", "History.html")).isTrue();
	}

	@Test
	void animation()  {
		clickOn(lookup("#animationView").lookup("#helpButton").queryButton());
		assertThat(checkHelpPage("Animation", "Animation.html#Trace")).isTrue();
	}

	@Test
	void operation() {
		clickOn(lookup("#operationsView").lookup("#helpButton").queryButton());
		assertThat(checkHelpPage("Operations", "Operations.html")).isTrue();
	}

	@Test
	void mainviewStatesView() {
		clickOn(lookup("#statesView").lookup("#helpButton").queryButton());
		assertThat(checkHelpPage("State View", "State%20View.html")).isTrue();
	}

	@Test
	void mainviewBEditor() {
		clickOn("#beditorTab");
		clickOn(lookup("#beditorView").lookup("#helpButton").queryButton());
		assertThat(checkHelpPage("Editor", "Editor.html")).isTrue();
	}

	boolean checkHelpPage(String title, String htmlFile) {
		FxAssert.verifyThat(window(helpSystem), WindowMatchers.isShowing());
		WebView webview = lookup("#webView").query();
		boolean titleEquals = webview.getEngine().getTitle().equals(title);
		boolean fileEquals = webview.getEngine().getLocation().endsWith(htmlFile);
		return fileEquals && titleEquals;
	}

	@Nested
	class loadedProjectNeeded {
		@BeforeEach
		void setup() {
			Platform.runLater(() ->
			{
				try {
					new ProjectBuilder(injector)
							.fromFile("src/test/resources/Lift.mch")
							.withAnimatedMachine("Lift")
							.build();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		}

		@Test
		void stats() {
			clickOn("#statsTP");
			clickOn(lookup("#statsView").lookup("#helpButton").queryButton());
			assertThat(checkHelpPage("Statistics", "Statistics.html")).isTrue();
		}

		@Test
		void verification() {
			clickOn("#verificationsTP");
			clickOn(lookup("#verificationsView").lookup("#helpButton").queryButton());
			assertThat(checkHelpPage("Verification", "Verification.html#Model")).isTrue();
		}


		@Test
		void project() {
			clickOn(lookup("#projectView").lookup("#helpButton").queryButton());
			assertThat(checkHelpPage("Project", "Project.html#Machines")).isTrue();
		}
	}
}
