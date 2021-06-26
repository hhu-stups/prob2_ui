package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.Postcondition;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.FormalismType;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.Checked;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FXMLInjected
public class TraceTestView extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceTestView.class);

	private final class TestCell extends TableCell<PersistentTransition, String> {

		private TestCell() {
			super();
		}

		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);
			this.setGraphic(null);

			if (empty || item == null || this.getTableRow() == null || this.getTableRow().getItem() == null) {
				this.setGraphic(null);
			} else {
				final TableRow<PersistentTransition> tableRow = this.getTableRow();
				final PersistentTransition tableItem = tableRow.getItem();
				int index = tableRow.getIndex();

				final VBox box = new VBox();
				box.setSpacing(2);

				final Button btAddTest = new Button();
				btAddTest.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
				btAddTest.setOnAction(e1 -> {
					Postcondition postcondition = new Postcondition(Postcondition.PostconditionKind.PREDICATE);
					postconditions.get(index).add(postcondition);

					final HBox innerBox = new HBox();
					innerBox.setSpacing(2);
					final TextField textField = new TextField("");
					HBox.setHgrow(textField, Priority.ALWAYS);
					//TODO
					textField.setPrefHeight(fontSize.getFontSize() * 1.5);
					textField.textProperty().addListener((o, from, to) -> {
						if(to != null) {
							postcondition.setValue(to);
						}
					});

					final Label btRemoveTest = new Label();
					final BindableGlyph minusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.MINUS_CIRCLE);
					minusIcon.getStyleClass().add("status-icon");
					minusIcon.setPrefHeight(fontSize.getFontSize());
					btRemoveTest.setGraphic(minusIcon);
					btRemoveTest.setOnMouseClicked(e2 -> {
						box.getChildren().remove(innerBox);
						postconditions.get(index).remove(postcondition);
					});

					final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
					statusIcon.getStyleClass().add("status-icon");
					statusIcon.setPrefHeight(fontSize.getFontSize());
					statusIcon.setPrefWidth(fontSize.getFontSize()*1.5);
					TraceViewHandler.updateStatusIcon(statusIcon, Checked.NOT_CHECKED);

					innerBox.getChildren().add(textField);
					innerBox.getChildren().add(statusIcon);
					innerBox.getChildren().add(btRemoveTest);



					box.getChildren().add(box.getChildren().size() - 1, innerBox);
				});

				box.getChildren().add(btAddTest);

				for(int i = 0; i < tableItem.getPostconditions().size(); i++) {
					Postcondition postcondition = tableItem.getPostconditions().get(i);
					final HBox innerBox = new HBox();
					innerBox.setSpacing(2);
					final TextField textField = new TextField("");
					HBox.setHgrow(textField, Priority.ALWAYS);
					//TODO
					textField.setPrefHeight(fontSize.getFontSize() * 1.5);
					textField.setText(postcondition.getValue());

					textField.textProperty().addListener((o, from, to) -> {
						if(to != null) {
							postcondition.setValue(to);
						}
					});

					final Label btRemoveTest = new Label();
					btRemoveTest.setAlignment(Pos.TOP_CENTER);
					final BindableGlyph minusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.MINUS_CIRCLE);
					minusIcon.getStyleClass().add("status-icon");
					minusIcon.setPrefHeight(fontSize.getFontSize() * 1.5);
					btRemoveTest.setGraphic(minusIcon);
					btRemoveTest.setOnMouseClicked(e2 -> {
						box.getChildren().remove(innerBox);
						postconditions.get(index).remove(postcondition);
					});

					final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
					statusIcon.getStyleClass().add("status-icon");
					statusIcon.setPrefHeight(fontSize.getFontSize() * 1.5);
					statusIcon.setPrefWidth(fontSize.getFontSize()*1.5);

					if(replayTrace.getPostconditionStatus().isEmpty()) {
						TraceViewHandler.updateStatusIcon(statusIcon, Checked.NOT_CHECKED);
					} else {
						Checked status = replayTrace.getPostconditionStatus().get(index).get(i);
						replayTrace.checkedProperty().addListener((o, from, to) -> TraceViewHandler.updateStatusIcon(statusIcon, status));
						TraceViewHandler.updateStatusIcon(statusIcon, status);
					}

					innerBox.getChildren().add(textField);
					innerBox.getChildren().add(statusIcon);
					innerBox.getChildren().add(btRemoveTest);


					box.getChildren().add(box.getChildren().size() - 1, innerBox);
				}

				this.setGraphic(box);
			}
		}
	}

	private final class TransitionDescriptionCell extends TableCell<PersistentTransition, String> {

		private TransitionDescriptionCell() {
			super();
		}

		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);
			this.setGraphic(null);

			if (empty || item == null || this.getTableRow() == null || this.getTableRow().getItem() == null) {
				this.setGraphic(null);
			} else {
				final TableRow<PersistentTransition> tableRow = this.getTableRow();
				int index = tableRow.getIndex();
				final TextArea textArea = new TextArea();
				textArea.setStyle("-fx-control-inner-background: #f8f8f8; -fx-border-color: -prob-aqua; -fx-border-width: 2;");
				textArea.setText(descriptions.get(index));
				textArea.setPrefHeight(100);
				textArea.textProperty().addListener((o, from, to) -> {
					if(to != null) {
						descriptions.set(index, to);
					}
				});
				this.setGraphic(textArea);
			}
		}
	}

	@FXML
	private TableView<PersistentTransition> traceTableView;
	@FXML
	private TableColumn<PersistentTransition, String> transitionColumn;
	@FXML
	private TableColumn<PersistentTransition, String> testColumn;
	@FXML
	private TableColumn<PersistentTransition, String> descriptionColumn;
	@FXML
	private SplitPane splitPane;

	private final CurrentProject currentProject;

	private final StageManager stageManager;

	private final FontSize fontSize;

	private final Injector injector;

	private ReplayTrace replayTrace;

	private final List<List<Postcondition>> postconditions = new ArrayList<>();

	private final List<String> descriptions = new ArrayList<>();

	@Inject
	public TraceTestView(final CurrentProject currentProject, final StageManager stageManager, final FontSize fontSize,
						 final Injector injector) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fontSize = fontSize;
		this.injector = injector;
		stageManager.loadFXML(this, "trace_test_view.fxml");
	}

	@FXML
	private void initialize() {
		transitionColumn.setCellValueFactory(features -> new SimpleStringProperty(buildTransitionString(features.getValue())));
		testColumn.setCellFactory(param -> new TestCell());
		testColumn.setCellValueFactory(features -> new SimpleStringProperty(""));
		descriptionColumn.setCellFactory(param -> new TransitionDescriptionCell());
		descriptionColumn.setCellValueFactory(features -> new SimpleStringProperty(""));
	}

	private String buildTransitionString(PersistentTransition persistentTransition) {
		String opName = persistentTransition.getOperationName();
		if(persistentTransition.getParameters().isEmpty()) {
			return opName;
		} else {
			return String.format("%s(%s)", opName, persistentTransition.getParameters().entrySet()
					.stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(", ")));
		}
	}

	public void loadReplayTrace(ReplayTrace replayTrace) {
		this.postconditions.clear();
		this.replayTrace = replayTrace;
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		traceTableView.getItems().clear();
		if(persistentTrace != null) {
			traceTableView.getItems().addAll(persistentTrace.getTransitionList());
			persistentTrace.getTransitionList().forEach(transition -> {
				postconditions.add(transition.getPostconditions());
				descriptions.add(transition.getDescription());
			});
		}
	}

	@FXML
	private void applyTest() {
		List<PersistentTransition> transitions = replayTrace.getPersistentTrace().getTransitionList();
		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			transition.getPostconditions().clear();
			transition.getPostconditions().addAll(postconditions.get(i));
			transition.setDescription(descriptions.get(i));
		}
		this.saveTrace(transitions);
		injector.getInstance(TraceChecker.class).check(replayTrace, true);
		this.close();
	}

	public void saveTrace(List<PersistentTransition> transitions) {
		Path projectLocation = currentProject.getLocation();

		final Path tempLocation = projectLocation.resolve(replayTrace.getLocation() + ".tmp");
		try {
			TraceJsonFile traceJsonFile = new TraceJsonFile(new PersistentTrace(replayTrace.getDescription(), transitions), injector.getInstance(CurrentTrace.class).getStateSpace().getLoadedMachine(), TraceJsonFile.metadataBuilder().build());
			injector.getInstance(TraceFileHandler.class).save(traceJsonFile, tempLocation);
			Files.move(tempLocation, projectLocation.resolve(replayTrace.getLocation()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException exc) {
			LOGGER.warn("Failed to save project (caused by saving a trace)", exc);
			stageManager.makeExceptionAlert(exc, "traceSave.buttons.saveTrace.error", "traceSave.buttons.saveTrace.error.msg").show();
			try {
				Files.deleteIfExists(tempLocation);
			} catch (IOException e) {
				LOGGER.warn("Failed to delete temporary trace file after save error", e);
			}
		}

	}

}
