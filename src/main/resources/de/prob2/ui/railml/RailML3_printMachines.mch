MACHINE RailML3_printMachines
INCLUDES RailML_import
DEFINITIONS
    "LibraryIO.def";
    "LibraryMeta.def";
    "LibraryStrings.def";
    "SORT.def"
CONSTANTS
    outputBaseFile, outputAnimationFile, outputValidationFile, svgFile, animationMachineName, validationMachineName
PROPERTIES
    {file, outputBaseFile, outputAnimationFile, outputValidationFile, svgFile, animationMachineName, validationMachineName} <: STRING
VARIABLES
    printAnimation, printValidation
INVARIANT
    printAnimation : BOOL & printValidation : BOOL
INITIALISATION
    importRailML; printAnimation := FALSE; printValidation := FALSE
OPERATIONS
    triggerPrintAnimation =
        PRE printAnimation = FALSE THEN
            IF "FALSE" /: ran(FORMULA_VALUES("inv")) & no_error = TRUE THEN
                printAnimation := TRUE                
            ELSIF "FALSE" : ran(FORMULA_VALUES("inv")) THEN
                ADD_ERROR("RailML: ", "Import failed because of an invariant violation - check details in state view")            
            ELSE ADD_ERROR("RailML: ", "Import failed") END
        END;
    triggerPrintValidation =
        PRE printValidation = FALSE THEN
            IF "FALSE" /: ran(FORMULA_VALUES("inv")) & no_error = TRUE THEN
                printValidation := TRUE                
            ELSIF "FALSE" : ran(FORMULA_VALUES("inv")) THEN
                ADD_ERROR("RailML: ", "Import failed because of an invariant violation - check details in state view")            
            ELSE ADD_ERROR("RailML: ", "Import failed") END
        END;

    printAnimationMachine =
    PRE printAnimation = TRUE THEN
    FPRINTF(outputAnimationFile, "~w~n", ['''/*@generated */''']);
    FPRINTF(outputAnimationFile, "~w~w~n", ["MACHINE ", animationMachineName]);

    FPRINTF(outputAnimationFile, "~w~n", ['''ABSTRACT_CONSTANTS
    allIdsOfType''']);
    FPRINTF(outputAnimationFile, "~w~n", ['''CONCRETE_CONSTANTS
    all_ids, RS_trainFront_init, RS_trainBack_init, RS_train_states_init,''']);
    VAR variable_names, filtered_variable_names IN
        variable_names := FORMULA_INFOS("variables");
        filtered_variable_names := SQUASH({ i, name | i : dom(variable_names) & name = variable_names(i) & SUB_STRING(name, 1, 11) : {"RailML3_IS_", "RailML3_IL_", "RailML3_VIS", "RailML3_CO_"} });
        FPRINTF(outputAnimationFile, "    ~w~n", [STRING_JOIN(filtered_variable_names, ",\n    ")])
    END;

    FPRINTF(outputAnimationFile, "~w~n", ['''PROPERTIES
    allIdsOfType = %type.( type : STRING | all_ids~[{type} * ran(ran(all_ids))] )
    & RS_trainFront_init : RS_trains +-> (STRING * REAL * RailML3_IS_DIRECTION)
    & RS_trainBack_init : RS_trains +-> (STRING * REAL * RailML3_IS_DIRECTION)
    & RS_train_states_init : RS_trains +-> RS_TRAIN_STATES
    & RS_trainFront_init = {}
    & RS_trainBack_init = {}
    & RS_train_states_init = {}''']); // TODO: set individually
    FPRINTF(outputAnimationFile, "    & all_ids = ~w~n", [all_ids]);
    FPRINTF(outputAnimationFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML3_IS"), " &"), "\n    &")]); // prevent very long lines
    FPRINTF(outputAnimationFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML3_IL"), " &"), "\n    &")]);
    FPRINTF(outputAnimationFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML3_VIS"), " &"), "\n    &")]);
    FPRINTF(outputAnimationFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML3_CO"), " &"), "\n    &")]);
    // BEGIN ROUTES
    // r : dom(...) is necessary since ROUTE_IDS is initialised before "...".
    /*& !(r).(r : allIdsOfType("route") & r : dom(RailML3_IL_ROUTE_NXT) => RailML3_IL_ROUTE_NXT(r) /= {})

    // From prob_examples/public_examples/EventBPrologPackages/Abrial_Train_Ch17/Train_1_beebook_v2.mch :    
    & {r,b|(r,b) : RailML3_IL_ROUTES_FST}~ <: RailML3_IL_ROUTE_RTBL
    & {r,b|(r,b) : RailML3_IL_ROUTES_LST}~ <: RailML3_IL_ROUTE_RTBL
    & !(r).(r : allIdsOfType("route") & r : dom(RailML3_IL_ROUTES_FST) & r : dom(RailML3_IL_ROUTES_LST) => RailML3_IL_ROUTES_FST(r) /= RailML3_IL_ROUTES_LST(r))
    /*& !(r).(r : allIdsOfType("route") & r : dom(RailML3_IL_ROUTE_NXT) & r : dom(RailML3_IL_ROUTES_FST) & r : dom(RailML3_IL_ROUTES_LST)
        => !(S).(S <: ran(RailML3_IL_ROUTE_NXT(r)) & S <: RailML3_IL_ROUTE_NXT(r)[S] => S = {} or (RailML3_IL_ROUTES_FST(r) = RailML3_IL_ROUTES_LST(r) & S = {RailML3_IL_ROUTES_FST(r)}) ))*/
    // double switch causes errors for all following:
    /*& !(r).(r : allIdsOfType("route") & r : dom(RailML3_IL_ROUTE_NXT) & r : dom(RailML3_IL_ROUTES_FST) & r : dom(RailML3_IL_ROUTES_LST) & RailML3_IL_ROUTE_RTBL /= {} => RailML3_IL_ROUTE_NXT(r) : (RailML3_IL_ROUTE_RTBL~)[{r}] \ {RailML3_IL_ROUTES_LST(r)} >->> (RailML3_IL_ROUTE_RTBL~)[{r}] \ {RailML3_IL_ROUTES_FST(r)})
    & !(r,s).((r : allIdsOfType("route") & s : allIdsOfType("route")) & r /= s & {r,s} <: dom(RailML3_IL_ROUTES_FST) & {r,s} <: dom(RailML3_IL_ROUTES_LST) & RailML3_IL_ROUTE_RTBL /= {}
        & {RailML3_IL_ROUTES_FST(s) |-> RailML3_IL_ROUTES_LST(r), RailML3_IL_ROUTES_FST(r) |-> RailML3_IL_ROUTES_LST(r)} <: RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1
        => RailML3_IL_ROUTES_FST(r) /: (RailML3_IL_ROUTE_RTBL~)[{s}] \ {RailML3_IL_ROUTES_FST(s),RailML3_IL_ROUTES_LST(s)})
    & !(r,s).((r : allIdsOfType("route") & s : allIdsOfType("route")) & r /= s & {r,s} <: dom(RailML3_IL_ROUTES_FST) & {r,s} <: dom(RailML3_IL_ROUTES_LST) & RailML3_IL_ROUTE_RTBL /= {}
        & {RailML3_IL_ROUTES_FST(s) |-> RailML3_IL_ROUTES_LST(r), RailML3_IL_ROUTES_FST(r) |-> RailML3_IL_ROUTES_LST(r)} <: RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1
        => RailML3_IL_ROUTES_LST(r) /: (RailML3_IL_ROUTE_RTBL~)[{s}] \ {RailML3_IL_ROUTES_FST(s),RailML3_IL_ROUTES_LST(s)})*/
    // END ROUTES'''});

    FPRINTF(outputAnimationFile, "~w~n", ['''DEFINITIONS
    SET_PREF_optimize_enum_set_elems == TRUE;
    "RailML_animation.def";''']);
    FPRINTF(outputAnimationFile, "    VISB_SVG_FILE == \"~w\"~n", [svgFile]);

    FPRINTF(outputAnimationFile, "~w~n", ["SETS"]);
    VAR set_names, set_values, filtered_sets IN
        set_names := FORMULA_INFOS("sets");
        set_values := FORMULA_VALUES("sets");
        filtered_sets := SQUASH({ i, name | i : dom(set_names) & name = set_names(i)^" = "^set_values(i) & SUB_STRING(set_names(i), 1, 6) = "RailML" });
        FPRINTF(outputAnimationFile, "    ~w;~n", [STRING_JOIN(filtered_sets, ";\n    ")])
    END;
    FPRINTF(outputAnimationFile, "~w~n", ['''    RS_trains;
    RS_TRAIN_STATES = {train_moving, train_standing}
VARIABLES
    IS_next,
    IS_crossing_states, IS_crossingsInMovement, IS_crossing_keyLocked,
    IS_derailer_states, IS_derailersInMovement, IS_derailer_keyLocked,
    IS_switch_states, IS_switchsInMovement, IS_switch_keyLocked,

    RS_requestingArrivalTrains, RS_arrivedTrains, RS_trainOccupiedBlocks, RS_trainFront, RS_trainBack, RS_train_states,

    IL_occupiedTvdSections, 
    IL_routes_in_res, IL_rsrtbl, IL_res_routes, IL_res_blocks, IL_released_partialRoutes,
    IL_res_overlaps, IL_overlaps_in_release,
    IL_signal_states, IL_noted_signal_states,
    IL_crossing_locked_routes, IL_derailer_locked_routes, IL_switch_locked_routes, IL_signal_locked, IL_signalplan_in_activation
INVARIANT
    IS_next : ((allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION) >+> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION))
    & IS_next <: RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS 
    & dom(dom(dom(IS_next) \/ ran(IS_next))) = allIdsOfType("netElement") /\ RailML3_IS_NET_RESOURCES_MICRO_LEVEL

    & IS_crossingsInMovement : allIdsOfType("crossing") +-> (allIdsOfType("movableCrossing") * RailML3_IL_CROSSING_POSITIONS) // Stores destination positions
    & !(cr, branch).(cr : dom(IS_crossingsInMovement) & branch : RailML3_IS_CROSSING_BRANCHES(cr) => branch /: IS_next) /*@desc "crossings in movement are not part of IS_next" */
    & IS_crossing_states : allIdsOfType("crossing") --> (allIdsOfType("movableCrossing") * RailML3_IL_CROSSING_POSITIONS)
    & !(cr).(cr : allIdsOfType("crossing") => IS_crossing_states(cr) : dom(RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)))
    & !(cr).(cr : allIdsOfType("crossing") - dom(IS_crossingsInMovement) => RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)) <: IS_next) /*@desc "actual crossing positions are part of IS_next" */
    & !(cr, branch).(cr : allIdsOfType("crossing") & cr /: dom(IS_crossingsInMovement) & branch : RailML3_IS_CROSSING_BRANCHES(cr) & branch /: (RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)))) => branch /: IS_next) /*@desc "other crossing positions are not part of IS_next" */
    & IS_crossing_keyLocked : allIdsOfType("crossing") --> BOOL

    & IS_derailersInMovement : allIdsOfType("derailerIS") +-> (allIdsOfType("derailerIL") * RailML3_IL_DERAILER_POSITIONS) // Stores destination positions
    & !(der, pos).(der : dom(IS_derailersInMovement) & pos : RailML3_IS_SPOT_LOCATIONS(der) => pos /: dom(IS_next) \/ ran(IS_next)) /*@desc "derailers in movement are not part of IS_next" */
    & IS_derailer_states : allIdsOfType("derailerIS") --> (allIdsOfType("derailerIL") * RailML3_IL_DERAILER_POSITIONS)
    //& !(i_der, p1, p2).({p1,p2} <: dom(IS_derailer_states(i_der)) & p1 = p2 => IS_derailer_states(i_der)(p1) = IS_derailer_states(i_der)(p2)) /*@desc "IL derailers have the same state" */
    & !(der, pos).(der : allIdsOfType("derailerIS") & der /: dom(IS_derailersInMovement) & prj2(IS_derailer_states(der)) = position_derailingPosition & pos : RailML3_IS_SPOT_LOCATIONS(der) => pos /: dom(IS_next) \/ ran(IS_next))
    & IS_derailer_keyLocked : allIdsOfType("derailerIS") --> BOOL

    & IS_switchsInMovement : RailML3_IS_SWITCH_IDS +-> (allIdsOfType("switchIL") +-> RailML3_IL_SWITCH_POSITIONS) // Stores destination positions
    & !(sw, branch).(sw : dom(IS_switchsInMovement) & branch : RailML3_IS_SWITCH_BRANCHES(sw) => branch /: IS_next) /*@desc "switchs in movement are not part of IS_next" */
    & IS_switch_states : RailML3_IS_SWITCH_IDS --> (allIdsOfType("switchIL") +-> RailML3_IL_SWITCH_POSITIONS)
    & !(sw).(sw : RailML3_IS_SWITCH_IDS => IS_switch_states(sw) : dom(RailML3_IL_SWITCH_BRANCHES(sw)))
    & !(i_sw, p1, p2).({p1,p2} <: dom(IS_switch_states(i_sw)) & p1 = p2 => IS_switch_states(i_sw)(p1) = IS_switch_states(i_sw)(p2)) /*@desc "IL switchs have the same state" */
    & !(sw).(sw : RailML3_IS_SWITCH_IDS - dom(IS_switchsInMovement) => RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)) <: IS_next) /*@desc "actual switch positions are part of IS_next" */
    & !(sw, branch).(sw : RailML3_IS_SWITCH_IDS & sw /: dom(IS_switchsInMovement) & branch : RailML3_IS_SWITCH_BRANCHES(sw) & branch /: (RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)))) => branch /: IS_next) /*@desc "other switch positions are not part of IS_next" */
    & IS_switch_keyLocked : RailML3_IS_SWITCH_IDS --> BOOL

    & RS_requestingArrivalTrains : RS_trains - RS_arrivedTrains +-> ((allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION) * allIdsOfType("tvdSection"))
    & RS_arrivedTrains <: RS_trains
    & RS_arrivedTrains /\ dom(RS_requestingArrivalTrains) = {}
    & RS_trainOccupiedBlocks : RS_arrivedTrains --> FIN(allIdsOfType("netElement") * REAL)
    & !(t1,t2).({t1,t2} <: RS_arrivedTrains & t1 /= t2 => RS_trainOccupiedBlocks(t1) /\ RS_trainOccupiedBlocks(t2) = {}) /*@desc "no collision of trains" */
    & RS_trainFront : RS_arrivedTrains --> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)
    & RS_trainBack : RS_arrivedTrains --> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)
    & !t.(t : RS_arrivedTrains => RS_trainBack(t) |-> RS_trainFront(t) : closure1(IS_next)) /*@desc "train not broken in two parts (which also covers that not on a element in movement), back behind front, and back /= front" */
    & RS_train_states : RS_arrivedTrains --> RS_TRAIN_STATES

    & IL_occupiedTvdSections : allIdsOfType("tvdSection") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & !tvd.(tvd : dom(IL_occupiedTvdSections) & IL_occupiedTvdSections(tvd) : RS_arrivedTrains
        => #b.(b : union(ran(RS_trainOccupiedBlocks)) * {direction_normal, direction_reverse} & b : dom(RailML3_IL_TVD_SECTIONS(tvd)) \/ ran(RailML3_IL_TVD_SECTIONS(tvd)) )) /*@desc "on each occupied tvd section is a train" */
    & IL_routes_in_res : allIdsOfType("route") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & IL_res_routes : allIdsOfType("route") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & IL_routes_in_res /\ IL_res_routes = {}
    & !(r1,r2).({r1,r2} <: dom(IL_routes_in_res \/ IL_res_routes) & r1 /= r2 => RailML3_IL_ROUTE_NXT(r1) /\ RailML3_IL_ROUTE_NXT(r2) = {}) /*@desc "all reserved routes are disjoint" */
    & IL_res_overlaps : allIdsOfType("overlap") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & !o.(o : dom(IL_res_overlaps) => #r.(r : dom(IL_routes_in_res \/ IL_res_routes) & o : union(RailML3_IL_ROUTE_OVERLAPS[{r}]))) /*@desc "for each reserved overlap exists a corresponding reserved route" */
    & IL_overlaps_in_release <: dom(IL_res_overlaps)
    
    & IL_released_partialRoutes : dom(IL_res_routes) --> FIN(allIdsOfType("routeReleaseGroupAhead") \/ allIdsOfType("routeReleaseGroupRear"))
    & UNION(r, rg).(r : dom(IL_res_routes) & rg : IL_released_partialRoutes(r) /\ allIdsOfType("routeReleaseGroupRear") | RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r) ) <: RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS

    // for route interlocking (inspired by Abrial's train system model)
    & IL_rsrtbl : IL_res_blocks <<-> dom(IL_routes_in_res \/ IL_res_routes)
    & !res_bl.(res_bl : IL_res_blocks /\ ran(ran(RailML3_IL_ROUTE_ENTRY \/ RailML3_IL_ROUTE_EXIT)) => card(IL_rsrtbl[{res_bl}]) : 0..2 ) /*@desc "route entries/exits can be reserved by at most two routes" */
    & !res_bl.(res_bl : IL_res_blocks - ran(ran(RailML3_IL_ROUTE_ENTRY \/ RailML3_IL_ROUTE_EXIT)) => card(IL_rsrtbl[{res_bl}]) : 0..1 ) /*@desc "other blocks can be reserved by at most one route" */
    & IL_rsrtbl <: RailML3_IL_ROUTE_RTBL
    & !(r).(r : allIdsOfType("route") => RailML3_IL_ROUTE_NXT(r)[(IL_rsrtbl~)[{r}]] <: (IL_rsrtbl~)[{r}])
    & !(r,rg).(r : dom(IL_res_routes) & rg : IL_released_partialRoutes(r) => ((dom(RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(r) \/ RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r)) \/ ran(RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(r) \/ RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r))) <| RailML3_IL_ROUTE_RTBL) |> {r} = IL_rsrtbl |> {r})
    & !(r).(r : dom(IL_res_routes) => (IL_rsrtbl~)[{r}] <| RailML3_IL_ROUTE_NXT(r) = (IL_rsrtbl~)[{r}] <| IS_next |> (IL_rsrtbl~)[{r}])
    // end route interlocking
    
    & IL_crossing_locked_routes : allIdsOfType("crossing") <-> allIdsOfType("route")
    & !cr.(cr : dom(IL_crossing_locked_routes) => cr /: dom(IS_crossingsInMovement)) /*@desc "locked crossings should never be in movement" */
    & IL_derailer_locked_routes  : allIdsOfType("derailerIS") <-> allIdsOfType("route")
    & !der.(der : dom(IL_derailer_locked_routes) => der /: dom(IS_derailersInMovement)) /*@desc "locked derailers should never be in movement" */
    & IL_switch_locked_routes  : RailML3_IS_SWITCH_IDS <-> allIdsOfType("route")
    & !sw.(sw : dom(IL_switch_locked_routes) => sw /: dom(IS_switchsInMovement)) /*@desc "locked switches should never be in movement" */

    & IL_signal_states : allIdsOfType("signalIS") --> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
    & !sig.(sig : dom(IL_signal_states) & aspect_closed : IL_signal_states(sig) => card(IL_signal_states(sig)) = 1) /*@desc "If aspect_closed is active, no other aspect can be shown" */
    // TODO: block after proceeding signal is free
    & IL_noted_signal_states : allIdsOfType("signalIS") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
    & !sig.(sig : dom(IL_noted_signal_states) & aspect_closed : IL_noted_signal_states(sig) => card(IL_noted_signal_states(sig)) = 1) /*@desc "If aspect_closed is noted, no other aspect can be shown" */
    // TODO: block after proceeding noted signal is free
    & IL_signal_locked : FIN(allIdsOfType("signalIS"))
    & IL_signalplan_in_activation : 
        FIN(struct(
            master: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS),
            slave: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS),
            distant: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
        ))//FIN(allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS))
    //& !sig_il.(sig_il : dom(union(IL_signalplan_in_activation)) => RailML3_IL_SIGNAL_REFERS_TO(sig_il) : dom(IL_signal_locked_by_signalplan)) /*@desc "corresponding is signal is locked by signalplan when signalplan for il signal is in activation" */

    // states that must be proven continuously: (IFs for performance reasons)
    & !(r,rr).(r : dom(IL_res_routes) /\ dom(RailML3_IL_ROUTE_ADDITIONAL_RELATIONS) & rr : RailML3_IL_ROUTE_ADDITIONAL_RELATIONS(r) =>
        (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS) =>
            (proving_continuously |-> FALSE : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)) =>
                RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)(proving_continuously,FALSE) <: IS_derailer_states
            )
            & (proving_continuously |-> TRUE : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)) =>
                RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)(proving_continuously, TRUE) /\ IS_derailer_states = {}
            )
        )
        & (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS) =>
            (proving_continuously |-> FALSE : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)) =>
                RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)(proving_continuously, FALSE) <: IS_switch_states
            )
            & (proving_continuously |-> TRUE : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)) =>
                RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)(proving_continuously, TRUE) /\ IS_switch_states = {}
            )
        )
    ) // TODO: detection / section states
        
    & !o.(o : dom(IL_res_overlaps) =>
        (o : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS) =>
            (proving_continuously |-> FALSE : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)) =>
                RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)(proving_continuously, FALSE) <: IS_switch_states
            )
            & (proving_continuously |-> TRUE : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)) =>
                RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)(proving_continuously, TRUE) /\ IS_switch_states = {}
            )
        )
    )

    // TODO: Position restrictions of switches ok
INITIALISATION
    IS_crossing_states :=
        { i_cr, cr | #i_cr_il.(i_cr_il : allIdsOfType("movableCrossing") & RailML3_IL_MOVABLE_CROSSING_REFERS_TO(i_cr_il) = i_cr & cr = i_cr_il |-> IF i_cr_il : dom(RailML3_IL_MOVABLE_CROSSING_PREFERRED_POSITION) THEN RailML3_IL_MOVABLE_CROSSING_PREFERRED_POSITION(i_cr_il) ELSE `position_upleft-rightdown` END) } ||
    IS_derailer_states :=
        { i_der, der | #i_der_il.(i_der_il : allIdsOfType("derailerIL") & RailML3_IL_DERAILER_REFERS_TO(i_der_il) = i_der & der = i_der_il |-> IF i_der_il : dom(RailML3_IL_DERAILER_PREFERRED_POSITION) THEN RailML3_IL_DERAILER_PREFERRED_POSITION(i_der_il) ELSE position_derailingPosition END) } ||
    IS_switch_states :=
        %i_sw.( i_sw : RailML3_IS_SWITCH_IDS |
            { i_sw_il, sw | i_sw_il : allIdsOfType("switchIL") & RailML3_IL_SWITCH_REFERS_TO(i_sw_il) = i_sw & sw = IF i_sw_il : dom(RailML3_IL_SWITCH_PREFERRED_POSITION) THEN RailML3_IL_SWITCH_PREFERRED_POSITION(i_sw_il) ELSE position_left END }
        );
    
    IS_next := RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS
        - ({ np | #(i_cr,currState,otherState).(IS_crossing_states(i_cr) = currState & otherState /= currState & np : RailML3_IL_MOVABLE_CROSSING_BRANCHES(i_cr)(otherState) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(i_cr)(otherState)) ) }
            \/
            dom({ np, i_der | prj2(IS_derailer_states(i_der)) = position_derailingPosition & np : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) })
            \/
            { np | #(i_sw,currState,otherState).(IS_switch_states(i_sw) = currState & otherState /= currState & np : RailML3_IL_SWITCH_BRANCHES(i_sw)(otherState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(i_sw)(otherState)) ) }
        );
    IS_crossingsInMovement, IS_derailersInMovement, IS_switchsInMovement := {}, {}, {} ||
    IS_crossing_keyLocked := RailML3_IL_MOVABLE_CROSSING_IS_KEY_LOCKED ||
    IS_derailer_keyLocked := RailML3_IL_DERAILER_IS_KEY_LOCKED ||
    IS_switch_keyLocked := RailML3_IL_SWITCH_IS_KEY_LOCKED ||

    RS_requestingArrivalTrains := {};
    RS_arrivedTrains := dom(RS_train_states_init);
    RS_trainOccupiedBlocks := fnc({ train, block | train : RS_arrivedTrains & #ssB.(ssB = ignoreDirection(RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1~[{RS_trainFront_init(train)}] <| RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS |> RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1[{RS_trainBack_init(train)}]) & block : dom(ssB) \/ ran(ssB)) }) ||
    RS_trainFront, RS_trainBack, RS_train_states := RS_trainFront_init, RS_trainBack_init, RS_train_states_init;

    IL_occupiedTvdSections := { i_tvd, train | #block.(block : RS_trainOccupiedBlocks(train) * {direction_normal, direction_reverse} & block : dom(RailML3_IL_TVD_SECTIONS(i_tvd)) \/ ran(RailML3_IL_TVD_SECTIONS(i_tvd))) } ||
    IL_routes_in_res, IL_res_routes, IL_res_blocks, IL_rsrtbl, IL_released_partialRoutes := {}, {}, {}, {}, {} ||
    IL_res_overlaps, IL_overlaps_in_release := {}, {} ||
    IL_crossing_locked_routes := {} ||
    IL_derailer_locked_routes := {} ||
    IL_switch_locked_routes := {} ||
    IL_signal_states, IL_noted_signal_states := allIdsOfType("signalIS") * {{aspect_closed}}, {} ||
    IL_signal_locked,IL_signalplan_in_activation := {}, {}
OPERATIONS
    IS_startChangeDerailer(DerailerId, DestState) =
        ANY
            signals
        WHERE
            DerailerId : dom(IS_derailer_states)
            & IS_derailer_keyLocked(DerailerId) = FALSE
            & DerailerId /: dom(IL_derailer_locked_routes)
            & IF DerailerId : dom(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT) THEN
                RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId) /: dom(IS_derailersInMovement) \/ dom(IS_crossingsInMovement) \/ dom(IS_switchsInMovement)
                & IF RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId) : RailML3_IS_SWITCH_IDS THEN
                    !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId)}] & i_sw_il |-> IS_switch_states(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId))(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                        => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> IS_switch_states(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId))(i_sw_il))'derailer(RailML3_IL_DERAILER_REFERS_TO~(DerailerId)) /= prj2(IS_derailer_states(DerailerId)) )
                ELSE btrue END
            ELSE btrue END
            & DestState : {prj1(IS_derailer_states(DerailerId))} * RailML3_IL_DERAILER_POSITIONS
            & IS_derailer_states(DerailerId) /= DestState
            & DerailerId /: dom(IS_derailersInMovement)
            & !sloc.(sloc : dom(RailML3_IS_SPOT_LOCATIONS(DerailerId)) => sloc /: union(ran(RS_trainOccupiedBlocks)))
            & !tvd.(tvd : union(RailML3_IL_DERAILER_HAS_TVD_SECTION[{DerailerId}]) => tvd /: dom(IL_occupiedTvdSections))
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & union(ran(RailML3_IS_SIGNAL_CONTROL_SECTIONS(i_sig))) /\ IS_next /\ RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId) /= {} }
        THEN
            IS_derailersInMovement := IS_derailersInMovement \/ {DerailerId |-> DestState} ||
            IF prj2(DestState) = position_derailingPosition THEN
                IS_next := IS_next - RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId) ||
                IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
                IL_noted_signal_states := signals <<| IL_noted_signal_states ||
                IL_signal_locked := IL_signal_locked - signals
            ELSE skip END
        END;
    IS_endChangeDerailer(DerailerId) =
        ANY
            destState
        WHERE
            DerailerId : dom(IS_derailersInMovement)
            & destState = IS_derailersInMovement(DerailerId)
        THEN
            IS_derailer_states(DerailerId) := destState ||
            IS_derailersInMovement := IS_derailersInMovement - {DerailerId |-> destState} ||
            IF prj2(destState) = position_passablePosition THEN
                IS_next := IS_next \/ RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId)
            ELSE skip END
        END;
    IS_startChangeCrossing(CrossingId, currState, DestState) =
        ANY
            signals
        WHERE
            CrossingId : dom(IS_crossing_states)
            & IS_crossing_keyLocked(CrossingId) = FALSE
            & CrossingId /: dom(IL_crossing_locked_routes)
            & currState = IS_crossing_states(CrossingId)
            & DestState : {prj1(IS_crossing_states(CrossingId))} * RailML3_IL_CROSSING_POSITIONS
            & currState /= DestState
            & CrossingId /: dom(IS_crossingsInMovement)
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_CROSSING_BRANCHES(CrossingId)
                => prj1(prj1(branch)) /: RS_trainOccupiedBlocks(train) or prj1(prj2(branch)) /: RS_trainOccupiedBlocks(train)  )
            & !tvd.(tvd : union(RailML3_IL_MOVABLE_CROSSING_HAS_TVD_SECTION[{CrossingId}]) => tvd /: dom(IL_occupiedTvdSections))
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & union(ran(RailML3_IS_SIGNAL_CONTROL_SECTIONS(i_sig))) /\ IS_next /\ RailML3_IS_CROSSING_BRANCHES(CrossingId) /= {} }
        THEN
            IS_crossingsInMovement := IS_crossingsInMovement \/ {CrossingId |-> DestState} ||
            IS_next := IS_next - (RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(currState) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(currState))) ||
            IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
            IL_noted_signal_states := signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - signals
        END;
    IS_endChangeCrossing(CrossingId) =
        ANY
            destState
        WHERE
            CrossingId : dom(IS_crossingsInMovement)
            & destState = IS_crossingsInMovement(CrossingId)
        THEN
            IS_crossing_states(CrossingId) := destState ||
            IS_crossingsInMovement := IS_crossingsInMovement - {CrossingId |-> destState} ||
            IS_next := IS_next \/ (RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(destState) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(destState)))
        END;
    IS_startChangeSwitch(SwitchId, CurrState, DestState) =
        ANY
            signals
        WHERE
            SwitchId : dom(IS_switch_states)
            & IF SwitchId : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) \/ ran(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) THEN
                {switch_switchCrossingPart, switch_singleSwitchCrossing, switch_doubleSwitchCrossing} /\ RailML3_IS_SWITCH_TYPE[{SwitchId}] /= {} 
                or
                IF SwitchId : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) THEN RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT(SwitchId) : union(allIdsOfType[{"crossing","derailerIS"}]) ELSE bfalse END
            ELSE btrue END
            & IS_switch_keyLocked(SwitchId) = FALSE
            & SwitchId /: dom(IL_switch_locked_routes)
            // currentBranch in IS_next            
            & CurrState = IS_switch_states(SwitchId)
            & DestState : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId))
            & CurrState /= DestState
            & SwitchId /: dom(IS_switchsInMovement)
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId}] & i_sw_il |-> DestState(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'switch) /\ dom(union(ran(IS_switchsInMovement))) = {} )
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId)
                => prj1(prj1(branch)) /: RS_trainOccupiedBlocks(train) or prj1(prj2(branch)) /: RS_trainOccupiedBlocks(train)  )
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId}]) => tvd /: dom(IL_occupiedTvdSections))
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & union(ran(RailML3_IS_SIGNAL_CONTROL_SECTIONS(i_sig))) /\ IS_next /\ RailML3_IS_SWITCH_BRANCHES(SwitchId) /= {} }
        THEN
            IS_switchsInMovement := IS_switchsInMovement \/ {SwitchId |-> DestState} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId)(CurrState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId)(CurrState))) ||
            IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
            IL_noted_signal_states := signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - signals
        END;
    IS_endChangeSwitch(SwitchId) =
        ANY
            destState
        WHERE
            SwitchId : dom(IS_switchsInMovement)
            & destState = IS_switchsInMovement(SwitchId)
        THEN
            IS_switch_states(SwitchId) := destState ||
            IS_switchsInMovement := IS_switchsInMovement - {SwitchId |-> destState} ||
            IS_next := IS_next \/ (RailML3_IL_SWITCH_BRANCHES(SwitchId)(destState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId)(destState)))
        END;
    // COUPLED SWITCHES (here only start operation is needed - end is performed individually)
    IS_startChangeCoupledSwitches(SwitchId1, CurrState1, DestState1, SwitchId2, CurrState2, DestState2) =
        ANY
            signals
        WHERE
            SwitchId1 : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT)
            & SwitchId2 = RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT(SwitchId1)
            & !t.(t : {switch_switchCrossingPart, switch_singleSwitchCrossing, switch_doubleSwitchCrossing} => t /: RailML3_IS_SWITCH_TYPE[{SwitchId1,SwitchId2}])
        // Switch1 Guards:
            & SwitchId1 : dom(IS_switch_states)
            & IS_switch_keyLocked(SwitchId1) = FALSE
            & SwitchId1 /: dom(IL_switch_locked_routes)
            // currentBranch in IS_next
            & CurrState1 = IS_switch_states(SwitchId1)
            & DestState1 : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId1))
            & CurrState1 /= DestState1
            & SwitchId1 /: dom(IS_switchsInMovement)
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId1}] & i_sw_il |-> DestState1(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'switch) /\ dom(union(ran(IS_switchsInMovement))) = {} )
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId1)
                => prj1(prj1(branch)) /: RS_trainOccupiedBlocks(train) or prj1(prj2(branch)) /: RS_trainOccupiedBlocks(train)  )
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId1}]) => tvd /: dom(IL_occupiedTvdSections))
        // Switch2 Guards:
            & SwitchId2 : dom(IS_switch_states)
            & IS_switch_keyLocked(SwitchId2) = FALSE
            & SwitchId2 /: dom(IL_switch_locked_routes)
            // currentBranch in IS_next
            & CurrState2 = IS_switch_states(SwitchId2)
            & DestState2 : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId2))
            & CurrState2 /= DestState2
            & SwitchId2 /: dom(IS_switchsInMovement)
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId2}] & i_sw_il |-> DestState2(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'switch) /\ dom(union(ran(IS_switchsInMovement))) = {} )
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId2)
                => prj1(prj1(branch)) /: RS_trainOccupiedBlocks(train) or prj1(prj2(branch)) /: RS_trainOccupiedBlocks(train)  )
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId2}]) => tvd /: dom(IL_occupiedTvdSections))
        // Signals:
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & union(ran(RailML3_IS_SIGNAL_CONTROL_SECTIONS(i_sig))) /\ IS_next /\ union(RailML3_IS_SWITCH_BRANCHES[{SwitchId1, SwitchId2}]) /= {} }
        THEN
            IS_switchsInMovement := IS_switchsInMovement \/ {SwitchId1 |-> DestState1} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId1)(CurrState1) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId1)(CurrState1)));
            IS_switchsInMovement := IS_switchsInMovement \/ {SwitchId2 |-> DestState2} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId2)(CurrState2) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId2)(CurrState2))) ||
            IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
            IL_noted_signal_states := signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - signals
        END;

    IL_startRouteReservation(Route, Train) =
        ANY
            signals
        WHERE
            Route : allIdsOfType("route") - dom(IL_routes_in_res \/ IL_res_routes)
            & Train : (dom(RS_requestingArrivalTrains) \/ RS_arrivedTrains)
            & (RS_trainFront(Train) |-> prj2(RailML3_IL_ROUTE_ENTRY(Route)) : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1 or RS_trainFront(Train) = prj2(RailML3_IL_ROUTE_ENTRY(Route)))
            & (IF Route : dom(RailML3_IL_ROUTE_ACTIVATION_SECTIONS) THEN
                #actTvd.(actTvd : RailML3_IL_ROUTE_ACTIVATION_SECTIONS(Route) & Train : IL_occupiedTvdSections[{actTvd}] & RS_trainFront(Train) : (dom(RailML3_IL_TVD_SECTIONS(actTvd)) \/ ran(RailML3_IL_TVD_SECTIONS(actTvd))))
            ELSE
                RS_trainFront(Train) |-> prj2(RailML3_IL_ROUTE_ENTRY(Route)) : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS // train could stand on first (prj1) block
            END
                or RailML3_IL_ROUTE_LOCKS_AUTOMATICALLY(Route) = TRUE
                or #(signalplan, aspects).( signalplan : IL_signalplan_in_activation & prj1(RailML3_IL_ROUTE_ENTRY(Route)) |-> aspects : signalplan'master & aspect_closed /: aspects )
                or #r.(r : dom(IL_res_routes) & RailML3_IL_ROUTE_EXIT(r) = RailML3_IL_ROUTE_ENTRY(Route))
            )
            & !tvd.(tvd : union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) - IL_occupiedTvdSections~[{Train}] => tvd /: dom(IL_occupiedTvdSections))
            & !r.(r : union(RailML3_IL_CONFLICTING_ROUTES[{Route}]) => r /: dom(IL_routes_in_res \/ IL_res_routes))            
            & !r.(r : dom(IL_routes_in_res \/ IL_res_routes) & r /= Route => union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) /\ union(RailML3_IL_ROUTE_TVD_SECTIONS[{r}]) = {} & RailML3_IL_ROUTE_NXT(r) /\ RailML3_IL_ROUTE_NXT(Route) = {} )
            & !o.(o : dom(IL_res_overlaps) & IL_res_overlaps(o) /= Train => RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_ROUTE_NXT(Route) = {} )
            & !(o,o2).(o : union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) & o2 : dom(IL_res_overlaps) & o /= o2 => o /: dom(IL_res_overlaps) & RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_OVERLAP_NXT(o2) = {} & !r.(r : dom(IL_routes_in_res \/ IL_res_routes) & r /= Route => RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_ROUTE_NXT(r) = {}))
            // & no overlap with other res routes resp. net elements are not occupied
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & RailML3_IL_ROUTE_NXT(Route) /\ union(ran(RailML3_IS_SIGNAL_CONTROL_SECTIONS(i_sig))) /= {} }
        THEN
            IL_routes_in_res := IL_routes_in_res \/ {Route |-> Train} ||
            IL_rsrtbl := IL_rsrtbl \/ (RailML3_IL_ROUTE_RTBL |> {Route}) ||
            IL_res_blocks := IL_res_blocks \/ (RailML3_IL_ROUTE_RTBL~)[{Route}] ||
            IL_res_overlaps := IL_res_overlaps \/ (union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) * {Train}) ||
            IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
            IL_noted_signal_states := signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - signals
        END;
    IL_endRouteReservation(Route, Train) =
        ANY
            r_overlaps, r_relations, r_crossings, r_derailers, r_switches
        WHERE
            Route |-> Train : IL_routes_in_res
            & RailML3_IL_ROUTE_NXT(Route) <: IS_next
            & r_overlaps = union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            & r_relations = union(RailML3_IL_ROUTE_ADDITIONAL_RELATIONS[{Route}])

            & r_crossings = union(RailML3_IL_ROUTE_CROSSING_POSITIONS[{Route}]) \/ union(RailML3_IL_OVERLAP_CROSSING_POSITIONS[r_overlaps])
            & !i_cr.( i_cr : dom(r_crossings) => r_crossings(i_cr) = IS_crossing_states(i_cr) )            
            & !i_cr_il.(i_cr_il : dom(ran(r_crossings)) => i_cr_il /: dom(ran(IS_crossingsInMovement)) )

            & r_derailers = union(RailML3_IL_ROUTE_DERAILERS[{Route}]) \/ union(RailML3_IL_OVERLAP_DERAILERS[r_overlaps])
                \/ dom({ der, rr | rr : r_relations & der : dom(union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[{proving_continuously} * BOOL])) })
            & !i_der.( i_der : r_derailers => prj1(IS_derailer_states(i_der)) /: dom(ran(IS_derailersInMovement)) & prj2(IS_derailer_states(i_der)) = position_passablePosition )

            & r_switches = union(RailML3_IL_ROUTE_SWITCH_POSITIONS[{Route}]) \/ union(RailML3_IL_OVERLAP_SWITCH_POSITIONS[r_overlaps])
                \/ dom({ sw, rr | rr : r_relations & sw : union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[{proving_continuously} * BOOL]) })
                \/ dom({ sw, o | o : r_overlaps & sw : union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[{proving_continuously} * BOOL]) })
            & !i_sw.( i_sw : dom(r_switches) => r_switches(i_sw) <: IS_switch_states(i_sw) )            
            & !i_sw_il.(i_sw_il : dom(union(ran(r_switches))) => i_sw_il /: dom(union(ran(IS_switchsInMovement))) )

            & (#aspectRelation.(
                    aspectRelation : dom(RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES) /\ dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS)
                    & Route : RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES(aspectRelation)
                    & dom({ s1, s2 | #asp_rel.( asp_rel = RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS(aspectRelation) & s2 : asp_rel'master \/ asp_rel'slave \/ asp_rel'distant & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) ) }) <: IL_noted_signal_states
                )
                or #signalplan.(Route : dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE)
                    & signalplan : RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE(Route)
                    & dom({ s1, s2 | s2 : (signalplan'master \/ signalplan'slave \/ signalplan'distant) & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) <: IL_noted_signal_states
                )
            )
            // & falls vorhanden: Precond. aus route relation erfüllt
            & !rr.(rr : r_relations =>
                IF rr : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS) THEN
                    union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_derailer_states
                    & !rrr.(rrr : union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_derailer_states)
                ELSE btrue END        
                & IF rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS) THEN
                    union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_switch_states
                    & !rrr.(rrr : union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_switch_states)
                ELSE btrue END        
            ) // TODO for later extension: detection / section states
                
            & !o.(o : r_overlaps =>
                IF o : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS) THEN
                    union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_switch_states
                    & !rrr.(rrr : union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_switch_states)
                ELSE btrue END)
        THEN
            IL_routes_in_res := IL_routes_in_res - {Route |-> Train} ||
            IL_res_routes := IL_res_routes \/ {Route |-> Train} ||
            IL_crossing_locked_routes := IL_crossing_locked_routes \/ (dom(r_crossings) * {Route}) ||
            IL_derailer_locked_routes := IL_derailer_locked_routes \/ (r_derailers * {Route}) ||
            IL_switch_locked_routes := IL_switch_locked_routes \/ (dom(r_switches) * {Route}) ||
            IL_released_partialRoutes := IL_released_partialRoutes \/ {Route |-> {}}
        END;
    /*IL_abortRouteReservation(Route) =
        ANY
            train, NXT, NXT_partOfTwoRoutes
        WHERE
            Route |-> train : IL_routes_in_res
            & NXT = RailML3_IL_ROUTE_NXT(Route)
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT)
                & ( ( por = prj2(RailML3_IL_ROUTE_ENTRY(Route)) & por = prj2(RailML3_IL_ROUTE_EXIT(r)) )
                    or
                ( por = prj2(RailML3_IL_ROUTE_EXIT(Route)) & por = prj2(RailML3_IL_ROUTE_ENTRY(r)) ) )
            })
            // TODO: undo signalplan
        THEN
            IL_routes_in_res := IL_routes_in_res - {Route |-> train} ||
            IL_rsrtbl := IL_rsrtbl |>> {Route} ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_RTBL~)[{Route}] /\ ( (dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes ) ) ||
            IL_res_overlaps := IL_res_overlaps - (union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) * {train})
        END;*/
    
    IL_partialRouteReleaseAhead(Route, routeReleaseGroup) =
        ANY
            train, r_crossings, r_derailers, r_switches, r_signals, NXT, NXT_partOfTwoRG, NXT_partOfTwoRoutes
        WHERE
            Route |-> train : IL_res_routes
            & RS_train_states(train) = train_standing
            & Route : dom(RailML3_IL_ROUTE_RELEASE_GROUPS)// /\ dom(RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(routeReleaseGroup)) should be checked in rules
            & routeReleaseGroup : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead")
            & routeReleaseGroup /: IL_released_partialRoutes(Route)
            & r_crossings = dom({i_cr, branch | i_cr : IL_crossing_locked_routes~[{Route}] & branch : RailML3_IS_CROSSING_BRANCHES(i_cr) & branch : NXT })
            & r_derailers = dom({i_der, branch | i_der : IL_derailer_locked_routes~[{Route}] & branch : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) & branch : NXT })
            & r_switches = dom({i_sw, branch | i_sw : IL_switch_locked_routes~[{Route}] & branch : RailML3_IS_SWITCH_BRANCHES(i_sw) & branch : NXT })
            & r_signals = dom({i_sig, pos | i_sig : allIdsOfType("signalIS") & pos : RailML3_IS_SPOT_LOCATIONS(i_sig) & pos : dom(NXT) \/ ran(NXT) & i_sig /= prj1(RailML3_IL_ROUTE_EXIT(Route)) })
            & !i_tvd.(i_tvd : RailML3_IL_ROUTE_RELEASE_GROUPS_AHEAD_TVD_SECTIONS(routeReleaseGroup) => i_tvd /: dom(IL_occupiedTvdSections))
            & NXT = RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(routeReleaseGroup)(Route)
            & NXT_partOfTwoRG = dom({ porg, rg | rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead")
                & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                & porg : RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(routeReleaseGroup)(Route) /\ RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(Route) })
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes)
                & por : RailML3_IL_ROUTE_NXT(Route)
                & prj2(por) = prj2(RailML3_IL_ROUTE_EXIT(Route)) & prj2(por) = prj2(RailML3_IL_ROUTE_ENTRY(r)) })
        THEN
            IL_released_partialRoutes(Route) := IL_released_partialRoutes(Route) \/ {routeReleaseGroup} ||
            IL_rsrtbl := IL_rsrtbl - ( (dom(NXT-NXT_partOfTwoRG) \/ ran(NXT-NXT_partOfTwoRG)) <| (RailML3_IL_ROUTE_RTBL |> {Route}) ) ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_RTBL~)[{Route}] /\ (dom(NXT-(NXT_partOfTwoRG \/ NXT_partOfTwoRoutes)) \/ ran(NXT-(NXT_partOfTwoRG \/ NXT_partOfTwoRoutes))) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes - (r_crossings * {Route}) ||
            IL_derailer_locked_routes := IL_derailer_locked_routes - (r_derailers * {Route}) ||
            IL_switch_locked_routes := IL_switch_locked_routes - (r_switches * {Route}) ||
            IL_signal_locked := IL_signal_locked - r_signals ||
            IL_signal_states := IL_signal_states <+ (r_signals * {{aspect_closed}})
            // movable elements return to preferred position
        END;
    IL_completeRouteReleaseAhead(Route) =
        ANY
            train, NXT, NXT_partOfTwoRoutes, r_signals, currTvdSection
        WHERE
            Route |-> train : IL_res_routes
            & RS_train_states(train) = train_standing
            & currTvdSection : allIdsOfType("tvdSection")
            & IL_occupiedTvdSections~[{train}] = {currTvdSection}
            & RailML3_IL_TVD_SECTION_BERTHING_TRACKS(currTvdSection) = TRUE
            & RS_trainFront(train) |-> prj2(RailML3_IL_ROUTE_EXIT(Route)) : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1 // allows release even when the train is on the route
            & RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead") <: IL_released_partialRoutes(Route)            
            & NXT = RailML3_IL_ROUTE_NXT(Route)
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT)
                & ( ( por = prj2(RailML3_IL_ROUTE_ENTRY(Route)) & por = prj2(RailML3_IL_ROUTE_EXIT(r)) )
                    or
                ( por = prj2(RailML3_IL_ROUTE_EXIT(Route)) & por = prj2(RailML3_IL_ROUTE_ENTRY(r)) ) )
            })
            & r_signals = dom({ i_sig, pos | i_sig : allIdsOfType("signalIS") & pos : RailML3_IS_SPOT_LOCATIONS(i_sig) & pos : dom(NXT) \/ ran(NXT) & i_sig /= prj1(RailML3_IL_ROUTE_EXIT(Route)) })
            & !(i_sig,sp).(i_sig : r_signals & sp : IL_signalplan_in_activation => i_sig /: RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master \/ sp'slave \/ sp'distant)] )
        THEN
            IL_released_partialRoutes := {Route} <<| IL_released_partialRoutes ||
            IL_res_routes := IL_res_routes - {Route |-> train} ||
            IL_rsrtbl := IL_rsrtbl |>> {Route} ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_RTBL~)[{Route}] /\ ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes |>> {Route} ||
            IL_derailer_locked_routes := IL_derailer_locked_routes |>> {Route} ||
            IL_switch_locked_routes := IL_switch_locked_routes |>> {Route} ||
            IL_signal_states := IL_signal_states <+ (r_signals * {{aspect_closed}}) ||
            IL_noted_signal_states := r_signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - r_signals ||
            IL_res_overlaps := IL_res_overlaps |>> {train} ||
            IL_overlaps_in_release := IL_overlaps_in_release - union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            // movable elements return to preferred position
        END;

    IL_partialRouteReleaseRear(Route, routeReleaseGroup) =
        ANY
            train, r_crossings, r_derailers, r_switches, NXT, NXT_partOfTwoRG, NXT_partOfTwoRoutes
        WHERE
            Route |-> train : IL_res_routes
            & Route : dom(RailML3_IL_ROUTE_RELEASE_GROUPS)
            & routeReleaseGroup : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
            & routeReleaseGroup /: IL_released_partialRoutes(Route)
            & r_crossings = dom({i_cr, branch | i_cr : IL_crossing_locked_routes~[{Route}] & branch : RailML3_IS_CROSSING_BRANCHES(i_cr) & branch : NXT })
            & r_derailers = dom({i_der, branch | i_der : IL_derailer_locked_routes~[{Route}] & branch : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) & branch : NXT })
            & r_switches = dom({i_sw, branch | i_sw : IL_switch_locked_routes~[{Route}] & branch : RailML3_IS_SWITCH_BRANCHES(i_sw) & branch : NXT })
            & !i_tvd.(i_tvd : RailML3_IL_ROUTE_RELEASE_GROUPS_REAR_TVD_SECTIONS(routeReleaseGroup) => i_tvd /: dom(IL_occupiedTvdSections))
            & NXT = RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(routeReleaseGroup)(Route)
            & NXT_partOfTwoRG = dom({ porg, rg | rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
                & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                & porg : (dom(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route)) \/ ran(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route))) /\ (dom(NXT) \/ ran(NXT))
                })
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes)
                & por : RailML3_IL_ROUTE_NXT(Route)
                & prj2(por) = prj2(RailML3_IL_ROUTE_EXIT(Route)) & prj2(por) = prj2(RailML3_IL_ROUTE_ENTRY(r)) })
            & !(rg, first_block_of_rg).( rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
                    & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                    & first_block_of_rg : dom(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route)) & first_block_of_rg /: ran(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route))
                => #bl.(bl : dom(NXT) \/ ran(NXT) & bl |-> first_block_of_rg : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1 or first_block_of_rg = bl) )
            & !nxt.(nxt : NXT => prj2(nxt) |-> RS_trainBack(train) : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1)
        THEN
            IL_released_partialRoutes(Route) := IL_released_partialRoutes(Route) \/ {routeReleaseGroup} ||
            IL_rsrtbl := IL_rsrtbl - ( ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRG) <| (RailML3_IL_ROUTE_RTBL |> {Route}) ) ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_RTBL~)[{Route}] /\ ((dom(NXT-NXT_partOfTwoRoutes) \/ ran(NXT-NXT_partOfTwoRoutes)) - NXT_partOfTwoRG) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes - (r_crossings * {Route}) ||
            IL_derailer_locked_routes := IL_derailer_locked_routes - (r_derailers * {Route}) ||
            IL_switch_locked_routes := IL_switch_locked_routes - (r_switches * {Route})
            // movable elements return to preferred position
        END;
    IL_completeRouteReleaseRear(Route) =
        ANY
            train, NXT, NXT_partOfTwoRoutes, r_signals
        WHERE
            Route |-> train : IL_res_routes
            & prj2(RailML3_IL_ROUTE_EXIT(Route)) |-> RS_trainBack(train) : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS_closure1
            & RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear") <: IL_released_partialRoutes(Route)
            & !i_tvd.(i_tvd : union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) => i_tvd /: dom(IL_occupiedTvdSections))
            & NXT = RailML3_IL_ROUTE_NXT(Route)
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT)
                & ( ( por = prj2(RailML3_IL_ROUTE_ENTRY(Route)) & por = prj2(RailML3_IL_ROUTE_EXIT(r)) )
                    or
                ( por = prj2(RailML3_IL_ROUTE_EXIT(Route)) & por = prj2(RailML3_IL_ROUTE_ENTRY(r)) ) )
            })
            & r_signals = dom({ i_sig, pos | i_sig : allIdsOfType("signalIS") & pos : RailML3_IS_SPOT_LOCATIONS(i_sig) & pos : dom(NXT) \/ ran(NXT) & i_sig /= prj1(RailML3_IL_ROUTE_EXIT(Route)) })
        THEN
            IL_released_partialRoutes := {Route} <<| IL_released_partialRoutes ||
            IL_res_routes := IL_res_routes - {Route |-> train} ||
            IL_rsrtbl := IL_rsrtbl |>> {Route} ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_RTBL~)[{Route}] /\ ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes |>> {Route} ||
            IL_derailer_locked_routes := IL_derailer_locked_routes |>> {Route} ||
            IL_switch_locked_routes := IL_switch_locked_routes |>> {Route} ||
            IL_signal_states := IL_signal_states <+ (r_signals * {{aspect_closed}}) ||
            IL_noted_signal_states := r_signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - r_signals ||
            IL_res_overlaps := IL_res_overlaps |>> {train} ||
            IL_overlaps_in_release := IL_overlaps_in_release - union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            // movable elements return to preferred position
        END;

    IL_startOverlapRelease(OverlapId) =
        ANY
            train
        WHERE
            OverlapId |-> train : IL_res_overlaps
            & OverlapId : dom(RailML3_IL_OVERLAP_RELEASE_TRIGGER_SECTION)
            & OverlapId /: IL_overlaps_in_release
            & RailML3_IL_OVERLAP_RELEASE_TRIGGER_SECTION(OverlapId) |-> train : IL_occupiedTvdSections
        THEN
            IL_overlaps_in_release := IL_overlaps_in_release \/ {OverlapId}
        END;
    IL_endOverlapRelease(OverlapId) =
        ANY
            r_crossings, r_derailers, r_switches
        WHERE
            OverlapId : IL_overlaps_in_release
            & r_crossings = dom(union(RailML3_IL_OVERLAP_CROSSING_POSITIONS[{OverlapId}]))
            & r_derailers = union(RailML3_IL_OVERLAP_DERAILERS[{OverlapId}])
            & r_switches = dom(union(RailML3_IL_OVERLAP_SWITCH_POSITIONS[{OverlapId}]))
        THEN
            IL_overlaps_in_release := IL_overlaps_in_release - {OverlapId} ||
            IL_res_overlaps := {OverlapId} <<| IL_res_overlaps ||
            IL_crossing_locked_routes := r_crossings <<| IL_crossing_locked_routes ||
            IL_derailer_locked_routes := r_derailers <<| IL_derailer_locked_routes ||
            IL_switch_locked_routes := r_switches <<| IL_switch_locked_routes
        END;
    
    IL_startActivateSignalplan(Signalplan) =
        SELECT
            Signalplan /: IL_signalplan_in_activation
            & ( #aspectRelation.(
                    aspectRelation : dom(RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES) /\ dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS)
                    & not(!route.(route : RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES(aspectRelation) => route /: dom(IL_routes_in_res)))
                    & Signalplan = RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS(aspectRelation)
                )
                or #route.(
                    route : dom(IL_routes_in_res)
                    & route : dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE)
                    & Signalplan : RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE(route)
                )
            )
            & !sp.(sp : IL_signalplan_in_activation => sp'master /= Signalplan'master & sp'slave /= Signalplan'slave)
            & LET signalplan_relation BE signalplan_relation = Signalplan'master \/ Signalplan'slave \/ Signalplan'distant IN
                !(signal_il, signal_is).(signal_il : dom(signalplan_relation) & signal_is = RailML3_IL_SIGNAL_REFERS_TO(signal_il) =>
                    IL_signal_states(signal_is) = {aspect_closed}
                    & signal_is /: IL_signal_locked
                    & IF signal_is : dom(IL_noted_signal_states) THEN signalplan_relation(signal_il) = IL_noted_signal_states(signal_is) ELSE btrue END
                )
            END
        THEN
            IL_signalplan_in_activation := IL_signalplan_in_activation \/ {Signalplan} ||
            //IL_signal_locked_by_signalplan := IL_signal_locked_by_signalplan \/ dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) ||
            IL_signal_locked := IL_signal_locked \/ dom({ s1, s2 | s2 : dom(Signalplan'slave \/ Signalplan'distant) & s1 = RailML3_IL_SIGNAL_REFERS_TO(s2) })
        END;
    IL_endActivateSignalplan(Signalplan) =
        ANY
            signalplan_relation
        WHERE
            Signalplan : IL_signalplan_in_activation
            & signalplan_relation = Signalplan'master \/ Signalplan'slave \/ Signalplan'distant
            & ( #aspectRelation.(
                    aspectRelation : dom(RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES) /\ dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS)
                    & #route.(route : RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES(aspectRelation) & route : dom(IL_res_routes))
                    & Signalplan = RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS(aspectRelation)
                )
                or #route.(
                    route : dom(IL_res_routes)
                    & route : dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE)
                    & Signalplan : RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE(route)
                )
            )
            & dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) <: IL_noted_signal_states
        THEN
            IL_signalplan_in_activation := IL_signalplan_in_activation - {Signalplan} ||
            //IL_signal_locked_by_signalplan := IL_signal_locked_by_signalplan - dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) ||
            IL_signal_locked := IL_signal_locked - dom({ s1, s2 | s2 : dom(signalplan_relation) & s1 = RailML3_IL_SIGNAL_REFERS_TO(s2) & aspect_closed : signalplan_relation(s2) }) ||
            IL_signal_states := IL_signal_states <+ dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) ||
            IL_noted_signal_states := IL_noted_signal_states
                - dom({ s1, s2 | s2 : signalplan_relation & not( #sp.( sp : IL_signalplan_in_activation & sp /= Signalplan & s2 : sp'master \/ sp'slave \/ sp'distant ) ) & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) 
        END;
    /*IL_abortActivateSignalplan(Signalplan) =
        SELECT
            Signalplan : IL_signalplan_in_activation
        THEN
            IL_signalplan_in_activation := IL_signalplan_in_activation - {Signalplan} ||
            IL_signal_locked_by_signalplan := IL_signal_locked_by_signalplan - dom({ s1, s2 | s2 : Signalplan & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) ||
            IL_signal_locked := IL_signal_locked - dom({ s1, s2 | s2 : dom(Signalplan) & s1 = RailML3_IL_SIGNAL_REFERS_TO(s2) }) ||
            IL_noted_signal_states := IL_noted_signal_states - dom({ s1, s2 | s2 : Signalplan & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) 
        END;*/
    IL_noteChangeSignalState(SignalId) = // for signals that are controlled by at least one signalplan
        ANY
            currAspect, destAspect
        WHERE
            SignalId : allIdsOfType("signalIS")
            & SignalId /: dom(IL_noted_signal_states)
            & currAspect = IL_signal_states(SignalId)
            & #(signalplan, sp_rel, signalId_il).(signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] & sp_rel = signalplan'master \/ signalplan'slave \/ signalplan'distant & signalId_il : dom(sp_rel)
                & destAspect = sp_rel(signalId_il) )
            & ( !(signalplan, signalId_il).( signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] /\ dom(ran(RailML3_IL_ROUTE_ENTRY)) /\ dom(signalplan'master) & aspect_closed /: destAspect =>
                #route.( route : dom(RailML3_IL_ROUTE_ENTRY) & prj1(RailML3_IL_ROUTE_ENTRY(route)) = signalId_il & route : dom(IL_res_routes) ) )
            or 
            #( signalplan, signalId_il).( signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] /\ dom(ran(RailML3_IL_ROUTE_ENTRY)) /\ dom(signalplan'slave) & aspect_closed /: destAspect &
                #route.( route : dom(RailML3_IL_ROUTE_ENTRY) & prj1(RailML3_IL_ROUTE_ENTRY(route)) = signalId_il & route : dom(IL_routes_in_res) ) )
            )
            // following track to next signal or trackEnd is passable, i.e. in IS_next, and all tvdSections are free (only if destAspect is not aspect_closed, closing a signal is always possible):
            & IF aspect_closed /: destAspect THEN
                #(sig_exit, sloc_sig, sloc_sig_exit).(
                    sig_exit : RailML3_IS_SIGNAL_END_OF_CONTROL_SECTION(SignalId)
                    & sloc_sig : RailML3_IS_SPOT_LOCATIONS(SignalId)
                    & sloc_sig_exit : RailML3_IS_SPOT_LOCATIONS(sig_exit)
                    & sloc_sig |-> sloc_sig_exit : closure1(IS_next)
                    & !tvd.(tvd : RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(SignalId)(sig_exit) => tvd /: dom(IL_occupiedTvdSections))
                )
            ELSE btrue END
        THEN
            IL_noted_signal_states := IL_noted_signal_states \/ {SignalId |-> destAspect}
            //IL_signal_locked_by_signalplan := IL_signal_locked_by_signalplan - {SignalId |-> destState} ||
            //IL_signal_locked := IL_signal_locked \/ {SignalId}
        END;
    IL_changeSignalState(SignalId) = // for signals that are not controlled by any signalplan
        ANY
            currAspect, destAspect
        WHERE
            SignalId : allIdsOfType("signalIS")
            & SignalId /: dom(IL_noted_signal_states)
            & currAspect = IL_signal_states(SignalId)
            & SignalId /: IL_signal_locked
            // independent signal or not locked distant signal from any signalplan:
            & (SignalId : RailML3_IL_SIGNAL_NOT_CONTROLLED_BY_SIGNALPLAN
                or
                #signalplan.(
                    ( #aspectRelation.(
                        aspectRelation : dom(RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES) /\ dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS)
                        & #route.(route : RailML3_IL_ASPECT_RELATION_APPLIES_TO_ROUTES(aspectRelation) & route : dom(IL_res_routes))
                        & signalplan = RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS(aspectRelation)
                    )
                    or #route.(
                        route : dom(IL_res_routes)
                        & route : dom(RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE)
                        & signalplan : RailML3_IL_ASPECT_RELATION_SIGNAL_ASPECTS_INFERRED_FROM_ROUTE(route)
                    ) )
                    & #signalId_il.( signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}]
                        & ( signalId_il : dom(signalplan'distant)
                            or
                            ( signalId_il : dom(signalplan'master) & signalId_il /: dom(ran(RailML3_IL_ROUTE_ENTRY)) )
                        )
                    )
                )
            )
            & destAspect : {{aspect_proceed}, {aspect_closed}}
            & destAspect /= currAspect
            // following track to next signal or trackEnd is passable, i.e. in IS_next, and all tvdSections are free (only if destAspect is not aspect_closed, closing a signal is always possible):
            & IF aspect_closed /: destAspect THEN
                #(sig_exit, sloc_sig, sloc_sig_exit).(
                    sig_exit : RailML3_IS_SIGNAL_END_OF_CONTROL_SECTION(SignalId)
                    & sloc_sig : RailML3_IS_SPOT_LOCATIONS(SignalId)
                    & sloc_sig_exit : RailML3_IS_SPOT_LOCATIONS(sig_exit)
                    & sloc_sig |-> sloc_sig_exit : closure1(IS_next)
                    & !tvd.(tvd : RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(SignalId)(sig_exit) => tvd /: dom(IL_occupiedTvdSections))
                    & !route.(route : dom(IL_routes_in_res \/ IL_res_routes) => RailML3_IL_ROUTE_NXT(route) /\ RailML3_IS_SIGNAL_CONTROL_SECTIONS(SignalId)(sig_exit) = {})
                )
            ELSE btrue END
        THEN
            IL_signal_states(SignalId) := destAspect
        END;

    RS_trainArrivalRequest(Position, Train) =
        ANY
            tvdSection, signals
        WHERE
            Position : dom(union(RailML3_IS_SPOT_LOCATIONS[RailML3_IS_OPENEND_IDS])) * {direction_normal, direction_reverse}
            & Position : dom(IS_next)
            & tvdSection : allIdsOfType("tvdSection")
            & tvdSection /: dom(IL_occupiedTvdSections)
            & Position : dom(RailML3_IL_TVD_SECTIONS(tvdSection))
            & Position /: dom(union(RailML3_IL_TVD_SECTIONS[dom(IL_occupiedTvdSections)]))
            & IS_next(Position) /: dom(union(RailML3_IL_TVD_SECTIONS[dom(IL_occupiedTvdSections)]))
            & Train : RS_trains - (RS_arrivedTrains \/ dom(RS_requestingArrivalTrains))
            & signals = { i_sig | i_sig : allIdsOfType("signalIS") & tvdSection : union(ran(RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(i_sig))) }
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections \/ {tvdSection |-> Train} ||
            RS_requestingArrivalTrains := RS_requestingArrivalTrains \/ {Train |-> (Position, tvdSection)} ||
            IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
            IL_noted_signal_states := signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - signals
        END;
    IL_trainAcceptArrival(Train) =
        ANY
            position, tvdSection, route
        WHERE
            Train |-> (position, tvdSection) : RS_requestingArrivalTrains
            & route : allIdsOfType("route")
            & IF prj1(RailML3_IL_ROUTE_ENTRY(route)) : RailML3_IS_OPENEND_IDS & prj2(RailML3_IL_ROUTE_ENTRY(route)) = position THEN
                route : dom(IL_res_routes) & IL_res_routes(route) = Train
            ELSE btrue END
        THEN            
            RS_requestingArrivalTrains := {Train} <<| RS_requestingArrivalTrains ||
            RS_arrivedTrains := RS_arrivedTrains \/ {Train} ||
            RS_trainOccupiedBlocks := RS_trainOccupiedBlocks \/ {Train |-> {prj1(position), prj1(IS_next(position))}} ||
            RS_trainFront := RS_trainFront \/ {Train |-> IS_next(position)} ||
            RS_trainBack := RS_trainBack \/ {Train |-> position} ||
            RS_train_states := RS_train_states \/ {Train |-> train_moving}
        END;
    IL_trainDeclineArrival(Train) =
        SELECT
            Train : dom(RS_requestingArrivalTrains)
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections |>> {Train} ||
            RS_requestingArrivalTrains := {Train} <<| RS_requestingArrivalTrains
        END;
    RS_trainLeave(Train) =
        ANY
            position, tvdSection
        WHERE
            position : dom(union(RailML3_IS_SPOT_LOCATIONS[RailML3_IS_OPENEND_IDS])) * {direction_normal, direction_reverse}
            & tvdSection : allIdsOfType("tvdSection")
            & tvdSection : IL_occupiedTvdSections~[{Train}]
            & position : dom(RailML3_IL_TVD_SECTIONS(tvdSection))
            & Train : RS_arrivedTrains
            & RS_trainFront(Train) = position
            & RS_trainBack(Train) |-> position : RailML3_IS_NET_RELATION_SUBSEQUENT_BLOCKS
            & RS_train_states(Train) = train_moving
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections |>> {Train} ||
            RS_arrivedTrains := RS_arrivedTrains - {Train} ||
            RS_trainOccupiedBlocks := {Train} <<| RS_trainOccupiedBlocks ||
            RS_trainFront := {Train} <<| RS_trainFront ||
            RS_trainBack := {Train} <<| RS_trainBack ||
            RS_train_states := {Train} <<| RS_train_states
        END;

    RS_trainMoveFront(currFront, newFront, Train) =
        ANY
            tvds, passed_signals, other_signals
        WHERE
            Train : RS_arrivedTrains
            & RS_train_states(Train) = train_moving
            & currFront = RS_trainFront(Train)
            & currFront |-> newFront : IS_next
            & tvds = dom({ i_t, i_tde | i_t : allIdsOfType("tvdSection") & i_tde : allIdsOfType("trainDetectionElement") & currFront : RailML3_IS_SPOT_LOCATIONS(i_tde)
                & i_tde : RailML3_IL_TVD_SECTION_DEMARCATING_ELEMENTS(i_t)})
            & passed_signals = { i_sig | i_sig : allIdsOfType("signalIS") & currFront : RailML3_IS_SPOT_LOCATIONS(i_sig) }
            & !i_sig.(i_sig : passed_signals => aspect_closed /: IL_signal_states(i_sig))
            & other_signals = { i_sig | i_sig : allIdsOfType("signalIS") & tvds /\ union(ran(RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(i_sig))) /= {} }
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections \/ (tvds * {Train}) ||
            RS_trainOccupiedBlocks(Train) := RS_trainOccupiedBlocks(Train) \/ {prj1(newFront)} ||
            RS_trainFront(Train) := newFront ||
            IL_signal_states := IL_signal_states <+ ((passed_signals \/ other_signals) * {{aspect_closed}}) ||
            IL_noted_signal_states := other_signals <<| IL_noted_signal_states <+ passed_signals * {{aspect_closed}} ||
            IL_signal_locked := IL_signal_locked - (passed_signals \/ other_signals)
        END;
    RS_trainMoveBack(currBack, newBack, Train) =
        ANY
            tvds
        WHERE
            Train : RS_arrivedTrains
            & RS_train_states(Train) = train_moving
            & currBack = RS_trainBack(Train)
            & newBack /= RS_trainFront(Train)
            & currBack |-> newBack : IS_next
            & prj1(newBack) : RS_trainOccupiedBlocks(Train)
            & tvds = dom({ i_t, i_tde | i_t : allIdsOfType("tvdSection") & i_tde : allIdsOfType("trainDetectionElement") & newBack : RailML3_IS_SPOT_LOCATIONS(i_tde)
                & i_tde : RailML3_IL_TVD_SECTION_DEMARCATING_ELEMENTS(i_t) & RS_trainOccupiedBlocks(Train) /\ dom(dom(RailML3_IL_TVD_SECTIONS(i_t)) \/ ran(RailML3_IL_TVD_SECTIONS(i_t))) = {prj1(currBack),prj1(newBack)} })
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections - (tvds * {Train}) ||
            RS_trainOccupiedBlocks(Train) := RS_trainOccupiedBlocks(Train) - {prj1(currBack)} ||
            RS_trainBack(Train) := newBack
        END;
    RS_trainChangeDirection(Train) =
        ANY
            newDir, currTvdSection
        WHERE
            Train : RS_arrivedTrains
            & RS_train_states(Train) = train_standing
            & newDir : {direction_normal, direction_reverse}
            & newDir /= prj2(RS_trainFront(Train))
            & prj2(RS_trainFront(Train)) = prj2(RS_trainBack(Train))
            & prj1(RS_trainFront(Train)) |-> newDir : ran(IS_next)
            & prj1(RS_trainBack(Train)) |-> newDir : dom(IS_next)
            & currTvdSection : allIdsOfType("tvdSection")
            & IL_occupiedTvdSections~[{Train}] = {currTvdSection}
            & RS_trainBack(Train) |-> RS_trainFront(Train) : closure1(RailML3_IL_TVD_SECTIONS(currTvdSection))
            & RailML3_IL_TVD_SECTION_BERTHING_TRACKS(currTvdSection) = TRUE
            & Train /: ran(IL_routes_in_res \/ IL_res_routes \/ IL_res_overlaps)
        THEN
            RS_trainFront(Train) := prj1(RS_trainBack(Train)) |-> newDir ||
            RS_trainBack(Train) := prj1(RS_trainFront(Train)) |-> newDir
        END;
    RS_trainChangeState(Train) =
        ANY
            destState
        WHERE
            Train : RS_arrivedTrains
            & destState = IF RS_train_states(Train) = train_moving THEN train_standing ELSE train_moving END
        THEN
            RS_train_states(Train) := destState
        END
END'''])
    END;

    printValidationMachine =
    PRE printValidation = TRUE THEN
    FPRINTF(outputValidationFile, "~w~n", ['''/*@generated */''']);
    FPRINTF(outputValidationFile, "~w~w~n", ["RULES_MACHINE ", validationMachineName]);

    FPRINTF(outputValidationFile, "~w~n", ['''DEFINITIONS
    SET_PREF_optimize_enum_set_elems == TRUE
ABSTRACT_CONSTANTS
    allIdsOfType''']);
    FPRINTF(outputValidationFile, "~w~n", ['''CONCRETE_CONSTANTS
    all_ids,''']);
    VAR variable_names, filtered_variable_names IN
        variable_names := FORMULA_INFOS("variables");
        filtered_variable_names := SQUASH({ i, name | i : dom(variable_names) & name = variable_names(i) & SUB_STRING(name, 1, 6) = "RailML" });
        FPRINTF(outputValidationFile, "    ~w~n", [STRING_JOIN(filtered_variable_names, ",\n    ")])
    END;

    FPRINTF(outputValidationFile, "~w~n", ['''PROPERTIES
    allIdsOfType = %type.( type : STRING | all_ids~[{type} * ran(ran(all_ids))] )''']);
    FPRINTF(outputValidationFile, "    & all_ids = ~w~n", [all_ids]);
    FPRINTF(outputValidationFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML"), " &"), "\n    &")]); // prevent very long lines

    //FPRINTF(outputFile, "~w~n", ['''DEFINITIONS
    //"RailML_animation.def";''']);
    //FPRINTF(outputFile, "    VISB_SVG_FILE == \"~w\"~n", [svgFile]);

    FPRINTF(outputValidationFile, "~w~n", ["SETS"]);
    VAR set_names, set_values, filtered_sets IN
        set_names := FORMULA_INFOS("sets");
        set_values := FORMULA_VALUES("sets");
        filtered_sets := SQUASH({ i, name | i : dom(set_names) & name = set_names(i)^" = "^set_values(i) & SUB_STRING(set_names(i), 1, 6) = "RailML" });
        FPRINTF(outputValidationFile, "    ~w~n", [STRING_JOIN(filtered_sets, ";\n    ")])
    END;
    FPRINTF(outputValidationFile, "~w~n", ['''DEFINITIONS
    "CHOOSE.def";
    "LibraryMath.def";
    "LibraryMeta.def";
    "LibraryReals.def";

    EBO_maximum_speed_per_curve_radius(radius, superelevation) ==
        RSQRT(RABS(radius)/11.8 * (superelevation + 130.0)); // http://www.gesetze-im-internet.de/ebo/__40.html, "Ueberhoehungsfehlbetrag" here chosen as 130 mm
OPERATIONS
    RULE EBO_CheckSwitchSpeeds
    BODY
        RULE_FORALL
            e_sw, b_lb, b_lj, b_rb, b_rj//, b_s, b_t
        WHERE
            e_sw : RailML3_switchIS
            // left branch, branching speed, if available
            & b_lb = bool( #(left, r_l, bs_l).( IF card(e_sw'leftBranch) = 1 THEN left = MU(e_sw'leftBranch) ELSE 1=1 END
                    & IF card(left'radius) = 1 THEN r_l = MU(left'radius) ELSE 1=1 END
                    & IF card(left'branchingSpeed) = 1 THEN bs_l = MU(left'branchingSpeed) ELSE 1=1 END
                    & (r_l = 0.0 or bs_l <= EBO_maximum_speed_per_curve_radius(r_l,0.0)) ) )
            // left branch, joining speed, if available
            & b_lj = bool( #(left, r_l, js_l).( IF card(e_sw'leftBranch) = 1 THEN left = MU(e_sw'leftBranch) ELSE 1=1 END
                    & IF card(left'radius) = 1 THEN r_l = MU(left'radius) ELSE 1=1 END
                    & IF card(left'joiningSpeed) = 1 THEN js_l = MU(left'joiningSpeed) ELSE 1=1 END
                    & (r_l = 0.0 or js_l <= EBO_maximum_speed_per_curve_radius(r_l,0.0)) ) )
            // right branch, branching speed, if available
            & b_rb = bool( #(right, r_r, bs_r).( IF card(e_sw'rightBranch) = 1 THEN right = MU(e_sw'rightBranch) ELSE 1=1 END
                    & IF card(right'radius) = 1 THEN r_r = MU(right'radius) ELSE 1=1 END
                    & IF card(right'branchingSpeed) = 1 THEN bs_r = MU(right'branchingSpeed) ELSE 1=1 END
                    & (r_r = 0.0 or bs_r <= EBO_maximum_speed_per_curve_radius(r_r,0.0)) ) )
            // right branch, joining speed, if available
            & b_rj = bool( #(right, r_r, js_r).( IF card(e_sw'rightBranch) = 1 THEN right = MU(e_sw'rightBranch) ELSE 1=1 END
                    & IF card(right'radius) = 1 THEN r_r = MU(right'radius) ELSE 1=1 END
                    & IF card(right'joiningSpeed) = 1 THEN js_r = MU(right'joiningSpeed) ELSE 1=1 END
                    & (r_r = 0.0 or js_r <= EBO_maximum_speed_per_curve_radius(r_r,0.0)) ) )
        EXPECT
            b_lb = TRUE & b_lj = TRUE & b_rb = TRUE & b_rj = TRUE
        COUNTEREXAMPLE
            "[EBO_CheckSwitchSpeeds] Switch id "^MU(e_sw'Id)^" has invalid speed(s):"^
            IF b_lb = FALSE
                THEN STRING_FORMAT(" [branching speed of left branch is ~w, but expected <= ~w] ", MU(MU(e_sw'leftBranch)'branchingSpeed), EBO_maximum_speed_per_curve_radius(MU(MU(e_sw'leftBranch)'radius),0.0))
                ELSE ""
            END
            ^
            IF b_lj = FALSE
                THEN STRING_FORMAT(" [joining speed of left branch is ~w, but expected <= ~w] ", MU(MU(e_sw'leftBranch)'joiningSpeed), EBO_maximum_speed_per_curve_radius(MU(MU(e_sw'leftBranch)'radius),0.0))
                ELSE ""
            END
            ^
            IF b_rb = FALSE
                THEN STRING_FORMAT(" [branching speed of right branch is ~w, but expected <= ~w] ", MU(MU(e_sw'rightBranch)'branchingSpeed), EBO_maximum_speed_per_curve_radius(MU(MU(e_sw'rightBranch)'radius),0.0))
                ELSE ""
            END
            ^
            IF b_rj = FALSE
                THEN STRING_FORMAT(" [joining speed of right branch is ~w, but expected <= ~w] ", MU(MU(e_sw'rightBranch)'joiningSpeed), EBO_maximum_speed_per_curve_radius(MU(MU(e_sw'rightBranch)'radius),0.0))
                ELSE ""
            END
        END
    END
END'''])
    END
END