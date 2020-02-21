package de.prob2.ui.visb.visbobjects;

import java.util.ArrayList;

/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * The VisBEvent is designed for the JSON / VisB file
 * */
public class VisBEvent {
    private String id;
    private String event;
    private ArrayList<String> predicates;

    /**
     * These two parameters will be mapped to the id of the corresponding svg element
     * @param id the id of the svg element, that should be clickable
     * @param event the event has to be an executable operation of the corresponding B machine
     * @param predicates the predicates have to be the predicates, which are used for the event above
     */
    public VisBEvent(String id, String event, ArrayList<String> predicates){
        this.id = id;
        this.event = event;
        this.predicates = predicates;
    }

    public String getEvent() {
        return event;
    }

    public ArrayList<String> getPredicates() {
        return predicates;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString(){
        return "ID: "+id+"\nEVENT: "+event+"\nPREDICATES: "+predicates+"\n";
    }
}
