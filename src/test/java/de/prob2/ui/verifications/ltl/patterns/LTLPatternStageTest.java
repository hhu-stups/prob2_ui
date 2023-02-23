package de.prob2.ui.verifications.ltl.patterns;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LTLPatternStageTest extends TestBase {

	LTLPatternStage ltlPatternStage;

	@Override
	public void start(final Stage stage) {
		super.start(stage);
		ltlPatternStage = super.injector.getInstance(LTLPatternStage.class);
		stage.setScene(ltlPatternStage.getScene());
		stage.show();
	}

	@Test
	@DisplayName("Error TextArea is Empty when Stage is closed with valid Pattern")
	void PatternStage1() throws InterruptedException {
		new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").build();

		this.clickOn("#taCode").write("def test():GF([increment])");
		this.clickOn("#applyButton");

		TextArea textArea = lookup("#taErrors").query();
		assertThat(textArea.getText()).isEmpty();
	}

	@Test
	@DisplayName("Error TextArea shows Error when Stage is closed with invalid Pattern")
	void PatternStage2() throws InterruptedException {
		new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").build();

		this.clickOn("#taCode").write("test");
		this.clickOn("#applyButton");

		TextArea textArea = lookup("#taErrors").query();
		assertThat(textArea.getText()).isEqualTo("extraneous input 'test' expecting {<EOF>, 'def'}");
	}


}
