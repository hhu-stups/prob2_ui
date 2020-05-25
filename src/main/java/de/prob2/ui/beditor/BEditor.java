package de.prob2.ui.beditor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class BEditor extends CodeArea {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditor.class);

	private ExecutorService executor;

	private final FontSize fontSize;

	private final CurrentProject currentProject;

	private final ResourceBundle bundle;

	private final ObservableList<ErrorItem.Location> errorLocations;

	@Inject
	private BEditor(final FontSize fontSize, final ResourceBundle bundle, final CurrentProject currentProject) {
		this.fontSize = fontSize;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.errorLocations = FXCollections.observableArrayList();
		initialize();
		initializeContextMenu();
	}

	private void initializeContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem undoItem = new MenuItem(bundle.getString("common.contextMenu.undo"));
		undoItem.setOnAction(e -> this.getUndoManager().undo());
		contextMenu.getItems().add(undoItem);

		final MenuItem redoItem = new MenuItem(bundle.getString("common.contextMenu.redo"));
		redoItem.setOnAction(e -> this.getUndoManager().redo());
		contextMenu.getItems().add(redoItem);

		final MenuItem cutItem = new MenuItem(bundle.getString("common.contextMenu.cut"));
		cutItem.setOnAction(e -> this.cut());
		contextMenu.getItems().add(cutItem);

		final MenuItem copyItem = new MenuItem(bundle.getString("common.contextMenu.copy"));
		copyItem.setOnAction(e -> this.copy());
		contextMenu.getItems().add(copyItem);

		final MenuItem pasteItem = new MenuItem(bundle.getString("common.contextMenu.paste"));
		pasteItem.setOnAction(e -> this.paste());
		contextMenu.getItems().add(pasteItem);

		final MenuItem deleteItem = new MenuItem(bundle.getString("common.contextMenu.delete"));
		deleteItem.setOnAction(e -> this.deleteText(this.getSelection()));
		contextMenu.getItems().add(deleteItem);

		final MenuItem selectAllItem = new MenuItem(bundle.getString("common.contextMenu.selectAll"));
		selectAllItem.setOnAction(e -> this.selectAll());
		contextMenu.getItems().add(selectAllItem);

		this.setContextMenu(contextMenu);
	}

	private void initialize() {
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			this.clear();
			this.appendText(bundle.getString("beditor.hint"));
		});
		this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		this.richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.successionEnds(Duration.ofMillis(100))
				.supplyTask(this::computeHighlightingAsync)
				.awaitLatest(this.richChanges())
				.filterMap(t -> {
					if (t.isSuccess()) {
						return Optional.of(t.get());
					} else {
						LOGGER.info("Highlighting failed", t.getFailure());
						return Optional.empty();
					}
				}).subscribe(highlighting -> {
			this.getErrorLocations().clear(); // Remove error highlighting if editor text changes
			this.applyHighlighting(highlighting);
		});
		this.errorLocations.addListener((ListChangeListener<ErrorItem.Location>) change ->
				this.applyHighlighting(computeHighlighting(this.getText(), currentProject.getCurrentMachine()))
		);

		fontSize.fontSizeProperty().addListener((observable, from, to) ->
				this.setStyle(String.format("-fx-font-size: %dpx;", to.intValue()))
		);
	}

	public void startHighlighting() {
		if (this.executor == null) {
			this.executor = Executors.newSingleThreadExecutor();
		}
	}

	public void stopHighlighting() {
		if (this.executor != null) {
			this.executor.shutdown();
			this.executor = null;
		}
	}

	private static <T> Collection<T> combineCollections(final Collection<T> a, final Collection<T> b) {
		final Collection<T> ret = new ArrayList<>(a);
		ret.addAll(b);
		return ret;
	}

	private StyleSpans<Collection<String>> addErrorHighlighting(final StyleSpans<Collection<String>> highlighting) {
		StyleSpans<Collection<String>> highlightingWithErrors = highlighting;
		for (final ErrorItem.Location location : this.getErrorLocations()) {
			final int startParagraph = location.getStartLine() - 1;
			final int endParagraph = location.getEndLine() - 1;
			final int startIndex = this.getAbsolutePosition(startParagraph, location.getStartColumn());
			final int endIndex;
			if (startParagraph == endParagraph) {
				final int displayedEndColumn = location.getStartColumn() == location.getEndColumn() ? location.getStartColumn() + 1 : location.getEndColumn();
				endIndex = this.getAbsolutePosition(startParagraph, displayedEndColumn);
			} else {
				endIndex = this.getAbsolutePosition(endParagraph, location.getEndColumn());
			}
			highlightingWithErrors = highlightingWithErrors.overlay(
					new StyleSpansBuilder<Collection<String>>()
							.add(Collections.emptyList(), startIndex)
							.add(Collections.singletonList("error"), endIndex - startIndex)
							.create(),
					BEditor::combineCollections
			);
		}
		return highlightingWithErrors;
	}

	private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
		this.setStyleSpans(0, addErrorHighlighting(highlighting));
	}

	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
		final String text = this.getText();
		if (executor == null) {
			// No executor - run and return a dummy task that does no highlighting
			final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
				@Override
				protected StyleSpans<Collection<String>> call() {
					return StyleSpans.singleton(Collections.emptySet(), text.length());
				}
			};
			task.run();
			return task;
		} else {
			// Executor exists - do proper highlighting
			final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
				@Override
				protected StyleSpans<Collection<String>> call() {
					return computeHighlighting(text, currentProject.getCurrentMachine());
				}
			};
			executor.execute(task);
			return task;
		}
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text, Machine machine) {
		if (machine == null) {
			//Prompt text is a comment text
			return StyleSpans.singleton(Collections.singleton("editor_comment"), text.length());
		}
		Class<? extends ModelFactory<?>> modelFactoryClass = machine.getModelFactoryClass();
		if (modelFactoryClass == ClassicalBFactory.class || modelFactoryClass == EventBFactory.class) {
			return BLexerSyntaxHighlighting.computeBHighlighting(text);
		} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
			return RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text);
		} else {
			// Do not highlight unknown languages.
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	public void clearHistory() {
		this.getUndoManager().forgetHistory();
	}

	public ObservableList<ErrorItem.Location> getErrorLocations() {
		return this.errorLocations;
	}
}
