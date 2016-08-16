package de.prob2.ui;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang.ObjectUtils;

@Singleton
public class NewAnimationPerspective extends BorderPane {
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
    private VBox vBox;
    /*@FXML
    private FlowPane test;
    @FXML
    private TitledPane testTP;*/
    private boolean invisibleItems;
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

/*public class NewAnimationPerspective extends SplitPane {
	@FXML
	private OperationsView operations;
	@FXML
	private TitledPane operationsTP;
	@FXML
	private HistoryView history;
	@FXML
	private TitledPane historyTP;
	@FXML
	private AnimationsView animations;
	@FXML
	private TitledPane animationsTP;
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
			parentProperty().addListener((ObservableValue<? extends Parent>
			ov, Parent previousParent, Parent nextParent)-> { onDrag(); });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/

	@FXML
	public void initialize() {
		//this.setDividerPositions(0.3);
	}

	private void onDrag() {
        //this.dragAction(operations, operationsTP);
        //this.dragAction(history, historyTP);
        //this.dragAction(modelcheck, modelcheckTP);
        this.setOnMouseEntered(t -> this.setCursor(Cursor.OPEN_HAND));
        this.setOnMousePressed(t -> this.setCursor(Cursor.CLOSED_HAND));
        this.setOnMouseReleased(t -> this.setCursor(Cursor.DEFAULT));
        this.setOnDragDetected(t -> {
            operations.setOnDragDetected(s -> {
                addGesture(operations);
            });
            history.setOnDragDetected(s -> {
                addGesture(history);
            });
            modelcheck.setOnDragDetected(s -> {
                addGesture(modelcheck);
            });

            System.out.println("dragged");
        });
	}

    /*private void dragAction(Node node, TitledPane nodeTP){

    }*/

    private void addGesture(final Node node){
        System.out.println(node.getClass().toString()+" dragged");
        if (vBox.getChildren().contains(node)){
            if (this.getRight() == null) {
                this.setRight(node);
                vBox.getChildren().remove(node);
            } else if (this.getLeft() == null){
                this.setLeft(node);
                vBox.getChildren().remove(node);
            } else if (this.getBottom() == null){
                this.setBottom(node);
                vBox.getChildren().remove(node);
            }
            //accordion.getPanes().remove(nodeTP);
            //nodeTP.setVisible(false);
            //this.removeAccordion();
        } else {
            //this.addAccordion();
            //nodeTP.setContent(node);
            vBox.getChildren().add(node);
            this.getChildren().remove(node);
            //accordion.getPanes().add(0,nodeTP);
            //nodeTP.setVisible(true);
        }
        /*this.setOnMouseDragOver((MouseEvent) -> {
            if (!this.getItems().contains(node)) {
                this.getItems().add(node);
                //accordion.getPanes().remove(nodeTP);
                nodeTP.setVisible(false);

            }
        });*/
    }
    /*private void removeAccordion(){
        invisibleItems = true;
        for (TitledPane pane : accordion.getPanes()){
            if (pane.isVisible()){
                invisibleItems=false;
            }
        }
        if (invisibleItems){
            //this.getItems().remove(0);
            accordion.setVisible(false);
            //this.getDividers().remove(0);
        }
    }

    private void addAccordion(){
        if (invisibleItems){
            //this.getItems().add(0,accordion);
            accordion.setVisible(true);
            this.getDividers().add(0,new Divider());
        }
    }*/
/*private void dragAction(Node node, TitledPane nodeTP) {
		node.setOnDragDetected((MouseEvent t) -> {
			if (!this.getChildren().contains(node)) {
				this.getItems().add(node);
				accordion.getPanes().remove(nodeTP);
				this.removeAccordion();
			} else {
				this.addAccordion();
				nodeTP.setContent(node);
				accordion.getPanes().add(0, nodeTP);
			}
			t.consume();
		});
	}

	private void removeAccordion() {
		if (accordion.getPanes().size() == 0) {
			this.getItems().remove(0);
		}
	}

	private void addAccordion() {
		if (!this.getChildren().contains(accordion)) {
			this.getItems().add(0, accordion);
		}
	}*/
}
