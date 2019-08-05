package de.prob2.ui.animation.symbolic.testcasegeneration;


import com.google.inject.Inject;

import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


@FXMLInjected
public class MCDCInputView extends VBox {

    @FXML
    private TextField levelField;

    @FXML
    private TextField depthField;

    @Inject
    private MCDCInputView(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "test_case_generation_mcdc.fxml");
    }

    public String getLevel() {
        return levelField.getText();
    }

    public String getDepth() {
        return depthField.getText();
    }

    public void reset() {
        levelField.clear();
        depthField.clear();
    }

    public void setItem(SymbolicAnimationFormulaItem item) {
        //An element in the values set of additionalInformation can be from any type. GSON casts an integer to double when saving the project file.
        levelField.setText(String.valueOf((int) Double.parseDouble(item.getAdditionalInformation("level").toString())));
        depthField.setText(String.valueOf((int) Double.parseDouble(item.getAdditionalInformation("maxDepth").toString())));
    }

}
