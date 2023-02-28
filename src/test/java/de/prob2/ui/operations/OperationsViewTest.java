package de.prob2.ui.operations;


import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testfx.assertions.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;

import static org.testfx.assertions.api.Assertions.assertThat;

public class OperationsViewTest extends TestBase {

	OperationsView operationsView;

	@Override
	public void start(Stage stage) {
		super.start(stage);
		operationsView = injector.getInstance(OperationsView.class);
		stage.setScene(new Scene(operationsView));
		stage.show();
	}

	@DisplayName("When no project is open, the listView is empty and the randomButton is disabled")
	@Test
	void test2() {
		MenuButton randomButton = lookup("#randomButton").query();
		assertThat(randomButton).isDisabled();

		ListView<OperationItem> opList = lookup("#opsListView").query();
		Assertions.assertThat(opList).hasExactlyNumItems(0);
	}

	@Nested
	@DisplayName("When a machine is loaded ")
	class randomButtonTests {

		MenuButton randomButton;

		@BeforeEach
		void setup() throws InterruptedException {
			new ProjectBuilder(injector)
					.fromFile("src/test/resources/Lift.mch")
					.withAnimatedMachine("Lift")
					.build();
			randomButton = lookup("#randomButton").query();
		}

		@DisplayName("and the randomButton is clicked, 4 Options are shown")
		@Test
		void test1() {
			List<MenuItem> menuItems = randomButton.getItems();
			List<String> menuItemTexts = menuItems.stream().map(MenuItem::getText).collect(Collectors.toList());

			assertThat(menuItems).hasSize(4);
			assertThat(menuItemTexts).containsExactly("Execute 1 Operation", "Execute 5 Operations", "Execute 10 Operations", null);
			assertThat(menuItems.get(3)).isInstanceOf(CustomMenuItem.class);
		}

		@Test
		@DisplayName("the correct number of operations is executed")
		void test2() {
			clickOn(randomButton);
			TextField textField = lookup("#randomText").query();
			clickOn(textField).write("3").type(KeyCode.ENTER);
			Button backButton = lookup("#backButton").query();
			assertThat(backButton).isEnabled();
			clickOn(backButton);
			clickOn(backButton);
			clickOn(backButton);
			assertThat(backButton).isDisabled();
		}


		@Test
		@DisplayName("the listView shows the correct number of possible operations")
		void test3() {
			clickOn("#disabledOpsToggle");
			ListView<OperationItem> opsList = lookup("#opsListView").queryListView();
			assertThat(opsList.getItems()).hasSize(3);
			clickOn("#disabledOpsToggle");
			assertThat(opsList.getItems()).hasSize(1);
		}


		@DisplayName("the search toggle works correctly")
		@Test
		void test4() {
			clickOn("#disabledOpsToggle");
			ToggleButton toggleButton = lookup("#searchToggle").query();
			VBox searchBox = lookup("#searchBox").query();
			TextField textfield = lookup("#searchBar").query();

			clickOn(toggleButton);
			Assertions.assertThat(searchBox.isVisible()).isTrue();

			clickOn(textfield).write("decrement").type(KeyCode.ENTER);
			assertThat(lookup("#opsListView").queryListView()).hasExactlyNumItems(1);
			assertThat(lookup("#opsListView").queryListView().getItems().get(0).toString()).contains("decrement");

			clickOn(toggleButton);
			Assertions.assertThat(searchBox.isVisible()).isFalse();
			Assertions.assertThat(textfield.getCharacters()).isEmpty();

			clickOn(toggleButton);
			clickOn(textfield).write("schwubbeldidubbel").type(KeyCode.ENTER);
			assertThat(lookup("#opsListView").queryListView()).hasExactlyNumItems(0);

			clickOn(toggleButton);
			Assertions.assertThat(searchBox.isVisible()).isFalse();
			clickOn("#disabledOpsToggle");
		}


		@Test
		@DisplayName("the context menu of the listitems works correctly")
		void test5() throws InterruptedException {
			clickOn("#forwardButton");
			ListView<OperationItem> opsListView = lookup("#opsListView").query();
			// clickOn(opsListView.menuItem)
			/* TODO: Variante da oben funktioniert offensichtlich nicht, finde aber auch keine, die funktioniert,
			     da die MenuItems keine eigene Id haben und auch nicht über query() zugegriffen werden können */
		}

	}

}
