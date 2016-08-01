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
import javafx.scene.Node;
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
            /*parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent previousParent, Parent nextParent)-> {
                onDrag();
            });*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        this.setDividerPositions(0.3);
    }

    private void onDrag() {
        this.dragAction(operations,operationsTP);
        this.dragAction(history,historyTP);
        this.dragAction(dotty,dottyTP);
        this.dragAction(modelcheck,modelcheckTP);
    }

    private void dragAction(Node node, TitledPane nodeTP){
        node.setOnDragDetected((MouseEvent t) -> {
            if (!this.getChildren().contains(node)){
                this.getItems().add(node);
                accordion.getPanes().remove(nodeTP);
                this.removeAccordion();
            } else {
                this.addAccordion();
                nodeTP.setContent(node);
                accordion.getPanes().add(0,nodeTP);
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
