package de.prob2.ui.visualisation.traceDifference;

import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.refinement.TraceConnector;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.*;
import java.util.stream.Stream;

public class TracePlotter {

    final double SPACE_TO_EDGE = 5; // Spaces from the edge of a box to its content
    final double SPACE_TO_BOTH_EDGES = SPACE_TO_EDGE * 2; // Spaces from the edge of a box to its content
    final int BOX_CONSTANT = 50; //Space between two boxes
    final double STARTING_X = 50;
    final double STARTING_Y_FIRST_TRACE = 50;
    final double STARTING_Y_SECOND_TRACE = 150;

    public void drawTraces(List<TraceConnector.Pair<PersistentTransition, PersistentTransition>> trace, GraphicsContext gc){

         drawText(Color.RED, gc, 10, STARTING_Y_FIRST_TRACE-5, "Abstract Trace");
        drawText(Color.RED, gc,10, STARTING_Y_SECOND_TRACE-5, "Concrete Trace");

        double offSetXT1 = STARTING_X;
        double offSetXT2 = STARTING_X;

        double name_height_max = height(trace);

        double half_height = name_height_max/2;


        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);


        for(TraceConnector.Pair<PersistentTransition, PersistentTransition> entry : trace){


            double iterationWidth = width(Collections.singletonList(entry));
            double halfIterationWidth = iterationWidth/2;



            extendFrame( offSetXT1,  offSetXT2,  iterationWidth,  gc);

            writeTransition(Color.BLUE, gc, offSetXT1,  STARTING_Y_FIRST_TRACE, name_height_max, iterationWidth +  SPACE_TO_BOTH_EDGES, entry.first);
            writeTransition(Color.BLUE, gc, offSetXT2,  STARTING_Y_SECOND_TRACE,  name_height_max, iterationWidth +  SPACE_TO_BOTH_EDGES, entry.second);
            double pos_y_start = STARTING_Y_FIRST_TRACE + (name_height_max*entry.getFirst().getDestinationStateVariables().size()) + 10 + SPACE_TO_EDGE * 2;
            drawStroke(Color.BLACK, gc, offSetXT1+SPACE_TO_EDGE+ halfIterationWidth, pos_y_start, offSetXT2+SPACE_TO_EDGE+halfIterationWidth, STARTING_Y_SECOND_TRACE);
            drawRect(Color.BLACK, gc, offSetXT1+SPACE_TO_EDGE+ halfIterationWidth-2.5, pos_y_start,  + 5,  5);
            drawRect(Color.BLACK, gc, offSetXT2+SPACE_TO_EDGE+halfIterationWidth - 2.5, STARTING_Y_SECOND_TRACE -5,  + 5,  5);


            if(trace.lastIndexOf(entry) != trace.size() -1 ){
                drawStroke(Color.RED, gc, offSetXT1 + iterationWidth + SPACE_TO_BOTH_EDGES *2 , STARTING_Y_FIRST_TRACE +half_height + SPACE_TO_EDGE ,  offSetXT1 + iterationWidth+ BOX_CONSTANT,  STARTING_Y_FIRST_TRACE +half_height + SPACE_TO_EDGE);
                drawStroke(Color.RED, gc, offSetXT2 + iterationWidth + SPACE_TO_BOTH_EDGES *2,  STARTING_Y_SECOND_TRACE +half_height + SPACE_TO_EDGE ,  offSetXT2 + iterationWidth + BOX_CONSTANT, STARTING_Y_SECOND_TRACE +half_height +SPACE_TO_EDGE );
                offSetXT1 = offSetXT1 + iterationWidth + BOX_CONSTANT;
                offSetXT2 = offSetXT2 + iterationWidth + BOX_CONSTANT;
            }



        }
        

    }


    public double height(List<TraceConnector.Pair<PersistentTransition, PersistentTransition>> trace){

        return stringSize(trace, this::stringHeight);

    }

    public double width(List<TraceConnector.Pair<PersistentTransition, PersistentTransition>> trace){

        return stringSize(trace, this::stringWidth);

    }

    public double stringSize(List<TraceConnector.Pair<PersistentTransition, PersistentTransition>> trace, Measure measure){
        Optional<Double> first = Stream.concat(trace.stream().map(entry -> entry.getFirst().getOperationName()),
                        trace.stream().map(entry -> entry.getFirst().getDestinationStateVariables().entrySet()).flatMap(entry -> entry.stream().map(Object::toString)))
                .map(measure::measure)
                .max(Comparator.naturalOrder());

        Optional<Double> second = Stream.concat(trace.stream().map(entry -> entry.getSecond().getOperationName()),
                        trace.stream().map(entry -> entry.getSecond().getDestinationStateVariables().entrySet()).flatMap(entry -> entry.stream().map(Object::toString)))
                .map(measure::measure)
                .max(Comparator.naturalOrder());



        return Math.max(first.get(), second.get());
    }


    interface Measure{
        double measure(String first);
    }



    private void extendFrame(double offSetXT1, double offSetXT2, double name_width_max, GraphicsContext gc){
        if(gc.getCanvas().getWidth() + name_width_max + SPACE_TO_BOTH_EDGES + BOX_CONSTANT >= gc.getCanvas().getWidth()){
            gc.getCanvas().resize(gc.getCanvas().getWidth() + name_width_max + SPACE_TO_BOTH_EDGES + BOX_CONSTANT, gc.getCanvas().getHeight());
        }
    }

    private void writeTransition(Paint color, GraphicsContext gc, double x1, double y1, double name_height, double name_width, PersistentTransition toDraw){
        drawRect(color, gc, x1, y1, name_width + SPACE_TO_EDGE * 2, (name_height*toDraw.getDestinationStateVariables().size()) + 10 + SPACE_TO_EDGE * 2);

        List<Map.Entry<String, String>> listEntries = new ArrayList<>(toDraw.getDestinationStateVariables().entrySet());


        for(int i = 0 ; i < toDraw.getDestinationStateVariables().size()+1; i++){



            if(i == 0){
                String text = toDraw.getOperationName();
                drawText(Color.BLACK, gc ,x1 + SPACE_TO_EDGE, y1 + stringHeight(text) + (stringHeight(text) * i), text);


            }else{
                String text = listEntries.get(i-1).getKey() + ":= " + listEntries.get(i-1).getValue();

                 text = cutToWidth(text, name_width);
                 drawText(Color.GREEN, gc ,x1 + SPACE_TO_EDGE, y1 + stringHeight(text) + (stringHeight(text) * i), text);


            }

        }
        String text = toDraw.getOperationName();
        if(!listEntries.isEmpty()){
            drawStroke(Color.BLACK, gc, x1 , y1 + stringHeight(text) + 2.5 ,  x1 + name_width + SPACE_TO_EDGE * 2, y1 +  stringHeight(text) + 2.5);
        }


    }

    private String cutToWidth(String text, double allowed){
        double width = stringWidth(text);
        String modified = text;
        while(width > allowed){
            modified = modified.substring(0, modified.length()-2);
            width = stringWidth(modified);
        }
        return modified;
    }

    private void drawRect(Paint color, GraphicsContext gc, double x1, double y1, double width, double height){
        Paint colorOld = gc.getStroke();
        gc.setStroke(color);
        gc.strokeRect(x1, y1, width, height);
        gc.setStroke(colorOld);
    }


    private void drawStroke(Paint color, GraphicsContext gc, double x1, double y1, double x2, double y2){
        Paint colorOld = gc.getStroke();
        gc.setStroke(color);
        gc.strokeLine(x1, y1, x2, y2);
        gc.setStroke(colorOld);
    }

    private void drawText(Paint color, GraphicsContext gc, double x1, double y1, String text){
        Paint colorOld = gc.getFill();
        gc.setFill(color);
        gc.fillText(text, x1, y1);
        gc.setFill(colorOld);


    }

    private double stringWidth(String toMeasure){
       return new Text(toMeasure).getBoundsInLocal().getWidth();
    }

    private double stringHeight(String toMeasure){
        return new Text(toMeasure).getBoundsInLocal().getHeight();
    }

    public static class ResizableCanvas extends Canvas {

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double maxHeight(double width) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double maxWidth(double height) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double minWidth(double height) {
            return 1D;
        }

        @Override
        public double minHeight(double width) {
            return 1D;
        }

        @Override
        public void resize(double width, double height) {
            this.setWidth(width);
            this.setHeight(height);
        }
    }



}
