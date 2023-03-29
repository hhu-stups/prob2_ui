package de.prob2.ui.verifications.ltl;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.matcher.base.WindowMatchers;


import static org.testfx.assertions.api.Assertions.assertThat;

@Disabled
class LTLViewTest extends TestBase {
	LTLView ltlView;

	@Override
	public void start(final Stage stage) {
		super.start(stage);
		ltlView = super.injector.getInstance(LTLView.class);
		stage.setScene(new Scene(ltlView));
		stage.show();
	}

	@Test
	@DisplayName("PatternTable is invisible and unmanaged if empty with loaded Project")
	void PatternTable1() throws InterruptedException {
		new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.mch").build();

		TableView<LTLPatternItem> patternTable = lookup("#tvPattern").query();
		assertThat(patternTable.isManaged()).isFalse();
		assertThat(patternTable.isVisible()).isFalse();
	}

	@Test
	@DisplayName("PatternTable is Visible if Project has Patterns")
	void PatternTable2() throws InterruptedException {
		new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.mch").build();
		LTLPatternItem item = new LTLPatternItem("", "", "");
		ltlView.addPatternToMachine(item, injector.getInstance(CurrentProject.class).getCurrentMachine());

		TableView<LTLPatternItem> patternTable = lookup("#tvPattern").query();
		assertThat(patternTable.isManaged()).isTrue();
		assertThat(patternTable.isVisible()).isTrue();
		assertThat(patternTable).hasExactlyNumRows(1);
	}

	@Test
	@DisplayName("FormularTable shows all created LTL Formulas")
	void FormulaTable1() throws InterruptedException {
		new ProjectBuilder(injector)
				.fromMachineFile("src/test/resources/Lift.mch")
				.withLTLFormula(new LTLFormulaItem("", "", "",  false))
				.build();

//		new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").build();
//
//		LTLFormulaItem ltlFormulaItem = new LTLFormulaItem("", "", "",  false);
//		CurrentProject project = injector.getInstance(CurrentProject.class);
//		project.getCurrentMachine().ltlFormulasProperty().add(ltlFormulaItem);

		TableView<LTLPatternItem> patternTable = lookup("#itemsTable").query();
		assertThat(patternTable.isVisible()).isTrue();
		assertThat(patternTable).hasExactlyNumRows(1);
	}

	@Test
	@DisplayName("Stage to introduce new Patterns can be opened")
	void buttons1() throws InterruptedException {
		new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.mch").build();

		this.clickOn("#addMenuButton").clickOn("#addPatternButton");

		FxAssert.verifyThat(window("LTL Pattern"), WindowMatchers.isShowing());
	}

	@Test
	@DisplayName("Stage to introduce new Formula can be opened")
	void buttons2() throws InterruptedException {
		new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.mch").build();

		this.clickOn("#addMenuButton").clickOn("#addFormulaButton");

		FxAssert.verifyThat(window("LTL Formula"), WindowMatchers.isShowing());
	}
}
