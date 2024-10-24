package de.prob2.ui.sharedviews;

import java.util.Set;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.IFormulaTask;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEditFormulaStage<T extends IFormulaTask> extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEditFormulaStage.class);

	@FXML
	private TextField idField;
	@FXML
	private ExtendedCodeArea formulaTextArea;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	private final I18n i18n;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;

	private String existingId;
	private T result;

	@Inject
	public AbstractEditFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "/de/prob2/ui/sharedviews/edit_formula_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.formulaTextArea.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					this.okButton.fire();
					e.consume();
				} else {
					this.formulaTextArea.insertText(this.formulaTextArea.getCaretPosition(), "\n");
				}
			}
		});
		this.formulaTextArea.textProperty().addListener((observable, oldValue, newValue) -> this.checkFormula());

		BooleanBinding hasBlankFormula = Bindings.createBooleanBinding(() -> this.formulaTextArea.getText().isBlank(), this.formulaTextArea.textProperty());
		BooleanProperty hasDuplicateId = new SimpleBooleanProperty(false);
		this.idField.textProperty().addListener((observable, from, to) -> {
			String id = to.trim();
			if (!id.isEmpty() && !id.equals(this.existingId)) {
				Machine machine = this.currentProject.getCurrentMachine();
				if (machine != null) {
					Set<String> ids = machine.getValidationTaskIds();
					hasDuplicateId.set(ids.contains(id));
					return;
				}
			}
			hasDuplicateId.set(false);
		});
		this.errorExplanationLabel.textProperty().bind(
				Bindings.when(hasDuplicateId)
						.then(this.i18n.translateBinding("common.editFormula.idAlreadyExistsError", this.idField.textProperty().map(String::trim)))
						.otherwise("")
		);
		this.okButton.disableProperty().bind(hasBlankFormula.or(hasDuplicateId));
		this.okButton.setOnAction(e -> {
			this.setResult();
			this.close();
		});
		this.cancelButton.setOnAction(e -> this.close());
	}

	public void reset() {
		this.result = null;
		this.existingId = null;
		this.idField.clear();
		this.formulaTextArea.clear();
	}

	public void setInitialFormulaTask(T item) {
		this.result = null;
		String id = item.getId();
		if (id != null && id.isBlank()) {
			id = null;
		}
		this.existingId = id;
		this.idField.setText(id != null ? item.getId() : "");
		this.formulaTextArea.replaceText(item.getFormula());
	}

	private void setResult() {
		String id = this.idField.getText().isBlank() ? null : this.idField.getText();
		String formula = this.formulaTextArea.getText().trim();
		this.result = this.createFormulaTask(id, formula);
	}

	protected abstract T createFormulaTask(String id, String formula);

	public T getResult() {
		return this.result;
	}

	private void checkFormula() {
		try {
			this.currentTrace.getModel().parseFormula(this.formulaTextArea.getText());
		} catch (RuntimeException e) {
			this.handleError(e);
			LOGGER.debug("Could not parse user-entered formula", e);
		}
	}

	private void handleError(Throwable e) {
		if (e instanceof ProBError pe) {
			this.formulaTextArea.getErrors().setAll(pe.getErrors());
		} else if (e instanceof BCompoundException bce) {
			this.handleError(new ProBError(bce));
		} else if (e instanceof BException be) {
			this.handleError(new BCompoundException(be));
		} else if (e.getCause() != null) {
			this.handleError(e.getCause());
		}
	}
}
