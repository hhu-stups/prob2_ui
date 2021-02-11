package de.prob2.ui.simulation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.table.SimulationItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulationCheckingConfiguration {

    public static class SimulationCheckingInformation {

        public static final JsonDeserializer<SimulationCheckingInformation> JSON_DESERIALIZER = SimulationCheckingInformation::new;

        private final Map<String, Object> information;

        public SimulationCheckingInformation(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
            final JsonObject object = json.getAsJsonObject();
            information = new HashMap<>();
            if(object.has("PROBABILITY")) {
                information.put("PROBABILITY", JsonManager.checkDeserialize(context, object, "PROBABILITY", double.class));
            }
            if(object.has("EXECUTIONS")) {
                information.put("EXECUTIONS", JsonManager.checkDeserialize(context, object, "EXECUTIONS", int.class));
            }
            if(object.has("PREDICATE")) {
                information.put("PREDICATE", JsonManager.checkDeserialize(context, object, "PREDICATE", String.class));
            }
            if(object.has("STEPS_PER_EXECUTION")) {
                information.put("STEPS_PER_EXECUTION", JsonManager.checkDeserialize(context, object, "STEPS_PER_EXECUTION", int.class));
            }
            if(object.has("ENDING_PREDICATE")) {
                information.put("ENDING_PREDICATE", JsonManager.checkDeserialize(context, object, "ENDING_PREDICATE", String.class));
            }
            if(object.has("ENDING_TIME")) {
                information.put("ENDING_TIME", JsonManager.checkDeserialize(context, object, "ENDING_TIME", int.class));
            }
            if(object.has("HYPOTHESIS_CHECKING_TYPE")) {
                information.put("HYPOTHESIS_CHECKING_TYPE", JsonManager.checkDeserialize(context, object, "HYPOTHESIS_CHECKING_TYPE", SimulationHypothesisChecker.HypothesisCheckingType.class));
            }
            if(object.has("ESTIMATION_TYPE")) {
                information.put("ESTIMATION_TYPE", JsonManager.checkDeserialize(context, object, "ESTIMATION_TYPE", SimulationEstimator.EstimationType.class));
            }
            if(object.has("CHECKING_TYPE")) {
                information.put("CHECKING_TYPE", JsonManager.checkDeserialize(context, object, "CHECKING_TYPE", SimulationCheckingType.class));
            }
            if(object.has("DESIRED_VALUE")) {
                information.put("DESIRED_VALUE", JsonManager.checkDeserialize(context, object, "DESIRED_VALUE", double.class));
            }
            if(object.has("FAULT_TOLERANCE")) {
                information.put("FAULT_TOLERANCE", JsonManager.checkDeserialize(context, object, "FAULT_TOLERANCE", double.class));
            }
        }

        public Map<String, Object> getInformation() {
            return information;
        }
    }

    public static final JsonDeserializer<SimulationCheckingConfiguration> JSON_DESERIALIZER = SimulationCheckingConfiguration::new;

    private final SimulationType type;

    private final Map<String, Object> information;

    public SimulationCheckingConfiguration(SimulationType type, Map<String, Object> information) {
        this.type = type;
        this.information = information;
    }

    private SimulationCheckingConfiguration(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        final JsonObject object = json.getAsJsonObject();
        this.type = JsonManager.checkDeserialize(context, object, "type", SimulationType.class);
        this.information = JsonManager.checkDeserialize(context, object, "information", SimulationCheckingInformation.class).getInformation();
    }

    public SimulationType getType() {
        return type;
    }

    public Map<String, Object> getInformation() {
        return information;
    }

    public boolean containsField(String key) {
        return information.containsKey(key);
    }

    public Object getField(String key) {
        return information.get(key);
    }

    public String getConfiguration() {
        List<String> configurations = new ArrayList<>();
        System.out.println(information);
        System.out.println(information.values().stream().map(i -> i.getClass()).collect(Collectors.toList()));
        System.out.println("------------");
        for(String key : information.keySet()) {
            Object obj = information.get(key);
            if(obj instanceof ReplayTrace) {
                configurations.add(key + " : " + ((ReplayTrace) obj).getName());
            } else {
                configurations.add(key + " : " + obj.toString());
            }
        }
        if(!configurations.isEmpty()) {
            return String.join(", ", configurations);
        }
        return type.name();
    }
}
