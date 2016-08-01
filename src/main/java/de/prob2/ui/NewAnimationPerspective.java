package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

@Singleton
public class NewAnimationPerspective extends SplitPane{
    @FXML
    private OperationsView operations;
    @FXML
    private TitledPane operationsTP;
    @FXML
    private HistoryView history;
    @FXML
    private DottyView dotty;
    @FXML
    private ModelcheckingController modelcheck;
    @FXML
    private Accordion accordion;
    private Parent root;

    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent previousParent, Parent nextParent)-> {
                root = nextParent;
                onDrag();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        @FXML
    public void initialize() {}

    private void onDrag() {
        operations.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(operations)){
                this.getChildren().add(operations);
                accordion.getPanes().remove(operationsTP);
            }
            System.out.println("operations dragged");
        });
        history.setOnDragDetected((MouseEvent t) -> {
            System.out.println("history dragged");
        });
        modelcheck.setOnDragDetected((MouseEvent t) -> {
            System.out.println("modelcheck dragged");
        });
        dotty.setOnDragDetected((MouseEvent t) -> {
            System.out.println("dotty dragged");
        });
    }
}
