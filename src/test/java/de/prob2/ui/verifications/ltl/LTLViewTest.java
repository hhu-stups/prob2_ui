package de.prob2.ui.verifications.ltl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.ProB2;
import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.testfx.assertions.api.Assertions.assertThat;

class LTLViewTest extends ApplicationTest {

	Injector injector;

	LTLView ltlView;

	@Override
	public void start(final Stage stage) {
		RuntimeOptions runtimeOptions =new RuntimeOptions(null, null, null, null, false, false);
		ProB2Module module = new ProB2Module(new ProB2(), runtimeOptions);

		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);

		ltlView = injector.getInstance(LTLView.class);
		ltlView.initialize();
		stage.setScene(new Scene(ltlView));
		stage.show();
	}

	@Test
	@DisplayName("PatternTable ist invisible and unmanaged if empty with loaded Project")
	void PatternTable1() throws InterruptedException {
		new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").build();

		TableView<LTLPatternItem> patternTable = lookup("#tvPattern").query();
		assertThat(patternTable.isManaged()).isFalse();
		assertThat(patternTable.isVisible()).isFalse();
	}

	@Test
	@DisplayName("PatternTable ist Visible if Project has Patterns")
	void PatternTable2() throws InterruptedException {
		new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").build();
		LTLPatternItem item = new LTLPatternItem("", "", "");
		ltlView.addPatternToMachine(item, injector.getInstance(CurrentProject.class).getCurrentMachine());

		TableView<LTLPatternItem> patternTable = lookup("#tvPattern").query();
		assertThat(patternTable.isManaged()).isTrue();
		assertThat(patternTable.isVisible()).isTrue();
		assertThat(patternTable).hasExactlyNumRows(1);
	}
}
