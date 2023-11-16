package de.prob2.ui.verifications.temporal.ltl.patterns.builtins;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.WrappedTextTableCell;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;


@Singleton
public final class LTLBuiltinsStage extends Stage {

	@FXML
	private TableView<LTLBuiltinsItem> tvPatterns;

	@FXML
	private TableColumn<LTLBuiltinsItem, String> pattern;

	@FXML
	private TableColumn<LTLBuiltinsItem, String> description;

	private final PatternManager patternManager;

	@Inject
	private LTLBuiltinsStage(final StageManager stageManager, final PatternManager patternManager) {
		this.patternManager = patternManager;
		stageManager.loadFXML(this, "ltl_builtins_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		pattern.setCellValueFactory(new PropertyValueFactory<>("pattern"));
		description.setCellFactory(WrappedTextTableCell::new);
		description.setCellValueFactory(new PropertyValueFactory<>("description"));
		tvPatterns.setItems(FXCollections.observableList(patternManager.getBuiltins().stream()
			                                                 .map(pattern -> new LTLBuiltinsItem(String.join("\n", pattern.getSignatures()), pattern.getDescription()))
			                                                 .collect(Collectors.toList())));
	}
}
