package de.prob2.ui;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

@Singleton
public class NewAnimationPerspective extends BorderPane {
    // TODO improve DragDrop/Docking
    // TODO drag image
    // TODO? revert to SplitPane
    @FXML
    private OperationsView operations;
    @FXML
    private TitledPane operationsTP;
    @FXML
    private HistoryView history;
    @FXML
    private TitledPane historyTP;
    @FXML
    private ModelcheckingController modelcheck;
    @FXML
    private TitledPane modelcheckTP;
    @FXML
    private AnimationsView animations;
    @FXML
    private TitledPane animationsTP;
    @FXML
    private Accordion accordion;

    private boolean dragged;
    private ImageView snapshot = new ImageView();
    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            onDrag();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@FXML
	public void initialize() {
        double initialSize = 250;
        operations.setPrefSize(initialSize,initialSize);
        history.setPrefSize(initialSize,initialSize);
        modelcheck.setPrefSize(initialSize,initialSize);
        animations.setPrefSize(initialSize,initialSize);
	}

	private void onDrag() {
        registerDrag(operations,operationsTP);
        registerDrag(history,historyTP);
        registerDrag(modelcheck,modelcheckTP);
        registerDrag(animations,animationsTP);
	}

    private void registerDrag(final Node node, final TitledPane nodeTP){
        node.setOnMouseEntered(s -> this.setCursor(Cursor.OPEN_HAND));
        node.setOnMousePressed(s -> this.setCursor(Cursor.CLOSED_HAND));
        node.setOnMouseDragEntered(s -> {
            dragged = true;
            s.consume();
        });
        node.setOnMouseReleased(s -> {
            this.setCursor(Cursor.DEFAULT);
            if (dragged){
                dragDropped(node,nodeTP);
            }
            dragged = false;
            s.consume();
        });
        node.setOnDragDetected(s -> {
            node.startFullDrag();
            s.consume();
        });
    }

    private void dragDropped(final Node node, final TitledPane nodeTP){
        //System.out.println(node.getClass().toString() + " dragged, isResizable() = "+node.isResizable());
        if (accordion.getPanes().contains(nodeTP)) {
            if (this.getRight() == null) {
                this.setRight(node);
            } else if (this.getTop() == null) {
                this.setTop(node);
            } else if (this.getBottom() == null) {
                this.setBottom(node);
            } else if (this.getLeft() == null) {
                this.setLeft(node);
            }
            nodeTP.setContent(null);
            accordion.getPanes().remove(nodeTP);
            accordion.setExpandedPane(accordion.getPanes().get(0));
        } else {
            accordion.getPanes().add(nodeTP);
            nodeTP.setContent(node);
            accordion.setExpandedPane(nodeTP);
            this.getChildren().remove(node);
        }
    }
}
