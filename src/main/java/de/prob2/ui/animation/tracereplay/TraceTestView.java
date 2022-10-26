package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.check.tracereplay.OperationDisabledness;
import de.prob.check.tracereplay.OperationEnabledness;
import de.prob.check.tracereplay.OperationExecutability;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.Postcondition;
import de.prob.check.tracereplay.PostconditionPredicate;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.Checked;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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

import static de.prob2.ui.internal.TranslatableAdapter.enumNameAdapter;

@FXMLInjected
public class TraceTestView extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceTestView.class);

	private class TracePositionHighlightCell<S, T> extends TableCell<S, T> {
		private TracePositionHighlightCell() {
			super();
		}

		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("past", "present", "future"));

			final TableRow<S> tableRow = this.getTableRow();
			if (!empty && tableRow != null && currentTraceIsReplayedTrace()) {
				int index = tableRow.getIndex();
				final int currentIndex = currentTrace.get().getCurrent().getIndex();
				if (index < currentIndex) {
					this.getStyleClass().add("past");
				} else if (index > currentIndex) {
					this.getStyleClass().add("future");
				} else {
					this.getStyleClass().add("present");
				}
			}
		}
	}

	private final class PositionCell extends TraceTestView.TracePositionHighlightCell<PersistentTransition, String> {

		private PositionCell() {
			super();
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);

			final TableRow<PersistentTransition> tableRow = this.getTableRow();
			if (empty || tableRow == null) {
				this.setText(null);
			} else {
				int index = tableRow.getIndex();
				// Increment displayed index by 1 for consistency with the history view,
				// which displays the root state as "transition 0".
				// The trace table view doesn't show the root state,
				// so its numbering has to start at 1.
				this.setText(String.valueOf(index + 1));
			}
		}
	}

	private final class TransitionCell extends TraceTestView.TracePositionHighlightCell<PersistentTransition, String> {
		private TransitionCell() {
			super();
		}

		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);

			this.setText(empty ? null : item);
		}
	}

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
				transitionBoxes.set(index, box);

				Node btAddTest = buildAddButton(box, index);
				box.getChildren().add(btAddTest);

				for(int i = 0; i < tableItem.getPostconditions().size(); i++) {
					Postcondition postcondition = tableItem.getPostconditions().get(i);
					final HBox innerBox = buildInnerBox(box, postcondition, false, index, i);
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
				textArea.setPrefHeight(50);
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
	private TableColumn<PersistentTransition, String> positionColumn;
	@FXML
	private TableColumn<PersistentTransition, String> transitionColumn;
	@FXML
	private TableColumn<PersistentTransition, String> testColumn;
	@FXML
	private TableColumn<PersistentTransition, String> descriptionColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;

	private final FontSize fontSize;

	private final I18n i18n;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private SimpleObjectProperty<ReplayTrace> replayTrace;

	private final List<List<Postcondition>> postconditions = new ArrayList<>();

	private final List<String> descriptions = new ArrayList<>();

	private final List<VBox> transitionBoxes = new ArrayList<>();

	@Inject
	public TraceTestView(final StageManager stageManager, final FontSize fontSize,
						 final I18n i18n, final CurrentTrace currentTrace, final Injector injector) {
		this.stageManager = stageManager;
		this.fontSize = fontSize;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.replayTrace = new SimpleObjectProperty<>();
		stageManager.loadFXML(this, "trace_test_view.fxml");
	}

	@FXML
	private void initialize() {
		this.titleProperty().bind(i18n.translateBinding("animation.tracereplay.test.stage.title", Bindings.select(this.replayTrace, "name")));

		this.traceTableView.setRowFactory(param -> {
			final TableRow<PersistentTransition> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					goToPositionInReplayTrace(row.getIndex());
				}
			});
			return row;
		});

		positionColumn.setCellFactory(param -> new PositionCell());
		positionColumn.setCellValueFactory(features -> new SimpleStringProperty(""));
		transitionColumn.setCellFactory(param -> new TransitionCell());
		transitionColumn.setCellValueFactory(features -> new SimpleStringProperty(buildTransitionString(features.getValue())));
		testColumn.setCellFactory(param -> new TestCell());
		testColumn.setCellValueFactory(features -> new SimpleStringProperty(""));
		descriptionColumn.setCellFactory(param -> new TransitionDescriptionCell());
		descriptionColumn.setCellValueFactory(features -> new SimpleStringProperty(""));

		currentTrace.addListener((o, from, to) -> {
			if (currentTraceIsReplayedTrace()) {
				// Update highlighting of current transition
				traceTableView.refresh();
			}
		});
	}

	private boolean currentTraceIsReplayedTrace() {
		final Trace trace = currentTrace.get();
		final ReplayTrace replayed = replayTrace.get();
		return trace != null
			&& replayed != null
			&& replayed.getAnimatedReplayedTrace() != null
			&& safeListEquals(trace.getTransitionList(), replayed.getAnimatedReplayedTrace().getTransitionList());
	}

	private void goToPositionInReplayTrace(final int index) {
		if (currentTraceIsReplayedTrace()) {
			final Trace trace = currentTrace.get();
			if (index < trace.getTransitionList().size()) {
				currentTrace.set(trace.gotoPosition(index));
			}
		} else {
			this.saveTrace();
			injector.getInstance(TraceChecker.class).check(replayTrace.get(), true).thenAccept(r -> {
				if (index < r.getLoadedTrace().getTransitionList().size()) {
					currentTrace.set(r.getAnimatedReplayedTrace().gotoPosition(index));
				}
				traceTableView.refresh();
			});
		}
	}

	/**
	 * Workaround for the broken equals implementation of PersistentVector,
	 * which throws {@link ArrayIndexOutOfBoundsException}s for apparently no reason.
	 * This method converts one of the two lists to a plain {@link ArrayList},
	 * which has a properly working equals method.
	 * 
	 * @param left first list to compare
	 * @param right second list to compare
	 * @return whether the two lists are equal
	 * @param <E> element type of the lists
	 */
	private static <E> boolean safeListEquals(final List<E> left, final List<E> right) {
		return new ArrayList<>(left).equals(right);
	}

	private String buildTransitionString(PersistentTransition persistentTransition) {
		String opName = persistentTransition.getOperationName();
		if(persistentTransition.getParameters().isEmpty()) {
			return opName;
		} else {
			return String.format(Locale.ROOT, "%s(%s)", opName, persistentTransition.getParameters().entrySet()
					.stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(", ")));
		}
	}

	public void loadReplayTrace(ReplayTrace replayTrace) {
		this.postconditions.clear();
		this.replayTrace.set(replayTrace);
		traceTableView.getItems().clear();
		transitionBoxes.clear();

		final TraceJsonFile traceJsonFile;
		try {
			traceJsonFile = replayTrace.load();
		} catch (IOException e) {
			injector.getInstance(TraceFileHandler.class).showLoadError(replayTrace.getAbsoluteLocation(), e);
			return;
		}

		final List<PersistentTransition> transitions = traceJsonFile.getTransitionList();
		traceTableView.getItems().addAll(transitions);
		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			postconditions.add(new ArrayList<>(transition.getPostconditions()));
			descriptions.add(transition.getDescription());
			VBox box = new VBox();
			Node btAddTest = buildAddButton(box, i);
			box.getChildren().add(btAddTest);
			transitionBoxes.add(box);
		}
	}

	@FXML
	private void applyTest() {
		this.saveTrace();
		injector.getInstance(TraceChecker.class).check(replayTrace.get(), true);
		this.close();
	}

	public void saveTrace() {
		List<PersistentTransition> transitions = replayTrace.get().getLoadedTrace().getTransitionList();
		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			transition.getPostconditions().clear();
			transition.getPostconditions().addAll(postconditions.get(i));
			transition.setDescription(descriptions.get(i));
		}

		try {
			replayTrace.get().saveModified(replayTrace.get().getLoadedTrace().changeTrace(transitions));
		} catch (IOException | RuntimeException exc) {
			LOGGER.warn("Failed to save project (caused by saving a trace)", exc);
			stageManager.makeExceptionAlert(exc, "traceSave.buttons.saveTrace.error", "traceSave.buttons.saveTrace.error.msg").show();
		}
	}

	@FXML
	public void recordPostconditions() {
		List<PersistentTransition> transitions = traceTableView.getItems();
		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			for(Map.Entry<String, String> entry : transition.getDestinationStateVariables().entrySet()) {
				List<Postcondition> postconditionsForTransition = postconditions.get(i);
				String key = entry.getKey();
				String value = entry.getValue();
				PostconditionPredicate postcondition = new PostconditionPredicate(String.format(Locale.ROOT, "%s = %s", key, value));
				if(!postconditionsForTransition.contains(postcondition)) {
					postconditionsForTransition.add(postcondition);
					VBox box = transitionBoxes.get(i);
					final HBox innerBox = buildInnerBox(box, postcondition, true, i, postconditionsForTransition.size());
					box.getChildren().add(box.getChildren().size() - 1, innerBox);
				}
			}
		}
	}

	private Label buildRemoveButton(VBox box, HBox innerBox, Postcondition postcondition, int index) {
		final Label btRemoveTest = new Label();
		btRemoveTest.setAlignment(Pos.TOP_CENTER);
		btRemoveTest.getStyleClass().add("icon-dark");
		final BindableGlyph minusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.MINUS_CIRCLE);
		minusIcon.getStyleClass().add("status-icon");
		minusIcon.setPrefHeight(fontSize.getFontSize());
		btRemoveTest.setGraphic(minusIcon);
		btRemoveTest.setOnMouseClicked(e2 -> {
			box.getChildren().remove(innerBox);
			postconditions.get(index).remove(postcondition);
		});
		return btRemoveTest;
	}

	private TextField buildOperationPredicateTextField(Postcondition postcondition) {
		final TextField textField = new TextField("");
		HBox.setHgrow(textField, Priority.ALWAYS);
		textField.setPrefHeight(fontSize.getFontSize() * 1.5);
		String predicate = ((OperationExecutability) postcondition).getPredicate();
		textField.setText(predicate);
		textField.textProperty().addListener((o, from, to) -> {
			if (to != null) {
				((OperationExecutability) postcondition).setPredicate(to);
			}
		});
		return textField;
	}

	private TextField buildPostconditionTextField(Postcondition postcondition) {
		final TextField textField = new TextField("");
		HBox.setHgrow(textField, Priority.ALWAYS);
		textField.setPrefHeight(fontSize.getFontSize() * 1.5);

		switch (postcondition.getKind()) {
			case PREDICATE: {
				String predicate = ((PostconditionPredicate) postcondition).getPredicate();
				textField.setText(predicate);
				textField.textProperty().addListener((o, from, to) -> {
					if (to != null) {
						((PostconditionPredicate) postcondition).setPredicate(to);
					}
				});
				break;
			}
			case ENABLEDNESS:
			case DISABLEDNESS: {
				String operation = ((OperationExecutability) postcondition).getOperation();
				textField.setText(operation);
				textField.textProperty().addListener((o, from, to) -> {
					if (to != null) {
						((OperationExecutability) postcondition).setOperation(to);
					}
				});
				break;
			}
			default:
				throw new RuntimeException("Given postcondition kind does not exist: " + postcondition.getKind());
		}
		return textField;
	}

	private BindableGlyph buildStatusIcon() {
		final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		statusIcon.getStyleClass().add("status-icon");
		statusIcon.setPrefHeight(fontSize.getFontSize());
		statusIcon.setPrefWidth(fontSize.getFontSize()*1.5);
		return statusIcon;
	}

	private MenuButton buildAddButton(VBox box, int index) {
		final MenuButton btAddTest = new MenuButton("", new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
		btAddTest.getStyleClass().add("icon-dark");
		MenuItem addPredicate = new MenuItem(i18n.translate("animation.trace.replay.test.postcondition.addItem.predicate"));
		MenuItem addOperationEnabled = new MenuItem(i18n.translate("animation.trace.replay.test.postcondition.addItem.enabled"));
		MenuItem addOperationDisabled = new MenuItem(i18n.translate("animation.trace.replay.test.postcondition.addItem.disabled"));

		addPredicate.setOnAction(e1 -> {
			PostconditionPredicate postcondition = new PostconditionPredicate();
			postconditions.get(index).add(postcondition);
			final HBox innerBox = buildInnerBox(box, postcondition, true, index, postconditions.get(index).size());
			box.getChildren().add(box.getChildren().size() - 1, innerBox);
		});
		addOperationEnabled.setOnAction(e1 -> {
			OperationEnabledness postcondition = new OperationEnabledness();
			postconditions.get(index).add(postcondition);
			final HBox innerBox = buildInnerBox(box, postcondition, true, index, postconditions.get(index).size());
			box.getChildren().add(box.getChildren().size() - 1, innerBox);
		});
		addOperationDisabled.setOnAction(e1 -> {
			OperationDisabledness postcondition = new OperationDisabledness();
			postconditions.get(index).add(postcondition);
			final HBox innerBox = buildInnerBox(box, postcondition, true, index, postconditions.get(index).size());
			box.getChildren().add(box.getChildren().size() - 1, innerBox);
		});

		btAddTest.getItems().add(addPredicate);
		btAddTest.getItems().add(addOperationEnabled);
		btAddTest.getItems().add(addOperationDisabled);

		return btAddTest;
	}

	private HBox buildInnerBox(VBox box, Postcondition postcondition, boolean isNewBox, int index, int postconditionIndex) {
		final HBox innerBox = new HBox();
		innerBox.setSpacing(2);

		final Label typeLabel = new Label(i18n.translate(
				enumNameAdapter("animation.trace.replay.test.postcondition"),
				postcondition.getKind()
		));
		final TextField postconditionTextField = buildPostconditionTextField(postcondition);
		final Label btRemoveTest = buildRemoveButton(box, innerBox, postcondition, index);
		final BindableGlyph statusIcon = buildStatusIcon();

		final List<List<String>> transitionErrorMessages = replayTrace.get().getReplayedTrace().getTransitionErrorMessages();
		if (transitionErrorMessages.size() <= index || isNewBox) {
			TraceViewHandler.updateStatusIcon(statusIcon, Checked.NOT_CHECKED);
		} else {
			// TODO There's currently no good way to tell which errors belong to which postcondition.
			// For now, we display all postconditions as failed if there are any errors for the relevant transition.
			Checked status = transitionErrorMessages.get(index).isEmpty() ? Checked.SUCCESS : Checked.FAIL;
			replayTrace.get().checkedProperty().addListener((o, from, to) -> TraceViewHandler.updateStatusIcon(statusIcon, status));
			TraceViewHandler.updateStatusIcon(statusIcon, status);
		}

		innerBox.getChildren().add(typeLabel);
		innerBox.getChildren().add(postconditionTextField);

		if(postcondition instanceof OperationExecutability) {
			final Label withLabel = new Label(i18n.translate("animation.trace.replay.test.postcondition.with"));
			final TextField predicateTextField = buildOperationPredicateTextField(postcondition);
			innerBox.getChildren().add(withLabel);
			innerBox.getChildren().add(predicateTextField);
		}

		innerBox.getChildren().add(statusIcon);
		innerBox.getChildren().add(btRemoveTest);
		return innerBox;
	}

}
