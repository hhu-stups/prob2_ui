package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;

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
    private TitledPane historyTP;
    @FXML
    private DottyView dotty;
    @FXML
    private TitledPane dottyTP;
    @FXML
    private ModelcheckingController modelcheck;
    @FXML
    private TitledPane modelcheckTP;
    @FXML
    private FlowPane test;
    @FXML
    private TitledPane testTP;
    @FXML
    private Accordion accordion;

    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent previousParent, Parent nextParent)-> {
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
                this.getItems().add(operations);
                accordion.getPanes().remove(operationsTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                operationsTP.setContent(operations);
                accordion.getPanes().add(0,operationsTP);
            }
            t.consume();
        });
        history.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(history)){
                this.getItems().add(history);
                accordion.getPanes().remove(historyTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                historyTP.setContent(history);
                accordion.getPanes().add(1,historyTP);
            }
            t.consume();
        });
        dotty.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(dotty)){
                this.getItems().add(dotty);
                accordion.getPanes().remove(dottyTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                dottyTP.setContent(dotty);
                accordion.getPanes().add(2,dottyTP);
            }
            t.consume();
        });
        modelcheck.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(modelcheck)){
                this.getItems().add(modelcheck);
                accordion.getPanes().remove(modelcheckTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                modelcheckTP.setContent(modelcheck);
                accordion.getPanes().add(3,modelcheckTP);
            }
            t.consume();
        });
        test.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(test)){
                this.getItems().add(test);
                accordion.getPanes().remove(testTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                testTP.setContent(test);
                accordion.getPanes().add(4,testTP);
            }
            t.consume();
        });
    }

    private void removeAccordion(){
        if (accordion.getPanes().size()==0){
            this.getItems().remove(0);
        }
    }

    private void addAccordion(){
        if (!this.getChildren().contains(accordion)){
            this.getItems().add(0,accordion);
        }
    }
}
