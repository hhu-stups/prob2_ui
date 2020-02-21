package de.prob2.ui.visb.visbobjects;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 * */
public class VisBVisualisation {
    private ArrayList<VisBItem> visBItems;
    private ArrayList<VisBEvent> visBEvents;
    private Path svgPath;

    public VisBVisualisation(){
        this.visBItems = null;
        this.visBEvents = null;
        this.svgPath = null;
    }

    public VisBVisualisation(ArrayList<VisBItem> visBItems, ArrayList<VisBEvent> visBEvents, Path svgPath) {
        this.visBItems = visBItems;
        this.visBEvents = visBEvents;
        this.svgPath = svgPath;
    }

    public ArrayList<VisBEvent> getVisBEvents() {
        return visBEvents;
    }

    public ArrayList<VisBItem> getVisBItems() {
        return visBItems;
    }

    public Path getSvgPath() {
        return svgPath;
    }

    public VisBEvent getEventForID(String id){
        if(visBEvents != null && !visBEvents.isEmpty()){
            for(VisBEvent visBEvent : visBEvents){
                if(id.equals(visBEvent.getId())){
                    return visBEvent;
                }
            }
        }
        return null;
    }

    public boolean isReady(){
        return ((visBEvents != null && !visBEvents.isEmpty()) || (visBItems != null && !visBItems.isEmpty())) && svgPath != null;
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Visualisation Items List:\n");
        for (VisBItem visBItem : visBItems){
            stringBuilder.append(visBItem.toString());
            stringBuilder.append("\n");
        }
        stringBuilder.append("Visualisation Events List:\n");
        for (VisBEvent visBEvent : visBEvents){
            stringBuilder.append(visBEvent.toString());
            stringBuilder.append("\n");
        }
        stringBuilder.append("SVG: \n");
        stringBuilder.append(svgPath.toString());
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
