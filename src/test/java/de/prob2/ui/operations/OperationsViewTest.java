package de.prob2.ui.operations;


import static org.testfx.assertions.api.Assertions.assertThat;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.testfx.assertions.api.Assertions;


public class OperationsViewTest extends TestBase {

	OperationsView operationsView;

	@Override
	public void start(Stage stage) {
		super.start(stage);
		operationsView = injector.getInstance(OperationsView.class);
	}

	@DisplayName("When no project is open, the listView is empty and the randomButton is disabled")
	@Test
	void test2() {
		assertThat(operationsView.randomButton).isDisabled();
		Assertions.assertThat(operationsView.opsListView).hasExactlyNumItems(0);
	}

	@Nested
	@DisplayName("When a machine is loaded ")
	class randomButtonTests {

		@BeforeEach
		void setup() throws InterruptedException {
			new ProjectBuilder(injector)
				.fromMachineFile("src/test/resources/Lift.mch")
				.withAnimatedMachine("Lift")
				.build();
		}

		@DisplayName("and the randomButton is clicked, 4 Options are shown")
		@Test
		void test1() {
			List<MenuItem> menuItems = operationsView.randomButton.getItems();
			List<String> menuItemTexts =
					menuItems.stream().map(MenuItem::getText).collect(Collectors.toList());

			assertThat(menuItems).hasSize(4);
			assertThat(menuItemTexts).containsExactly("Execute 1 Operation", "Execute 5 Operations",
					"Execute 10 Operations", null);
			assertThat(menuItems.get(3)).isInstanceOf(CustomMenuItem.class);
		}

		@Test
		@DisplayName("the navigation buttons correspond the the number of random executed transitions")
		void test2() {
			TextField textField = operationsView.randomText;
			textField.setText("3");
			operationsView.random(new ActionEvent(textField, null));
			// TODO?
		}


		@Test
		@DisplayName("the listView shows the correct number of possible operations")
		@RepeatedTest(5)
		void test3() throws InterruptedException {
			ToggleButton disabledOps = operationsView.disabledOpsToggle;
			disabledOps.fire();
			Thread.sleep(1000L);
			ListView<OperationItem> opsList = operationsView.opsListView;
			assertThat(opsList.getItems()).hasSize(3);
			disabledOps.fire();
			Thread.sleep(1000L);
			assertThat(opsList.getItems()).hasSize(1);
		}


		@DisplayName("the search toggle works correctly")
		@Test
		@RepeatedTest(5)
		void test4() throws InterruptedException {
			ToggleButton disabledOps = operationsView.disabledOpsToggle;
			disabledOps.fire();

			ToggleButton toggleButton = operationsView.searchToggle;
			toggleButton.fire();

			VBox searchBox = operationsView.searchBox;
			Assertions.assertThat(searchBox.isVisible()).isTrue();

			TextField searchBar = operationsView.searchBar;
			searchBar.setText("decrement");
			operationsView.applyFilter(searchBar.getText());

			ListView<OperationItem> opsList = operationsView.opsListView;
			Thread.sleep(1000L);
			assertThat(opsList.getItems()).hasSize(1);
			assertThat(opsList.getItems().get(0).toString()).contains(
				"decrement");

			toggleButton.fire();

			Assertions.assertThat(searchBox.isVisible()).isFalse();
			Assertions.assertThat(searchBar.getCharacters()).isEmpty();

			toggleButton.fire();
			searchBar.setText("foobar");
			operationsView.applyFilter(searchBar.getText());
			assertThat(opsList.getItems()).hasSize(0);
		}

		// does not specifiy, which menuItem is clicked. Just shows the availability and
		// reaction of the context menu in general, that is set for the operationItems.
		@Test
		@DisplayName("the context menu of the listitems works correctly")
		@RepeatedTest(5)
		void test5() throws InterruptedException {
			ListView<OperationItem> opsList = operationsView.opsListView;
			operationsView.executeOperationIfPossible(opsList.getItems().get(0));
			Thread.sleep(1000L);
			assertThat(opsList.getItems()).hasSize(2);

			ObservableList<MenuItem> menuItems =
				opsList.getCellFactory().call(opsList).getContextMenu().getItems();

			assertThat(menuItems).hasSize(2);

			assertThat(menuItems.get(0).getText()).isEqualTo("Show Details");
			assertThat(menuItems.get(1).getText()).isEqualTo("Execute by Predicate...");

			// TODO: assert that windows of contextmenuitems are showing
		}
	}

}
