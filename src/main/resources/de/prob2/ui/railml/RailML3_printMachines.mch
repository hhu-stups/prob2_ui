MACHINE RailML3_printMachines
INCLUDES RailML3_import
DEFINITIONS
    "LibraryIO.def";
    "LibraryMeta.def";
    "LibraryStrings.def";
    "SORT.def";
    SET_PREF_MAX_DISPLAY_SET == -1
CONSTANTS
    outputDataFile, outputAnimationFile, outputValidationFile, svgFile, dataMachineName, animationMachineName, validationMachineName,
    LINK_SVG, FULL_IMPORT
PROPERTIES
    {file, outputDataFile, outputAnimationFile, outputValidationFile, svgFile, dataMachineName, animationMachineName, validationMachineName} <: STRING
    & {LINK_SVG, FULL_IMPORT} <: BOOL
VARIABLES
    printData, printAnimation, printValidation
INVARIANT
    {printData, printAnimation, printValidation} <: BOOL
INITIALISATION
    importRailML(FULL_IMPORT); printData := FALSE; printAnimation := FALSE; printValidation := FALSE
OPERATIONS
    triggerPrintData =
        PRE printData = FALSE & FULL_IMPORT = TRUE THEN
            IF "FALSE" /: ran(FORMULA_VALUES("inv")) & no_error = TRUE THEN
                printData := TRUE                
            ELSIF "FALSE" : ran(FORMULA_VALUES("inv")) THEN
                ADD_ERRORS( dom({msg, viol_inv_idx | viol_inv_idx : FORMULA_VALUES("inv")~[{"FALSE"}] & msg = "RailML: Import failed due to an invariant violation: "^FORMULA_INFOS("inv")(viol_inv_idx) }) )
            ELSE ADD_ERROR("RailML: ", "Import failed") END
        END;
    triggerPrintAnimation = PRE printAnimation = FALSE & FULL_IMPORT = TRUE THEN printAnimation := TRUE END;
    triggerPrintValidation = PRE printValidation = FALSE & FULL_IMPORT = TRUE THEN printValidation := TRUE END;



    printDataMachine =
        PRE printData = TRUE THEN
            FPRINTF(outputDataFile, "~w~n", ['''/*@generated */''']);
            FPRINTF(outputDataFile, "~w~w~n", ["MACHINE ", dataMachineName]);

            FPRINTF(outputDataFile, "~w~n", ["ABSTRACT_CONSTANTS\n    allIdsOfType"]);
            FPRINTF(outputDataFile, "~w~n", ["CONCRETE_CONSTANTS\n    all_ids, railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1,"]);
            VAR variable_names, filtered_variable_names IN
                variable_names := FORMULA_INFOS("variables");
                filtered_variable_names := SQUASH({ i, name | i : dom(variable_names) & name = variable_names(i) & SUB_STRING(name, 1, 6) = "RailML" });
                FPRINTF(outputDataFile, "    ~w~n", [STRING_JOIN(filtered_variable_names, ",\n    ")])
            END;

            FPRINTF(outputDataFile, "~w~n", ["PROPERTIES\n    allIdsOfType = %type.( type : STRING | all_ids~[{type} * ran(ran(all_ids))] )\n    & railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 = closure1(RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS)"]);
            FPRINTF(outputDataFile, "    & all_ids = ~w~n", [all_ids]);
            FPRINTF(outputDataFile, "    & ~w~n", [STRING_JOIN(STRING_SPLIT(VARS_AS_TYPED_STRING("RailML"), " &"), "\n    &")]); // prevent very long lines

            // Use the following line if not imported data is needed for own validation rules. (example: e : ran(data) & e'element = "netElement" & e'attributes("id") = "test_id" & e'attributes("length") = "500.0" & ... ))
            // FPRINTF(outputDataFile, "    & data = ~w~n", [data]);

            FPRINTF(outputDataFile, "~w~n", ["SETS"]);
            VAR set_names, set_values, filtered_sets IN
                set_names := FORMULA_INFOS("sets");
                set_values := FORMULA_VALUES("sets");
                filtered_sets := SQUASH({ i, name | i : dom(set_names) & name = set_names(i)^" = "^set_values(i) & SUB_STRING(set_names(i), 1, 6) = "RailML" });
                FPRINTF(outputDataFile, "    ~w~n", [STRING_JOIN(filtered_sets, ";\n    ")])
            END;

            FPRINTF(outputDataFile, "~w", ["END"])
        END;



    printAnimationMachine =
        PRE printAnimation = TRUE THEN
            FPRINTF(outputAnimationFile, "~w~n", ['''/*@generated */''']);
            FPRINTF(outputAnimationFile, "MACHINE ~w~n", [animationMachineName]);
            FPRINTF(outputAnimationFile, "EXTENDS ~w~n", [dataMachineName]);
            
            FPRINTF(outputAnimationFile, "~w~n", ["CONSTANTS\n    RS_trainFront_init, RS_trainBack_init"]);

            FPRINTF(outputAnimationFile, "~w~n", ['''PROPERTIES
    RS_trainFront_init : RS_trains +-> (STRING * REAL * RailML3_IS_DIRECTION)
    & RS_trainBack_init : RS_trains +-> (STRING * REAL * RailML3_IS_DIRECTION)
    & RS_trainFront_init = {}
    & RS_trainBack_init = {}
    & dom(RS_trainFront_init) = dom(RS_trainBack_init)''']); // TODO: set individually

            FPRINTF(outputAnimationFile, "~w~n", ['''DEFINITIONS
    ignoreDirection(Relation) == dom({ r, cr | cr : Relation & r = prj1(prj1(cr)) |-> prj1(prj2(cr)) });
    inverseDirectedRelation(Relation) == dom({ ir, r | r : Relation & ir = prj1(prj2(r)) |-> oppositeDirection(prj2(prj2(r))) |-> (prj1(prj1(r)) |-> oppositeDirection(prj2(prj1(r)))) });
    inverseDirectedPair(Pair) == prj1(Pair) |-> oppositeDirection(prj2(Pair));
    inverseSwitchPosition(pos) == IF pos = position_left THEN position_right ELSE position_left END;
    oppositeDirection(dir) == IF dir = direction_normal THEN direction_reverse ELSE direction_normal END;

    elementOfId(Set, eId) == MU( { e | e : Set & eId = MU(e'Id)} );

    "CHOOSE.def";
    "LibraryMath.def";
    "LibraryMeta.def";
    "LibraryReals.def";
    "LibraryStrings.def";
    "SORT.def";
    SET_PREF_MEMOIZE_FUNCTIONS == TRUE;
    SET_PREF_optimize_enum_set_elems == TRUE;
    ''']);
            IF LINK_SVG = TRUE THEN FPRINTF(outputAnimationFile, "    \"RailML3_VisB.def\";~n    VISB_SVG_FILE == \"~w\"~n", [svgFile]) ELSE skip END;

            FPRINTF(outputAnimationFile, "~w~n", ["SETS\n    RS_trains"]);
            FPRINTF(outputAnimationFile, "~w~n", ['''VARIABLES
    IS_next /*@desc "relation containing the currently passable track" */,
    IS_crossing_states /*@desc "current states of movable crossings" */,
    IS_crossingsInMovement /*@desc "movable crossings that are currently in movement" */,
    IS_crossing_keyLocked /*@desc "key locking state of movable crossings" */,
    IS_derailer_states /*@desc "current states of derailers" */,
    IS_derailersInMovement /*@desc "derailers that are currently in movement" */,
    IS_derailer_keyLocked /*@desc "key locking state of derailers" */,
    IS_switch_states /*@desc "current states of switches" */,
    IS_switchesInMovement /*@desc "switches that are currently in movement" */,
    IS_switch_keyLocked /*@desc "key locking state of switches" */,

    RS_requestingArrivalTrains /*@desc "arriving trains together with the location of the corresponding open end" */,
    RS_arrivedTrains /*@desc "trains that have been arrived and are currently controlled by the interlocking" */,
    RS_trainOccupiedLocations /*@desc "set of occupied locations without directions per train" */,
    RS_trainFront /*@desc "current front location of each train" */,
    RS_trainBack /*@desc "current back location of each train" */,

    IL_occupiedTvdSections /*@desc "IDs of occupied TVD sections together with the occupying train" */, 
    IL_routes_in_res /*@desc "IDs of routes that are currently in reservation together with the reserving train" */,
    IL_res_routes /*@desc "IDs of reserved routes together with the reserving train" */,
    IL_res_route_blocks /*@desc "blocks (locations) that are currently reserved by routes together with the reserving route" */,
    IL_res_blocks /*@desc "set of blocks (locations) that are currently reserved by routes" */,
    IL_released_partialRoutes /*@desc "IDs of released routeReleaseGroups per route" */,
    IL_res_overlaps /*@desc "IDs of reserved overlaps together with the reserving train" */,
    IL_overlaps_in_release /*@desc "IDs of reserved overlaps that are currently in release process" */,
    IL_signal_states /*@desc "current states of signals (i.e. their aspects)" */,
    IL_noted_signal_states /*@desc "noted states for change of signals that are applied when the corresponding signal plan is activated" */,
    IL_crossing_locked_routes /*@desc "IDs of movable crossings locked by a route together with the route ID" */,
    IL_derailer_locked_routes /*@desc "IDs of derailers locked by a route together with the route ID" */,
    IL_switch_locked_routes /*@desc "IDs of switches locked by a route together with the route ID" */,
    IL_signal_locked /*@desc "IDs of signals locked by a signal plan" */,
    IL_signalplan_in_activation /*@desc "set of signal plans in activation" */
INVARIANT
    IS_next : ((allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION) >+> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)) /*@desc "each movable element should have a unique passable branch - IS_next should be a partial injection" */
    & IS_next <: RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS 
    & dom(dom(dom(IS_next) \/ ran(IS_next))) = allIdsOfType("netElement") /\ RailML3_IS_NET_RESOURCES_MICRO_LEVEL

    & IS_crossingsInMovement : RailML3_IS_MOVABLE_CROSSING_IDS +-> (allIdsOfType("movableCrossing") * RailML3_IL_CROSSING_POSITIONS) /*@desc "Stores destination positions" */
    & !(cr, branch).(cr : dom(IS_crossingsInMovement) & branch : RailML3_IS_CROSSING_BRANCHES(cr) => branch /: IS_next) /*@desc "crossings in movement are not part of IS_next" */
    & IS_crossing_states : RailML3_IS_MOVABLE_CROSSING_IDS --> (allIdsOfType("movableCrossing") * RailML3_IL_CROSSING_POSITIONS)
    & !(cr).(cr : RailML3_IS_MOVABLE_CROSSING_IDS => IS_crossing_states(cr) : dom(RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)))
    & !(cr).(cr : RailML3_IS_MOVABLE_CROSSING_IDS - dom(IS_crossingsInMovement) => RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)) <: IS_next) /*@desc "actual crossing positions are part of IS_next" */
    & !(cr, branch).(cr : RailML3_IS_MOVABLE_CROSSING_IDS & cr /: dom(IS_crossingsInMovement) & branch : RailML3_IS_CROSSING_BRANCHES(cr) & branch /: (RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(cr)(IS_crossing_states(cr)))) => branch /: IS_next) /*@desc "other crossing positions are not part of IS_next" */
    & IS_crossing_keyLocked : RailML3_IS_MOVABLE_CROSSING_IDS --> BOOL

    & IS_derailersInMovement : allIdsOfType("derailerIS") +-> (allIdsOfType("derailerIL") * RailML3_IL_DERAILER_POSITIONS) /*@desc "Stores destination positions" */
    & !(der, pos).(der : dom(IS_derailersInMovement) & pos : RailML3_IS_SPOT_LOCATIONS(der) => pos /: dom(IS_next) \/ ran(IS_next)) /*@desc "derailers in movement are not part of IS_next" */
    & IS_derailer_states : allIdsOfType("derailerIS") --> (allIdsOfType("derailerIL") * RailML3_IL_DERAILER_POSITIONS)
    & !(der, pos).(der : allIdsOfType("derailerIS") & der /: dom(IS_derailersInMovement) & prj2(IS_derailer_states(der)) = position_derailingPosition & pos : RailML3_IS_SPOT_LOCATIONS(der) => pos /: dom(IS_next) \/ ran(IS_next))
    & IS_derailer_keyLocked : allIdsOfType("derailerIS") --> BOOL

    & IS_switchesInMovement : RailML3_IS_SWITCH_IDS +-> (allIdsOfType("switchIL") +-> RailML3_IL_SWITCH_POSITIONS) /*@desc "Stores destination positions" */
    & !(sw, branch).(sw : dom(IS_switchesInMovement) & branch : RailML3_IS_SWITCH_BRANCHES(sw) => branch /: IS_next) /*@desc "switchs in movement are not part of IS_next" */
    & IS_switch_states : RailML3_IS_SWITCH_IDS --> (allIdsOfType("switchIL") +-> RailML3_IL_SWITCH_POSITIONS)
    & !(sw).(sw : RailML3_IS_SWITCH_IDS => IS_switch_states(sw) : dom(RailML3_IL_SWITCH_BRANCHES(sw)))
    & !(i_sw, p1, p2).({p1,p2} <: dom(IS_switch_states(i_sw)) & p1 = p2 => IS_switch_states(i_sw)(p1) = IS_switch_states(i_sw)(p2)) /*@desc "IL switchs have the same state" */
    & !(sw).(sw : RailML3_IS_SWITCH_IDS - dom(IS_switchesInMovement) => RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)) <: IS_next) /*@desc "actual switch positions are part of IS_next" */
    & !(sw, branch).(sw : RailML3_IS_SWITCH_IDS & sw /: dom(IS_switchesInMovement) & branch : RailML3_IS_SWITCH_BRANCHES(sw) & branch /: (RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(sw)(IS_switch_states(sw)))) => branch /: IS_next) /*@desc "other switch positions are not part of IS_next" */
    & IS_switch_keyLocked : RailML3_IS_SWITCH_IDS --> BOOL

    & RS_requestingArrivalTrains : (RS_trains - RS_arrivedTrains) +-> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)
    & RS_arrivedTrains <: RS_trains
    & RS_arrivedTrains /\ dom(RS_requestingArrivalTrains) = {} /*@desc "arrived and requesting arrival trains are disjoint" */
    & RS_trainOccupiedLocations : RS_arrivedTrains --> FIN(allIdsOfType("netElement") * REAL)
    & !(t1,t2).({t1,t2} <: RS_arrivedTrains & t1 /= t2 => RS_trainOccupiedLocations(t1) /\ RS_trainOccupiedLocations(t2) = {}) /*@desc "no collision of trains" */
    & RS_trainFront : RS_arrivedTrains --> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)
    & RS_trainBack : RS_arrivedTrains --> (allIdsOfType("netElement") * REAL * RailML3_IS_DIRECTION)
    & !t.(t : RS_arrivedTrains => RS_trainBack(t) |-> RS_trainFront(t) : closure1(IS_next)) /*@desc "train not broken in two parts (which also covers that not on a element in movement), back behind front, and back /= front" */

    & IL_occupiedTvdSections : allIdsOfType("tvdSection") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & !tvd.(tvd : dom(IL_occupiedTvdSections) & IL_occupiedTvdSections(tvd) : RS_arrivedTrains
        => #b.(b : union(ran(RS_trainOccupiedLocations)) * {direction_normal, direction_reverse} & b : dom(RailML3_IL_TVD_SECTIONS(tvd)) \/ ran(RailML3_IL_TVD_SECTIONS(tvd)) )) /*@desc "on each occupied tvd section is a train" */
    & IL_routes_in_res : allIdsOfType("route") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & IL_res_routes : allIdsOfType("route") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & IL_routes_in_res /\ IL_res_routes = {}
    & !(r1,r2).({r1,r2} <: dom(IL_routes_in_res \/ IL_res_routes) & r1 /= r2
        => dom(IL_res_route_blocks~[{r1}] - {prj2(RailML3_IL_ROUTE_ENTRY(r1))}) /\ dom(IL_res_route_blocks~[{r2}] - {prj2(RailML3_IL_ROUTE_ENTRY(r2))} ) = {}
    ) /*@desc "all reserved routes are disjoint" */
    & !(r,t).(r |-> t : IL_routes_in_res \/ IL_res_routes => not(#t_conflict.(t_conflict : RS_arrivedTrains - {t} & (dom(IL_res_route_blocks~[{r}]) /*- {prj1(prj2(RailML3_IL_ROUTE_ENTRY(r)))}*/) /\ RS_trainOccupiedLocations(t_conflict) /= {}))) /*@desc "for a reserved route no other train is on the route" */

    & IL_res_overlaps : allIdsOfType("overlap") +-> RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)
    & !o.(o : dom(IL_res_overlaps) => #r.(r : dom(IL_routes_in_res \/ IL_res_routes) & o : union(RailML3_IL_ROUTE_OVERLAPS[{r}]))) /*@desc "for each reserved overlap exists a corresponding reserved route" */
    & IL_overlaps_in_release <: dom(IL_res_overlaps)
    
    & IL_released_partialRoutes : dom(IL_res_routes) --> FIN(allIdsOfType("routeReleaseGroupAhead") \/ allIdsOfType("routeReleaseGroupRear"))
    & UNION(r, rg).(r : dom(IL_res_routes) & rg : IL_released_partialRoutes(r) /\ allIdsOfType("routeReleaseGroupRear") | RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r) ) <: RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS

    // for route interlocking (inspired by Abrial's train system model)
    & IL_res_route_blocks : IL_res_blocks <<-> dom(IL_routes_in_res \/ IL_res_routes) /*@desc "each reserved block corresponds to a reserved route; inverse must not hold: a route can be partially completely released only missing the final complete release" */
    & !res_bl.(res_bl : IL_res_blocks /\ ran(ran(RailML3_IL_ROUTE_ENTRY \/ RailML3_IL_ROUTE_EXIT)) => card(IL_res_route_blocks[{res_bl}]) : 0..2 ) /*@desc "route entries/exits can be reserved by at most two routes" */
    & !res_bl.(res_bl : IL_res_blocks - ran(ran(RailML3_IL_ROUTE_ENTRY \/ RailML3_IL_ROUTE_EXIT)) => card(IL_res_route_blocks[{res_bl}]) : 0..1 ) /*@desc "other blocks can be reserved by at most one route" */
    & IL_res_route_blocks <: RailML3_IL_ROUTE_BLOCKS
    & !(r,res_bl).(r : dom(IL_routes_in_res) & res_bl : IL_res_route_blocks~[{r}] - {prj2(RailML3_IL_ROUTE_ENTRY(r)), prj2(RailML3_IL_ROUTE_EXIT(r))} => prj1(res_bl) /: union(ran(RS_trainOccupiedLocations)) ) /*@desc "no blocks of a route in reservation are occupied" */
    & !(r).(r : allIdsOfType("route") => RailML3_IL_ROUTE_NXT(r)[(IL_res_route_blocks~)[{r}]] <: (IL_res_route_blocks~)[{r}]) /*@desc "the successors of all reserved blocks of a route are also reserved by the same route" */
    & !(r,rg).(r : dom(IL_res_routes) & rg : IL_released_partialRoutes(r) => ((dom(RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(r) \/ RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r)) \/ ran(RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(r) \/ RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(r))) <| RailML3_IL_ROUTE_BLOCKS) |> {r} = IL_res_route_blocks |> {r}) /*@desc "all route blocks of a reserved route (except those released by partial route releases) are part of the reserved route blocks" */
    & !(r).(r : dom(IL_res_routes) => (IL_res_route_blocks~)[{r}] <| RailML3_IL_ROUTE_NXT(r) = ((IL_res_route_blocks~)[{r}] - {prj2(RailML3_IL_ROUTE_EXIT(r))}) <| IS_next ) /*@desc "the route path from all reserved blocks is passable, i.e. in IS_next" */
    // end route interlocking
    
    & IL_crossing_locked_routes : RailML3_IS_MOVABLE_CROSSING_IDS <-> allIdsOfType("route")
    & !cr.(cr : dom(IL_crossing_locked_routes) => cr /: dom(IS_crossingsInMovement)) /*@desc "locked crossings should never be in movement" */
    & IL_derailer_locked_routes  : allIdsOfType("derailerIS") <-> allIdsOfType("route")
    & !der.(der : dom(IL_derailer_locked_routes) => der /: dom(IS_derailersInMovement)) /*@desc "locked derailers should never be in movement" */
    & IL_switch_locked_routes  : RailML3_IS_SWITCH_IDS <-> allIdsOfType("route")
    & !sw.(sw : dom(IL_switch_locked_routes) => sw /: dom(IS_switchesInMovement)) /*@desc "locked switches should never be in movement" */

    & IL_signal_states : RailML3_IL_SIGNAL_CONTROLLED --> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
    & !sig.(sig : dom(IL_signal_states) & aspect_closed : IL_signal_states(sig) => card(IL_signal_states(sig)) = 1) /*@desc "If aspect_closed is active, no other aspect can be shown" */
    & !sig.(sig : dom(IL_signal_states) & aspect_closed /: IL_signal_states(sig) =>
        #exit.(
            exit : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(sig)
            & RailML3_IL_SIGNAL_CONTROL_SECTIONS(sig)(exit) <: IS_next
            & dom(dom({ nxt, NXT | NXT = RailML3_IL_SIGNAL_CONTROL_SECTIONS(sig)(exit) & NXT <: IS_next & nxt : ran(NXT) })) /\ union(ran(RS_trainOccupiedLocations)) = {}
            & RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(sig)(exit) /\ dom(IL_occupiedTvdSections) = {}
        )
    ) /*@desc "movable elements after not closed signal are passable; (current) block after not closed signal is not occupied; tvdSections after not closed signal are not occupied" */
    & IL_noted_signal_states : RailML3_IL_SIGNAL_CONTROLLED +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
    & !sig.(sig : dom(IL_noted_signal_states) & aspect_closed : IL_noted_signal_states(sig) => card(IL_noted_signal_states(sig)) = 1) /*@desc "If aspect_closed is noted, no other aspect can be shown" */
    & !sig.(sig : dom(IL_noted_signal_states) & aspect_closed /: IL_noted_signal_states(sig) =>
        #exit.(
            exit : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(sig)
            //& RailML3_IL_SIGNAL_CONTROL_SECTIONS(sig)(exit) <: IS_next /*@desc "movable elements after not closed noted signal change are passable" */
            & dom(dom({ nxt, NXT | NXT = RailML3_IL_SIGNAL_CONTROL_SECTIONS(sig)(exit) & NXT <: IS_next & nxt : ran(NXT) })) /\ union(ran(RS_trainOccupiedLocations)) = {} 
            //& union(ran(RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(sig))) /\ dom(IL_occupiedTvdSections) = {} /*@desc "tvdSections after not closed noted signal change are not occupied" */
        )
    ) /*@desc "(current) block after not closed noted signal change is not occupied" */
    & IL_signal_locked : FIN(RailML3_IL_SIGNAL_CONTROLLED)
    & IL_signalplan_in_activation : 
        FIN(struct(
            master: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS),
            slave: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS),
            distant: allIdsOfType("signalIL") +-> FIN(RailML3_IL_SIGNAL_GENERIC_ASPECTS)
        ))
    & !(sp,sig_il,asp).(sp : IL_signalplan_in_activation & sig_il |-> asp : sp'slave \/ sp'distant & aspect_closed /: asp => RailML3_IL_SIGNAL_REFERS_TO(sig_il) : IL_signal_locked) /*@desc "corresponding signalIS is locked when signalplan for signalIL not showing closed aspect is in activation" */

    & !(r,rr).(r : dom(IL_res_routes) /\ dom(RailML3_IL_ROUTE_ADDITIONAL_RELATIONS) & rr : RailML3_IL_ROUTE_ADDITIONAL_RELATIONS(r) =>
        (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS) =>
            union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[{proving_continuously |-> FALSE}]) <: IS_derailer_states
            & union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[{proving_continuously |-> TRUE}]) /\ IS_derailer_states = {}
        )
        & (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS) =>
            union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[{proving_continuously |-> FALSE}]) <: IS_switch_states
            & union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[{proving_continuously |-> TRUE}]) /\ IS_switch_states = {}
        )
        & (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES) =>
            RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> FALSE}]~[{tvd_occupied}] <: dom(IL_occupiedTvdSections)
            & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> FALSE}]~[{tvd_vacant, tvd_unknown}] /\ dom(IL_occupiedTvdSections) = {} // it wouldn't be unknown if known to be occupied
            & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> TRUE}]~[{tvd_occupied}] /\ dom(IL_occupiedTvdSections) = {}
            & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> TRUE}]~[{tvd_vacant, tvd_unknown}] <: dom(IL_occupiedTvdSections)
        )
    ) /*@desc "states that must be proven continuously: (FALSE means not negated, i.e. the specified property must hold; TRUE means negated, i.e. any other property than the specified one)" */ 
    // detection states currently not implemented
        
    & !o.(o : dom(IL_res_overlaps) =>
        (o : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS) =>
            (proving_continuously |-> FALSE : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)) =>
                RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)(proving_continuously, FALSE) <: IS_switch_states
            )
            & (proving_continuously |-> TRUE : dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)) =>
                RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)(proving_continuously, TRUE) /\ IS_switch_states = {}
            )
        )
    ) /*@desc "fixed switch positions of overlaps are correct" */

    & !(r,cr).(r : dom(IL_res_routes) & r : dom(RailML3_IL_ROUTE_CROSSING_POSITIONS) & cr : dom(RailML3_IL_ROUTE_CROSSING_POSITIONS(r)) /\ IL_crossing_locked_routes~[{r}] => RailML3_IL_ROUTE_CROSSING_POSITIONS(r)(cr) = IS_crossing_states(cr) & cr /: dom(IS_crossingsInMovement)) /*@desc "forced positions of movableCrossings are fulfilled for all reserved routes" */
    & !(r,der).(r : dom(IL_res_routes) & r : dom(RailML3_IL_ROUTE_DERAILERS) & der : IL_derailer_locked_routes~[{r}] => prj2(IS_derailer_states(der)) = position_passablePosition & der /: dom(IS_derailersInMovement)) /*@desc "derailers are in passable position for all reserved routes" */
    & !(r,sw).(r : dom(IL_res_routes) & r : dom(RailML3_IL_ROUTE_SWITCH_POSITIONS) & sw : dom(RailML3_IL_ROUTE_SWITCH_POSITIONS(r)) /\ IL_switch_locked_routes~[{r}] => RailML3_IL_ROUTE_SWITCH_POSITIONS(r)(sw) <: IS_switch_states(sw) & sw /: dom(IS_switchesInMovement)) /*@desc "forced positions of switches are fulfilled for all routes" */
INITIALISATION
    IS_crossing_states :=
        { i_cr, cr | #i_cr_il.(i_cr_il : allIdsOfType("movableCrossing") & RailML3_IL_MOVABLE_CROSSING_REFERS_TO(i_cr_il) = i_cr & cr = i_cr_il |-> IF i_cr_il : dom(RailML3_IL_MOVABLE_CROSSING_PREFERRED_POSITION) THEN RailML3_IL_MOVABLE_CROSSING_PREFERRED_POSITION(i_cr_il) ELSE `position_upleft-rightdown` END) } ||
    IS_derailer_states :=
        { i_der, der | #i_der_il.(i_der_il : allIdsOfType("derailerIL") & RailML3_IL_DERAILER_REFERS_TO(i_der_il) = i_der & der = i_der_il |-> IF i_der_il : dom(RailML3_IL_DERAILER_PREFERRED_POSITION) THEN RailML3_IL_DERAILER_PREFERRED_POSITION(i_der_il) ELSE position_derailingPosition END) } ||
    IS_switch_states :=
        %i_sw.( i_sw : RailML3_IS_SWITCH_IDS |
            { i_sw_il, sw | i_sw_il : allIdsOfType("switchIL") & RailML3_IL_SWITCH_REFERS_TO(i_sw_il) = i_sw & sw = IF i_sw_il : dom(RailML3_IL_SWITCH_PREFERRED_POSITION) THEN RailML3_IL_SWITCH_PREFERRED_POSITION(i_sw_il) ELSE position_left END }
        );
    
    IS_next := RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS
        - ({ np | #(i_cr,currState,otherState).(IS_crossing_states(i_cr) = currState & otherState /= currState & np : RailML3_IL_MOVABLE_CROSSING_BRANCHES(i_cr)(otherState) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(i_cr)(otherState)) ) }
            \/
            dom({ np, i_der | prj2(IS_derailer_states(i_der)) = position_derailingPosition & np : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) })
            \/
            { np | #(i_sw,currState,otherState).(IS_switch_states(i_sw) = currState & otherState /= currState & np : RailML3_IL_SWITCH_BRANCHES(i_sw)(otherState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(i_sw)(otherState)) ) }
        );
    IS_crossingsInMovement, IS_derailersInMovement, IS_switchesInMovement := {}, {}, {} ||
    IS_crossing_keyLocked := RailML3_IL_MOVABLE_CROSSING_IS_KEY_LOCKED ||
    IS_derailer_keyLocked := RailML3_IL_DERAILER_IS_KEY_LOCKED ||
    IS_switch_keyLocked := RailML3_IL_SWITCH_IS_KEY_LOCKED ||

    RS_requestingArrivalTrains := {};
    RS_arrivedTrains := dom(RS_trainFront_init);
    RS_trainOccupiedLocations := fnc({ train, block | train : RS_arrivedTrains & #ssB.(ssB = ignoreDirection(railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1~[{RS_trainFront_init(train)}] <| RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS |> railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[{RS_trainBack_init(train)}]) & block : dom(ssB) \/ ran(ssB)) }) ||
    RS_trainFront, RS_trainBack := RS_trainFront_init, RS_trainBack_init;

    IL_occupiedTvdSections := { i_tvd, train | train : RS_arrivedTrains & #block.(block : RS_trainOccupiedLocations(train) * {direction_normal, direction_reverse} & block : dom(RailML3_IL_TVD_SECTIONS(i_tvd)) \/ ran(RailML3_IL_TVD_SECTIONS(i_tvd))) } ||
    IL_routes_in_res, IL_res_routes, IL_res_blocks, IL_res_route_blocks, IL_released_partialRoutes := {}, {}, {}, {}, {} ||
    IL_res_overlaps, IL_overlaps_in_release := {}, {} ||
    IL_crossing_locked_routes := {} ||
    IL_derailer_locked_routes := {} ||
    IL_switch_locked_routes := {} ||
    IL_signal_states, IL_noted_signal_states := RailML3_IL_SIGNAL_CONTROLLED * {{aspect_closed}}, {} ||
    IL_signal_locked,IL_signalplan_in_activation := {}, {}
OPERATIONS
    IS_startChangeDerailer(DerailerId, DestState) =
        ANY
            signals
        WHERE
            DerailerId : dom(IS_derailer_states)
            & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : dom(RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId)))
                or #route.(route : dom(IL_routes_in_res) /\ dom(RailML3_IL_ROUTE_DERAILERS) & DerailerId : RailML3_IL_ROUTE_DERAILERS(route) )
                or #ovl.(ovl : dom(IL_res_overlaps) /\ dom(RailML3_IL_OVERLAP_DERAILERS) & DerailerId : RailML3_IL_OVERLAP_DERAILERS(ovl) )
                or #SignalId.(SignalId : { i_sig | i_sig : RailML3_IL_SIGNAL_CONTROLLED & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(i_sig))
                        or #sp.(sp : IL_signalplan_in_activation & i_sig : RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master)] ) ) }
                    & DerailerId /: union(RailML3_IL_ROUTE_DERAILERS[dom(IL_routes_in_res \/ IL_res_routes)]) \/ union(RailML3_IL_OVERLAP_DERAILERS[dom(IL_res_overlaps)])
                    & union(ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId))) /\ RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId) /= {})
            ) /*@desc "a derailer can only change state if: train stands directly in front of it, it is part of a route/overlap in reservation, it is part of a signal control section in front of whose associated signal the train is directly positioned or the corresponding signal is part of a signal plan in activation" */
            & IS_derailer_keyLocked(DerailerId) = FALSE
            & DerailerId /: dom(IL_derailer_locked_routes)
            & (DerailerId : dom(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT) =>
                RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId) /: dom(IS_derailersInMovement) \/ dom(IS_crossingsInMovement) \/ dom(IS_switchesInMovement)
                & (RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId) : RailML3_IS_SWITCH_IDS =>
                    !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId)}] & i_sw_il |-> IS_switch_states(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId))(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                        => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> IS_switch_states(RailML3_IL_DERAILER_RELATED_MOVABLE_ELEMENT(DerailerId))(i_sw_il))'derailer(RailML3_IL_DERAILER_REFERS_TO~(DerailerId)) /= prj2(IS_derailer_states(DerailerId)) )
                )
            ) /*@desc "the related movable element is not moving and, if it is a switch, possible position restrictions are fulfilled" */
            & DestState : {prj1(IS_derailer_states(DerailerId))} * RailML3_IL_DERAILER_POSITIONS
            & IS_derailer_states(DerailerId) /= DestState
            & DerailerId /: dom(IS_derailersInMovement) /*@desc "the derailer is not in movement" */
            & !sloc.(sloc : dom(RailML3_IS_SPOT_LOCATIONS(DerailerId)) => sloc /: union(ran(RS_trainOccupiedLocations))) /*@desc "the location of the derailer is not occupied by a train" */
            & !tvd.(tvd : union(RailML3_IL_DERAILER_HAS_TVD_SECTION[{DerailerId}]) => tvd /: dom(IL_occupiedTvdSections)) /*@desc "the TVD sections of the derailer are not occupied by a train" */
            & signals = { signal, path | #section.(
                section : ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(signal))
                & section /\ RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId) /= {}
                & path = railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[ran(RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId))] <<| section
                & !b.(b : path => b : IS_next)
            )} /*@desc "set containing all signals controlling the sections of the derailer" */
            & !path.(path : ran(signals) => dom(dom(path)) /\ union(ran(RS_trainOccupiedLocations)) = {} ) /*@desc "if part of signal section: the derailer is only controllable if the section is free for the path of the current position" */
        THEN
            IS_derailersInMovement := IS_derailersInMovement \/ {DerailerId |-> DestState} ||
            IF prj2(DestState) = position_derailingPosition THEN
                IS_next := IS_next - RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId) ||
                IL_signal_states := IL_signal_states <+ (dom(signals) * {{aspect_closed}}) ||
                IL_noted_signal_states := dom(signals) <<| IL_noted_signal_states
            ELSE skip END
        END;
    IS_endChangeDerailer(DerailerId) =
        ANY
            destState
        WHERE
            DerailerId : dom(IS_derailersInMovement) /*@desc "the derailer is in movement" */
            & destState = IS_derailersInMovement(DerailerId)
        THEN
            IS_derailer_states(DerailerId) := destState ||
            IS_derailersInMovement := IS_derailersInMovement - {DerailerId |-> destState} ||
            IF prj2(destState) = position_passablePosition THEN
                IS_next := IS_next \/ RailML3_IS_DERAILER_NOT_PASSABLE(DerailerId)
            ELSE skip END
        END;
    IS_startChangeCrossing(CrossingId, CurrState, DestState) =
        ANY
            signals
        WHERE
            CrossingId : dom(IS_crossing_states)
            & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : dom(RailML3_IS_CROSSING_BRANCHES(CrossingId)))
                or #route.(route : dom(IL_routes_in_res) /\ dom(RailML3_IL_ROUTE_CROSSING_POSITIONS) & CrossingId : dom(RailML3_IL_ROUTE_CROSSING_POSITIONS(route)) & IS_crossing_states(CrossingId) /= RailML3_IL_ROUTE_CROSSING_POSITIONS(route)(CrossingId))
                or #ovl.(ovl : dom(IL_res_overlaps) /\ dom(RailML3_IL_OVERLAP_CROSSING_POSITIONS) & CrossingId : dom(RailML3_IL_OVERLAP_CROSSING_POSITIONS(ovl)) & IS_crossing_states(CrossingId) /= RailML3_IL_OVERLAP_CROSSING_POSITIONS(ovl)(CrossingId))
                or #SignalId.(SignalId : { i_sig | i_sig : RailML3_IL_SIGNAL_CONTROLLED & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(i_sig))
                        or #sp.(sp : IL_signalplan_in_activation & i_sig : RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master)] ) ) }
                    & CrossingId /: dom(union(RailML3_IL_ROUTE_CROSSING_POSITIONS[dom(IL_routes_in_res \/ IL_res_routes)]) \/ union(RailML3_IL_OVERLAP_CROSSING_POSITIONS[dom(IL_res_overlaps)]))
                    & union(ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId))) /\ RailML3_IS_CROSSING_BRANCHES(CrossingId) /= {})
            ) /*@desc "a crossing can only change state if: train stands directly in front of it, it is part of a route/overlap in reservation, it is part of a signal control section in front of whose associated signal the train is directly positioned or the corresponding signal is part of a signal plan in activation" */
            & IS_crossing_keyLocked(CrossingId) = FALSE
            & CrossingId /: dom(IL_crossing_locked_routes)
            & CurrState = IS_crossing_states(CrossingId)
            & DestState : {prj1(IS_crossing_states(CrossingId))} * RailML3_IL_CROSSING_POSITIONS
            & CurrState /= DestState
            & CrossingId /: dom(IS_crossingsInMovement) /*@desc "the crossing is not in movement" */
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_CROSSING_BRANCHES(CrossingId)
                => prj1(prj1(branch)) /: RS_trainOccupiedLocations(train) or prj1(prj2(branch)) /: RS_trainOccupiedLocations(train)  ) /*@desc "the crossing is not occupied by a train" */
            & !tvd.(tvd : union(RailML3_IL_MOVABLE_CROSSING_HAS_TVD_SECTION[{CrossingId}]) => tvd /: dom(IL_occupiedTvdSections)) /*@desc "the TVD sections of the crossing are not occupied by a train" */
            & signals = { signal, path | #section.(
                section : ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(signal))
                & section /\ RailML3_IS_CROSSING_BRANCHES(CrossingId) /= {}
                & path = railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[ran(RailML3_IS_CROSSING_BRANCHES(CrossingId))] <<| section
                & !b.(b : path => b : IS_next)
            )} /*@desc "set containing all signals controlling the sections of the crossing" */
            & !path.(path : ran(signals) => dom(dom(path)) /\ union(ran(RS_trainOccupiedLocations)) = {} ) /*@desc "if part of signal section: the crossing is only controllable if the section is free for the path of the current position" */
        THEN
            IS_crossingsInMovement := IS_crossingsInMovement \/ {CrossingId |-> DestState} ||
            IS_next := IS_next - (RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(CurrState) \/ inverseDirectedRelation(RailML3_IL_MOVABLE_CROSSING_BRANCHES(CrossingId)(CurrState))) ||
            IL_signal_states := IL_signal_states <+ (dom(signals) * {{aspect_closed}}) ||
            IL_noted_signal_states := dom(signals) <<| IL_noted_signal_states
        END;
    IS_endChangeCrossing(CrossingId) =
        ANY
            destState
        WHERE
            CrossingId : dom(IS_crossingsInMovement) /*@desc "the crossing is in movement" */
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
            & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : dom(RailML3_IS_SWITCH_BRANCHES(SwitchId)))
                or #route.(route : dom(IL_routes_in_res) /\ dom(RailML3_IL_ROUTE_SWITCH_POSITIONS) & SwitchId : dom(RailML3_IL_ROUTE_SWITCH_POSITIONS(route)) & IS_switch_states(SwitchId) /= RailML3_IL_ROUTE_SWITCH_POSITIONS(route)(SwitchId) )
                or #ovl.(ovl : dom(IL_res_overlaps) /\ dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS) & SwitchId : dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)) & IS_switch_states(SwitchId) /= RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)(SwitchId) )
                or #SignalId.(SignalId : { i_sig | i_sig : RailML3_IL_SIGNAL_NOT_CONTROLLED_BY_SIGNALPLAN & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(i_sig))
                        or #sp.(sp : IL_signalplan_in_activation & i_sig : RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master)] ) ) }
                    & SwitchId /: dom(union(RailML3_IL_ROUTE_SWITCH_POSITIONS[dom(IL_routes_in_res \/ IL_res_routes)]) \/ union(RailML3_IL_OVERLAP_SWITCH_POSITIONS[dom(IL_res_overlaps)]))
                    & union(ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId))) /\ RailML3_IS_SWITCH_BRANCHES(SwitchId) /= {})
            ) /*@desc "a switch can only change state if: train stands directly in front of it, it is part of a route/overlap in reservation, it is part of a signal control section in front of whose associated signal the train is directly positioned or the corresponding signal is part of a signal plan in activation" */
            & (SwitchId : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) \/ ran(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) =>
                {switch_switchCrossingPart, switch_singleSwitchCrossing, switch_doubleSwitchCrossing} /\ RailML3_IS_SWITCH_TYPE[{SwitchId}] /= {} 
                or
                ( SwitchId : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT) & RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT(SwitchId) : union(allIdsOfType[{"crossing","derailerIS"}]) )
            ) /*@desc "the related movable element is of of type crossing, derailer or switch crossing" */
            & IS_switch_keyLocked(SwitchId) = FALSE
            & SwitchId /: dom(IL_switch_locked_routes)
            & CurrState = IS_switch_states(SwitchId)
            & DestState : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId))
            & CurrState /= DestState
            & SwitchId /: dom(IS_switchesInMovement) /*@desc "the switch is not in movement" */
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId}] & i_sw_il |-> DestState(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState(i_sw_il))'switch) /\ dom(union(ran(IS_switchesInMovement))) = {}
            ) /*@desc "position restrictions for new state are fulfilled" */
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId)
                => prj1(prj1(branch)) /: RS_trainOccupiedLocations(train) or prj1(prj2(branch)) /: RS_trainOccupiedLocations(train) ) /*@desc "the switch is not occupied by a train" */
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId}]) => tvd /: dom(IL_occupiedTvdSections)) /*@desc "the TVD sections of the switch are not occupied by a train" */
            & signals = { signal, path | #section.(
                section : ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(signal))
                & section /\ RailML3_IS_SWITCH_BRANCHES(SwitchId) /= {}
                & path = railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[ran(RailML3_IS_SWITCH_BRANCHES(SwitchId))] <<| section
                & !b.(b : path => b : IS_next)
            )} /*@desc "set containing all signals controlling the sections of the switch" */
            & !path.(path : ran(signals) => dom(dom(path)) /\ union(ran(RS_trainOccupiedLocations)) = {} ) /*@desc "if part of a signal section: the switch can only be controlled if the section up to the switch is free for the path of the current position" */
        THEN
            IS_switchesInMovement := IS_switchesInMovement \/ {SwitchId |-> DestState} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId)(CurrState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId)(CurrState))) ||
            IL_signal_states := IL_signal_states <+ (dom(signals) * {{aspect_closed}}) ||
            IL_noted_signal_states := dom(signals) <<| IL_noted_signal_states
        END;
    IS_endChangeSwitch(SwitchId) =
        ANY
            destState
        WHERE
            SwitchId : dom(IS_switchesInMovement) /*@desc "the switch is in movement" */
            & destState = IS_switchesInMovement(SwitchId)
        THEN
            IS_switch_states(SwitchId) := destState ||
            IS_switchesInMovement := IS_switchesInMovement - {SwitchId |-> destState} ||
            IS_next := IS_next \/ (RailML3_IL_SWITCH_BRANCHES(SwitchId)(destState) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId)(destState)))
        END;
    // COUPLED SWITCHES (here only start operation is needed - end is performed individually)
    IS_startChangeCoupledSwitches(SwitchId1, CurrState1, DestState1, SwitchId2, CurrState2, DestState2) =
        ANY
            signals
        WHERE
            SwitchId1 : dom(RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT)
            & SwitchId2 = RailML3_IL_SWITCH_RELATED_MOVABLE_ELEMENT(SwitchId1)
            & {switch_switchCrossingPart, switch_singleSwitchCrossing, switch_doubleSwitchCrossing} /\ RailML3_IS_SWITCH_TYPE[{SwitchId1,SwitchId2}] = {} /*@desc "switches are not forming a switch crossing - switch crossings are controlles by IS_startChangeSwitch" */
        // Switch1 Guards:
            & SwitchId1 : dom(IS_switch_states)
            & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : dom(RailML3_IS_SWITCH_BRANCHES(SwitchId1)))
                or #route.(route : dom(IL_routes_in_res) /\ dom(RailML3_IL_ROUTE_SWITCH_POSITIONS) & SwitchId1 : dom(RailML3_IL_ROUTE_SWITCH_POSITIONS(route)) & IS_switch_states(SwitchId1) /= RailML3_IL_ROUTE_SWITCH_POSITIONS(route)(SwitchId1) )
                or #ovl.(ovl : dom(IL_res_overlaps) /\ dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS) & SwitchId1 : dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)) & IS_switch_states(SwitchId1) /= RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)(SwitchId1) )
                or #SignalId.(SignalId : { i_sig | i_sig : RailML3_IL_SIGNAL_NOT_CONTROLLED_BY_SIGNALPLAN & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(i_sig))
                        or #sp.(sp : IL_signalplan_in_activation & i_sig : RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master)] ) ) }
                    & union(ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId))) /\ RailML3_IS_SWITCH_BRANCHES(SwitchId1) /= {})
            ) /*@desc "first switch can only change state if: train stands directly in front of it, it is part of a route/overlap in reservation, it is part of a signal control section in front of whose associated signal the train is directly positioned or the corresponding signal is part of a signal plan in activation" */
            & IS_switch_keyLocked(SwitchId1) = FALSE
            & SwitchId1 /: dom(IL_switch_locked_routes)
            & CurrState1 = IS_switch_states(SwitchId1)
            & DestState1 : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId1))
            & CurrState1 /= DestState1
            & SwitchId1 /: dom(IS_switchesInMovement) /*@desc "first switch is not in movement" */
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId1}] & i_sw_il |-> DestState1(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState1(i_sw_il))'switch) /\ dom(union(ran(IS_switchesInMovement))) = {}
            ) /*@desc "position restrictions for new state of first switch are fulfilled" */
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId1)
                => prj1(prj1(branch)) /: RS_trainOccupiedLocations(train) or prj1(prj2(branch)) /: RS_trainOccupiedLocations(train) )  /*@desc "the first switch is not occupied by a train" */
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId1}]) => tvd /: dom(IL_occupiedTvdSections)) /*@desc "the TVD sections of the first switch are not occupied by a train" */
        // Switch2 Guards:
            & SwitchId2 : dom(IS_switch_states)
            & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : dom(RailML3_IS_SWITCH_BRANCHES(SwitchId2)))
                or #route.(route : dom(IL_routes_in_res) /\ dom(RailML3_IL_ROUTE_SWITCH_POSITIONS) & SwitchId2 : dom(RailML3_IL_ROUTE_SWITCH_POSITIONS(route)) & IS_switch_states(SwitchId2) /= RailML3_IL_ROUTE_SWITCH_POSITIONS(route)(SwitchId2) )
                or #ovl.(ovl : dom(IL_res_overlaps) /\ dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS) & SwitchId2 : dom(RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)) & IS_switch_states(SwitchId2) /= RailML3_IL_OVERLAP_SWITCH_POSITIONS(ovl)(SwitchId2) )
                or #SignalId.(SignalId : { i_sig | i_sig : RailML3_IL_SIGNAL_NOT_CONTROLLED_BY_SIGNALPLAN & ( #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(i_sig))
                        or #sp.(sp : IL_signalplan_in_activation & i_sig : RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master)] ) ) }
                    & union(ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId))) /\ RailML3_IS_SWITCH_BRANCHES(SwitchId2) /= {})
            ) /*@desc "second switch can only change state if: train stands directly in front of it, it is part of a route/overlap in reservation, it is part of a signal control section in front of whose associated signal the train is directly positioned or the corresponding signal is part of a signal plan in activation" */
            & IS_switch_keyLocked(SwitchId2) = FALSE
            & SwitchId2 /: dom(IL_switch_locked_routes)
            & CurrState2 = IS_switch_states(SwitchId2)
            & DestState2 : dom(RailML3_IL_SWITCH_BRANCHES(SwitchId2))
            & CurrState2 /= DestState2
            & SwitchId2 /: dom(IS_switchesInMovement) /*@desc "second switch is not in movement" */
            & !i_sw_il.(i_sw_il : RailML3_IL_SWITCH_REFERS_TO~[{SwitchId2}] & i_sw_il |-> DestState2(i_sw_il) : dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS)
                => RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'derailer <: ran(IS_derailer_states)
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'derailer) /\ dom(ran(IS_derailersInMovement)) = {}
                & RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'switch <: union(ran(IS_switch_states))
                & dom(RailML3_IL_SWITCH_POSITION_RESTRICTIONS(i_sw_il |-> DestState2(i_sw_il))'switch) /\ dom(union(ran(IS_switchesInMovement))) = {}
            ) /*@desc "position restrictions for new state of second switch are fulfilled" */
            & !(train, branch).(train : RS_arrivedTrains & branch : RailML3_IS_SWITCH_BRANCHES(SwitchId2)
                => prj1(prj1(branch)) /: RS_trainOccupiedLocations(train) or prj1(prj2(branch)) /: RS_trainOccupiedLocations(train) ) /*@desc "the second switch is not occupied by a train" */
            & !tvd.(tvd : union(RailML3_IL_SWITCH_HAS_TVD_SECTION[{SwitchId2}]) => tvd /: dom(IL_occupiedTvdSections)) /*@desc "the TVD sections of the second switch are not occupied by a train" */
        // Signals:
            & signals = { signal, path | #section.(
                section : ran(RailML3_IL_SIGNAL_CONTROL_SECTIONS(signal))
                & section /\ union(RailML3_IS_SWITCH_BRANCHES[{SwitchId1,SwitchId2}]) /= {}
                & path = railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[ran(union(RailML3_IS_SWITCH_BRANCHES[{SwitchId1,SwitchId2}]))] <<| section
                & !b.(b : path => b : IS_next)
            )} /*@desc "set containing all signals controlling the sections of the switches" */
            & !path.(path : ran(signals) => dom(dom(path)) /\ union(ran(RS_trainOccupiedLocations)) = {} ) /*@desc "if part of signal section: the switches are only controllable if the section is free for the path of the current positions" */
        THEN
            IS_switchesInMovement := IS_switchesInMovement \/ {SwitchId1 |-> DestState1} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId1)(CurrState1) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId1)(CurrState1)));
            IS_switchesInMovement := IS_switchesInMovement \/ {SwitchId2 |-> DestState2} ||
            IS_next := IS_next - (RailML3_IL_SWITCH_BRANCHES(SwitchId2)(CurrState2) \/ inverseDirectedRelation(RailML3_IL_SWITCH_BRANCHES(SwitchId2)(CurrState2))) ||
            IL_signal_states := IL_signal_states <+ (dom(signals) * {{aspect_closed}}) ||
            IL_noted_signal_states := dom(signals) <<| IL_noted_signal_states
        END;

    IL_startRouteReservation(Route, Train) =
        SELECT
            Route : allIdsOfType("route") - dom(IL_routes_in_res \/ IL_res_routes)  /*@desc "the route is currently not reserved or in reservation" */
            & IF Train : dom(RS_requestingArrivalTrains) THEN
                RS_requestingArrivalTrains(Train) = prj2(RailML3_IL_ROUTE_ENTRY(Route))
            ELSE
                Train : RS_arrivedTrains
                & (RS_trainFront(Train) |-> prj2(RailML3_IL_ROUTE_ENTRY(Route)) : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 or RS_trainFront(Train) = prj2(RailML3_IL_ROUTE_ENTRY(Route))) /*@desc "the train is in front of the route entry" */
            END /*@desc "the requesting train is currently in the interlocking area or an arriving train reserves a rout starting at an open end" */             
            & (IF Route : dom(RailML3_IL_ROUTE_ACTIVATION_SECTIONS) THEN
                #actTvd.(actTvd : RailML3_IL_ROUTE_ACTIVATION_SECTIONS(Route) & Train : RS_arrivedTrains /\ IL_occupiedTvdSections[{actTvd}] & RS_trainFront(Train) : (dom(RailML3_IL_TVD_SECTIONS(actTvd)) \/ ran(RailML3_IL_TVD_SECTIONS(actTvd))))
                ELSE
                    Train : RS_arrivedTrains & RS_trainFront(Train) |-> prj2(RailML3_IL_ROUTE_ENTRY(Route)) : RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS /*@desc "train could stand on first (prj1) block" */
                END
                or RailML3_IL_ROUTE_LOCKS_AUTOMATICALLY(Route) = TRUE
                or #(signalplan, aspects, r).( signalplan : IL_signalplan_in_activation & r |-> Train : IL_routes_in_res & RailML3_IL_ROUTE_EXIT(r) = RailML3_IL_ROUTE_ENTRY(Route)
                    & prj1(RailML3_IL_ROUTE_ENTRY(Route)) |-> aspects : signalplan'master & aspect_closed /: aspects )
                or #r.(r |-> Train : IL_res_routes & RailML3_IL_ROUTE_EXIT(r) = RailML3_IL_ROUTE_ENTRY(Route))
            ) /*@desc "route reservation can be started if: the train occupies one of the activation sections, the train stands on the route entry, the route is an route which locks automatically, a signalplan is in activation changeing the route entry signal to a passable aspect, or a route is reserved up to the route entry" */
            & !tvd.(tvd : union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) - IL_occupiedTvdSections~[{Train}] => tvd /: dom(IL_occupiedTvdSections)) /*@desc "all TVD sections of the route are vacant" */
            & !nxt.(nxt : dom(ran(union(RailML3_IL_ROUTE_NXT[{Route}]))) => nxt /: union(ran(RS_trainOccupiedLocations))) /*@desc "locations of the route are not occupied" */
            & !r.(r : union(RailML3_IL_CONFLICTING_ROUTES[{Route}]) => r /: dom(IL_routes_in_res \/ IL_res_routes)) /*@desc "no conflicting route is in reservation or reserved" */       
            & !(r,t).(r |-> t : IL_routes_in_res \/ IL_res_routes & r /= Route & t /= Train =>
                union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) /\ (union(RailML3_IL_ROUTE_TVD_SECTIONS[{r}]) - union(RailML3_IL_ROUTE_ACTIVATION_SECTIONS[{r}])) = {}
            ) /*@desc "the TVD sections (except the activation section) are not reserved by another route (of another train)" */
            & !r.(r : dom(IL_routes_in_res \/ IL_res_routes) & r /= Route =>
                dom(IL_res_route_blocks~[{r}] - {prj2(RailML3_IL_ROUTE_ENTRY(r))}) /\ dom(RailML3_IL_ROUTE_BLOCKS~[{Route}] - {prj2(RailML3_IL_ROUTE_ENTRY(Route))}) = {}
            ) /*@desc "no route block is reserved by another route (dom(.) to ensure both directions are taken into account)" */
            & !o.(o : dom(IL_res_overlaps) & IL_res_overlaps(o) /= Train => RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_ROUTE_NXT(Route) = {} ) /*@desc "no reserved overlap intersects the route" */
            & !(o,o2).(o : union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) & o2 : dom(IL_res_overlaps) & o /= o2 => o /: dom(IL_res_overlaps) & RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_OVERLAP_NXT(o2) = {} & !r.(r : dom(IL_routes_in_res \/ IL_res_routes) & r /= Route => RailML3_IL_OVERLAP_NXT(o) /\ RailML3_IL_ROUTE_NXT(r) = {})) /*@desc "the overlaps to be reserved do not intersect with other reserved overlaps" */
        THEN
            IL_routes_in_res := IL_routes_in_res \/ {Route |-> Train} ||
            IL_res_route_blocks := IL_res_route_blocks \/ (RailML3_IL_ROUTE_BLOCKS |> {Route}) ||
            IL_res_blocks := IL_res_blocks \/ (RailML3_IL_ROUTE_BLOCKS~)[{Route}] ||
            IL_res_overlaps := IL_res_overlaps \/ (union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) * {Train}) ||
            LET signals BE
                signals = { i_sig | i_sig : RailML3_IL_SIGNAL_CONTROLLED & RailML3_IS_SPOT_LOCATIONS(i_sig) /\ RailML3_IL_ROUTE_BLOCKS~[{Route}] /= {} }
            IN
                IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
                IL_noted_signal_states := signals <<| IL_noted_signal_states
            END
        END;
    IL_endRouteReservation(Route, Train) =
        ANY
            r_overlaps, r_relations, r_crossings, r_derailers, r_switches
        WHERE
            Route |-> Train : IL_routes_in_res /*@desc "the route is currently in reservation" */
            & RailML3_IL_ROUTE_NXT(Route) <: IS_next /*@desc "all movable elements are in correct position, i.e. the route path is in IS_next" */
            & r_overlaps = union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            & r_relations = union(RailML3_IL_ROUTE_ADDITIONAL_RELATIONS[{Route}])

            & r_crossings = union(RailML3_IL_ROUTE_CROSSING_POSITIONS[{Route}]) \/ union(RailML3_IL_OVERLAP_CROSSING_POSITIONS[r_overlaps])
            & !i_cr.( i_cr : dom(r_crossings) => r_crossings(i_cr) = IS_crossing_states(i_cr) ) /*@desc "the positions of movable crossings are the expected" */
            & !i_cr_il.(i_cr_il : dom(ran(r_crossings)) => i_cr_il /: dom(ran(IS_crossingsInMovement)) ) /*@desc "no movable crossings on the route are in movement" */

            & r_derailers = union(RailML3_IL_ROUTE_DERAILERS[{Route}]) \/ union(RailML3_IL_OVERLAP_DERAILERS[r_overlaps])
                \/ dom({ der, rr | rr : r_relations /\ dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS) & der : dom(union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[{proving_continuously} * BOOL])) })
            & !i_der.( i_der : r_derailers => prj1(IS_derailer_states(i_der)) /: dom(ran(IS_derailersInMovement)) & prj2(IS_derailer_states(i_der)) = position_passablePosition )  /*@desc "no derailer on the route are in movement or in derailing position" */

            & r_switches = union(RailML3_IL_ROUTE_SWITCH_POSITIONS[{Route}]) \/ union(RailML3_IL_OVERLAP_SWITCH_POSITIONS[r_overlaps])
                \/ dom({ sw, rr | rr : r_relations /\ dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS) & sw : union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[{proving_continuously} * BOOL]) })
                \/ dom({ sw, o | o : r_overlaps /\ dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS) & sw : union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[{proving_continuously} * BOOL]) })
            & !i_sw.( i_sw : dom(r_switches) => r_switches(i_sw) <: IS_switch_states(i_sw) ) /*@desc "the positions of switches are the expected" */
            & !i_sw_il.(i_sw_il : dom(union(ran(r_switches))) => i_sw_il /: dom(union(ran(IS_switchesInMovement))) ) /*@desc "no switches on the route are in movement" */

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
            & !rr.(rr : r_relations =>
                (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS) =>
                    union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_derailer_states
                    & !rrr.(rrr : union(RailML3_IL_ROUTE_RELATION_MUST_DERAILER_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_derailer_states)
                )
                & (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS) =>
                    union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_switch_states
                    & !rrr.(rrr : union(RailML3_IL_ROUTE_RELATION_MUST_SWITCH_POSITIONS(rr)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_switch_states)
                )
                & (rr : dom(RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES) =>
                    RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> FALSE}]~[{tvd_occupied}] <: dom(IL_occupiedTvdSections)
                    & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> FALSE}]~[{tvd_vacant, tvd_unknown}] /\ dom(IL_occupiedTvdSections) = {}
                    & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> TRUE}]~[{tvd_occupied}] /\ dom(IL_occupiedTvdSections) = {}
                    & RailML3_IL_ROUTE_RELATION_MUST_SECTION_STATES(rr)[{proving_continuously |-> TRUE}]~[{tvd_vacant, tvd_unknown}] <: dom(IL_occupiedTvdSections)
                )
            ) /*@desc "Preconditions from routeRelations for derailers, switches, and sections are fulfilled (all proving types)" */ // TODO for later extension: detection states                
            & !o.(o : r_overlaps /\ dom(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS) =>
                union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[RailML3_IL_REQUIRE_PROVING_TYPES * {FALSE}]) <: IS_switch_states
                & !rrr.(rrr : union(RailML3_IL_OVERLAP_MUST_SWITCH_POSITIONS(o)[RailML3_IL_REQUIRE_PROVING_TYPES * {TRUE}]) => rrr /: IS_switch_states)
            ) /*@desc "for all associated overlaps the switch positions are the expected (all proving types)" */
        THEN
            IL_routes_in_res := IL_routes_in_res - {Route |-> Train} ||
            IL_res_routes := IL_res_routes \/ {Route |-> Train} ||
            IL_crossing_locked_routes := IL_crossing_locked_routes \/ (dom(r_crossings) * {Route}) ||
            IL_derailer_locked_routes := IL_derailer_locked_routes \/ (r_derailers * {Route}) ||
            IL_switch_locked_routes := IL_switch_locked_routes \/ (dom(r_switches) * {Route}) ||
            IL_released_partialRoutes := IL_released_partialRoutes \/ {Route |-> {}}
        END;
    
    IL_partialRouteReleaseAhead(Route, routeReleaseGroup) =
        ANY
            train, r_crossings, r_derailers, r_switches, r_signals, NXT, NXT_partOfTwoRG, NXT_partOfTwoRoutes
        WHERE
            Route |-> train : IL_res_routes /*@desc "the route is currently reserved" */
            & Route : dom(RailML3_IL_ROUTE_RELEASE_GROUPS)
            & routeReleaseGroup : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead")
            & routeReleaseGroup /: IL_released_partialRoutes(Route) /*@desc "the routeReleaseGroupAhead is not released" */
            & r_crossings = dom({i_cr, branch | i_cr : IL_crossing_locked_routes~[{Route}] & branch : RailML3_IS_CROSSING_BRANCHES(i_cr) & branch : NXT })
            & r_derailers = dom({i_der, branch | i_der : IL_derailer_locked_routes~[{Route}] & branch : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) & branch : NXT })
            & r_switches = dom({i_sw, branch | i_sw : IL_switch_locked_routes~[{Route}] & branch : RailML3_IS_SWITCH_BRANCHES(i_sw) & branch : NXT })
            & r_signals = dom({i_sig, pos | i_sig : RailML3_IL_SIGNAL_CONTROLLED & pos : RailML3_IS_SPOT_LOCATIONS(i_sig) & pos : dom(NXT) \/ ran(NXT) & i_sig /= prj1(RailML3_IL_ROUTE_EXIT(Route)) })
            & !i_tvd.(i_tvd : RailML3_IL_ROUTE_RELEASE_GROUPS_AHEAD_TVD_SECTIONS(routeReleaseGroup) => i_tvd /: dom(IL_occupiedTvdSections)) /*@desc "all TVD sections of the release group are vacant" */
            & NXT = RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(routeReleaseGroup)(Route)
            & NXT_partOfTwoRG = dom({ porg, rg | rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead")
                & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                & porg : RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(routeReleaseGroup)(Route) /\ RailML3_IL_ROUTE_RELEASE_GROUP_AHEAD_NXT(rg)(Route) })
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT) & r /= Route & por |-> r : IL_res_route_blocks })
            & !(i_sig,sp).(i_sig : r_signals & sp : IL_signalplan_in_activation => i_sig /: RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master \/ sp'slave \/ sp'distant)] ) /*@desc "there is no signalplan in activation conflicting with the signals that will be affected by the release" */
        THEN
            IL_released_partialRoutes(Route) := IL_released_partialRoutes(Route) \/ {routeReleaseGroup} ||
            IL_res_route_blocks := IL_res_route_blocks - ( (dom(NXT-NXT_partOfTwoRG) \/ ran(NXT-NXT_partOfTwoRG)) <| (RailML3_IL_ROUTE_BLOCKS |> {Route}) ) ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_BLOCKS~)[{Route}] /\ (dom(NXT-NXT_partOfTwoRG) \/ ran(NXT-NXT_partOfTwoRG) \/ NXT_partOfTwoRoutes) ) ||
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
            Route |-> train : IL_res_routes /*@desc "the route is currently reserved" */
            & currTvdSection : allIdsOfType("tvdSection")
            & IL_occupiedTvdSections~[{train}] = {currTvdSection} /*@desc "the train occupies only a single TVD section" */
            & RailML3_IL_TVD_SECTION_BERTHING_TRACKS(currTvdSection) = TRUE /*@desc "the only occupied TVD section is a berthingTrack" */
            & (RS_trainFront(train) |-> prj2(RailML3_IL_ROUTE_EXIT(Route)) : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 or RS_trainFront(train) = prj2(RailML3_IL_ROUTE_EXIT(Route))) /*@desc "the train has not passed the route exit" */
            & RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupAhead") <: IL_released_partialRoutes(Route) /*@desc "all routeReleaseGroupsAhead of the route are released" */
            & NXT = RailML3_IL_ROUTE_NXT(Route)
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT) & r /= Route & por |-> r : IL_res_route_blocks })
            & r_signals = dom({ i_sig, pos | i_sig : RailML3_IL_SIGNAL_CONTROLLED & pos : RailML3_IS_SPOT_LOCATIONS(i_sig) & pos : dom(NXT) \/ ran(NXT) & i_sig /: RailML3_IL_SIGNAL_REFERS_TO[{prj1(RailML3_IL_ROUTE_EXIT(Route))}] })
            & !(i_sig,sp).(i_sig : r_signals & sp : IL_signalplan_in_activation => i_sig /: RailML3_IL_SIGNAL_REFERS_TO[dom(sp'master \/ sp'slave \/ sp'distant)] ) /*@desc "there is no signalplan in activation conflicting with the signals that will be affected by the release" */
        THEN
            IL_released_partialRoutes := {Route} <<| IL_released_partialRoutes ||
            IL_res_routes := IL_res_routes - {Route |-> train} ||
            IL_res_route_blocks := IL_res_route_blocks |>> {Route} ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_BLOCKS~)[{Route}] /\ ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes |>> {Route} ||
            IL_derailer_locked_routes := IL_derailer_locked_routes |>> {Route} ||
            IL_switch_locked_routes := IL_switch_locked_routes |>> {Route} ||
            IL_signal_states := IL_signal_states <+ (r_signals * {{aspect_closed}}) ||
            IL_noted_signal_states := r_signals <<| IL_noted_signal_states ||
            IL_signal_locked := IL_signal_locked - r_signals ||
            IL_res_overlaps := union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) <<| IL_res_overlaps ||
            IL_overlaps_in_release := IL_overlaps_in_release - union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            // movable elements return to preferred position
        END;

    IL_partialRouteReleaseRear(Route, routeReleaseGroup) =
        ANY
            train, r_crossings, r_derailers, r_switches, NXT, equiv_rgs
        WHERE
            Route |-> train : IL_res_routes /*@desc "the route is currently reserved" */
            & Route : dom(RailML3_IL_ROUTE_RELEASE_GROUPS)
            & routeReleaseGroup : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
            & routeReleaseGroup /: IL_released_partialRoutes(Route) /*@desc "the routeReleaseGroupRear is not released" */
            & r_crossings = dom({i_cr, branch | i_cr : IL_crossing_locked_routes~[{Route}] & branch : RailML3_IS_CROSSING_BRANCHES(i_cr) & branch : NXT })
            & r_derailers = dom({i_der, branch | i_der : IL_derailer_locked_routes~[{Route}] & branch : RailML3_IS_DERAILER_NOT_PASSABLE(i_der) & branch : NXT })
            & r_switches = dom({i_sw, branch | i_sw : IL_switch_locked_routes~[{Route}] & branch : RailML3_IS_SWITCH_BRANCHES(i_sw) & branch : NXT })
            & !i_tvd.(i_tvd : RailML3_IL_ROUTE_RELEASE_GROUPS_REAR_TVD_SECTIONS(routeReleaseGroup) => i_tvd /: dom(IL_occupiedTvdSections)) /*@desc "all TVD sections of the release group are vacant" */
            & NXT = RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(routeReleaseGroup)(Route)
            & equiv_rgs = { rg | rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear") & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                & RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route) <: RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(routeReleaseGroup)(Route) }
            & !(rg, fst_bl_of_other_rg, fst_bl_of_current_rg).( rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
                    & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route)
                    & fst_bl_of_other_rg : dom(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route)) & fst_bl_of_other_rg /: ran(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route))
                    & fst_bl_of_current_rg : dom(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(routeReleaseGroup)(Route)) & fst_bl_of_current_rg /: ran(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(routeReleaseGroup)(Route))
                => fst_bl_of_current_rg |-> fst_bl_of_other_rg : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 or fst_bl_of_other_rg = fst_bl_of_current_rg ) /*@desc "the release group is the last of all releasable groups" */
            & ( !nxt.(nxt : NXT => prj2(nxt) |-> RS_trainBack(train) : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1) or train /: RS_arrivedTrains ) /*@desc "the train has completely passed the release group" */
        THEN
            IL_released_partialRoutes(Route) := IL_released_partialRoutes(Route) \/ {routeReleaseGroup} \/ equiv_rgs ||
            LET NXT_partOfTwoRG, NXT_partOfTwoRoutes BE
                NXT_partOfTwoRG = dom({ porg, rg | rg : RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear")
                    & rg /= routeReleaseGroup & rg /: IL_released_partialRoutes(Route) \/ equiv_rgs
                    & porg : (dom(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route)) \/ ran(RailML3_IL_ROUTE_RELEASE_GROUP_REAR_NXT(rg)(Route))) /\ (dom(NXT) \/ ran(NXT))
                })
                & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT) & r /= Route & por |-> r : IL_res_route_blocks })
            IN            
                IL_res_route_blocks := IL_res_route_blocks - ( ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRG) <| (RailML3_IL_ROUTE_BLOCKS |> {Route}) ) ||
                IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_BLOCKS~)[{Route}] /\ ((dom(NXT) \/ ran(NXT)) - (NXT_partOfTwoRG \/ NXT_partOfTwoRoutes)) )
            END ||
            IL_crossing_locked_routes := IL_crossing_locked_routes - (r_crossings * {Route}) ||
            IL_derailer_locked_routes := IL_derailer_locked_routes - (r_derailers * {Route}) ||
            IL_switch_locked_routes := IL_switch_locked_routes - (r_switches * {Route})
            // movable elements return to preferred position
        END;
    IL_completeRouteReleaseRear(Route) =
        ANY
            train, NXT, NXT_partOfTwoRoutes
        WHERE
            Route |-> train : IL_res_routes /*@desc "the route is currently reserved" */
            & ( prj2(RailML3_IL_ROUTE_EXIT(Route)) |-> RS_trainBack(train) : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 or train /: RS_arrivedTrains ) /*@desc "the train has completely passed the route or left the interlocking area" */
            & RailML3_IL_ROUTE_RELEASE_GROUPS(Route) /\ allIdsOfType("routeReleaseGroupRear") <: IL_released_partialRoutes(Route) /*@desc "all routeReleaseGroupsRear of the route are released" */
            & !i_tvd.(i_tvd : union(RailML3_IL_ROUTE_TVD_SECTIONS[{Route}]) => i_tvd /: dom(IL_occupiedTvdSections)) /*@desc "all TVD sections of the route are vacant" */
            & NXT = RailML3_IL_ROUTE_NXT(Route)
            & NXT_partOfTwoRoutes = dom({ por, r | r : dom(IL_routes_in_res \/ IL_res_routes) & por : dom(NXT) \/ ran(NXT) & r /= Route & por |-> r : IL_res_route_blocks })
        THEN
            IL_released_partialRoutes := {Route} <<| IL_released_partialRoutes ||
            IL_res_routes := IL_res_routes - {Route |-> train} ||
            IL_res_route_blocks := IL_res_route_blocks |>> {Route} ||
            IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_BLOCKS~)[{Route}] /\ ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes) ) ||
            IL_crossing_locked_routes := IL_crossing_locked_routes |>> {Route} ||
            IL_derailer_locked_routes := IL_derailer_locked_routes |>> {Route} ||
            IL_switch_locked_routes := IL_switch_locked_routes |>> {Route} ||
            IL_res_overlaps := union(RailML3_IL_ROUTE_OVERLAPS[{Route}]) <<| IL_res_overlaps ||
            IL_overlaps_in_release := IL_overlaps_in_release - union(RailML3_IL_ROUTE_OVERLAPS[{Route}])
            // movable elements return to preferred position
        END;

    IL_startOverlapRelease(OverlapId) =
        ANY
            train
        WHERE
            OverlapId |-> train : IL_res_overlaps /*@desc "the overlap is currently reserved" */
            & OverlapId : dom(RailML3_IL_OVERLAP_RELEASE_TRIGGER_SECTION)
            & OverlapId /: IL_overlaps_in_release /*@desc "the overlap is currently not in release" */
            & RailML3_IL_OVERLAP_RELEASE_TRIGGER_SECTION(OverlapId) |-> train : IL_occupiedTvdSections /*@desc "an overlap release trigger section is occupied by the reserving train" */
        THEN
            IL_overlaps_in_release := IL_overlaps_in_release \/ {OverlapId}
        END;
    IL_endOverlapRelease(OverlapId) =
        ANY
            r_crossings, r_derailers, r_switches
        WHERE
            OverlapId : IL_overlaps_in_release /*@desc "the overlap is currently in release" */
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
            Signalplan /: IL_signalplan_in_activation /*@desc "the signalplan is not in activation" */
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
            ) /*@desc "if the signalplan in activation corresponds to a route, the route reservation must be in progress" */
            & !sp.(sp : IL_signalplan_in_activation => sp'master /= Signalplan'master & sp'slave /= Signalplan'slave) /*@desc "there is no signalplan in activation with the same master and slave signal" */
            & LET signalplan_relation BE signalplan_relation = Signalplan'master \/ Signalplan'slave \/ Signalplan'distant IN
                !(signal_il, signal_is).(signal_il : dom(signalplan_relation) & signal_is = RailML3_IL_SIGNAL_REFERS_TO(signal_il) =>
                    IL_signal_states(signal_is) = {aspect_closed}
                    & signal_is /: IL_signal_locked
                    & ( signal_is : dom(IL_noted_signal_states) => signalplan_relation(signal_il) = IL_noted_signal_states(signal_is) )
                ) /*@desc "signals of the signalplan are not locked and if a change is already noted it matches the signalplan" */
                & !sp.(sp : IL_signalplan_in_activation & sp /= Signalplan & dom(sp'slave) <: dom(signalplan_relation) => sp'slave = Signalplan'master) /*@desc "if there is any other signalplan whose slave signal is changed by the current signalplan, the slave signal must be the master signal of the current signalplan showing the same aspect" */
            END
        THEN
            IL_signalplan_in_activation := IL_signalplan_in_activation \/ {Signalplan} ||
            IL_signal_locked := IL_signal_locked \/ dom({ s1, s2 | s2 : Signalplan'slave \/ Signalplan'distant & aspect_closed /: prj2(s2) & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) })
        END;
    IL_endActivateSignalplan(Signalplan) =
        ANY
            signalplan_relation
        WHERE
            Signalplan : IL_signalplan_in_activation /*@desc "the signalplan is in activation" */
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
            ) /*@desc "if the signalplan in activation corresponds to a route, the route reservation must be completed" */
            & dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) <: IL_noted_signal_states /*@desc "the noted signal states of all associated signals must match the requested aspects of the signalplan" */
            & !(s,sp_act).(s : dom(Signalplan'master) & sp_act : IL_signalplan_in_activation & sp_act /= Signalplan => s /: dom(sp_act'slave))  /*@desc "all signalplans for the master signal are activated" */
        THEN
            IL_signalplan_in_activation := IL_signalplan_in_activation - {Signalplan} ||
            IL_signal_locked := IL_signal_locked - dom({ s1, s2 | s2 : dom(signalplan_relation) & s1 = RailML3_IL_SIGNAL_REFERS_TO(s2) & aspect_closed : signalplan_relation(s2)
                & not(#sp.(sp : IL_signalplan_in_activation & s2 : dom(sp'slave \/ sp'distant))) }) ||
            IL_signal_states := IL_signal_states <+ dom({ s1, s2 | s2 : signalplan_relation & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) ||
            IL_noted_signal_states := IL_noted_signal_states
                - dom({ s1, s2 | s2 : signalplan_relation & not( #sp.( sp : IL_signalplan_in_activation & sp /= Signalplan & s2 : sp'master \/ sp'slave \/ sp'distant ) ) & s1 = RailML3_IL_SIGNAL_REFERS_TO(prj1(s2)) |-> prj2(s2) }) 
        END;
    IL_noteChangeSignalState(SignalId) = // for signals that are controlled by at least one signalplan
        ANY
            currAspect, destAspect
        WHERE
            SignalId : RailML3_IL_SIGNAL_CONTROLLED
            & SignalId /: dom(IL_noted_signal_states) /*@desc "the signal has no noted change" */
            & currAspect = IL_signal_states(SignalId)
            & #(signalplan, sp_rel, signalId_il).(signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] & sp_rel = signalplan'master \/ signalplan'slave \/ signalplan'distant & signalId_il : dom(sp_rel)
                & destAspect = sp_rel(signalId_il) ) /*@desc "signal is part of a signalplan in activation" */
            & (aspect_closed /: destAspect =>
                ( !(signalplan, signalId_il).( signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] /\ dom(ran(RailML3_IL_ROUTE_ENTRY)) /\ dom(signalplan'master) =>
                    #route.( route : dom(RailML3_IL_ROUTE_ENTRY) & prj1(RailML3_IL_ROUTE_ENTRY(route)) = signalId_il & route : dom(IL_res_routes) ) )
                or 
                #( signalplan, signalId_il).( signalplan : IL_signalplan_in_activation & signalId_il : RailML3_IL_SIGNAL_REFERS_TO~[{SignalId}] /\ dom(ran(RailML3_IL_ROUTE_ENTRY)) /\ dom(signalplan'slave)
                    & #route.( route : dom(RailML3_IL_ROUTE_ENTRY) & prj1(RailML3_IL_ROUTE_ENTRY(route)) = signalId_il & route : dom(IL_routes_in_res) ) )
                )
                & #(sig_exit, sloc_sig, sloc_sig_exit).(
                    sig_exit : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(SignalId)
                    & sloc_sig : RailML3_IS_SPOT_LOCATIONS(SignalId)
                    & sloc_sig_exit : RailML3_IS_SPOT_LOCATIONS(sig_exit)
                    & sloc_sig |-> sloc_sig_exit : closure1(IS_next)
                    & !tvd.(tvd : RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(SignalId)(sig_exit) => tvd /: dom(IL_occupiedTvdSections))
                )
            ) /*@desc "following track to next signal or trackEnd is passable, i.e. in IS_next, and all tvdSections are free (only if destAspect is not aspect_closed, closing a signal is always possible)" */
        THEN
            IL_noted_signal_states := IL_noted_signal_states \/ {SignalId |-> destAspect}
        END;
    IL_changeSignalState(SignalId) = // for signals that are not controlled by any signalplan
        ANY
            currAspect, destAspect, isDistantSig
        WHERE
            SignalId : RailML3_IL_SIGNAL_CONTROLLED /*@desc "only controllable signals can be changed" */
            & #train.(train : RS_arrivedTrains & RS_trainFront(train) : RailML3_IS_SPOT_LOCATIONS(SignalId)) /*@desc "a train is standing in front of the signal" */
            & SignalId /: dom(IL_noted_signal_states) /*@desc "the signal has no noted change" */
            & currAspect = IL_signal_states(SignalId)
            & SignalId /: IL_signal_locked /*@desc "the signal is not locked" */
            & ( (SignalId : RailML3_IL_SIGNAL_NOT_CONTROLLED_BY_SIGNALPLAN & isDistantSig = FALSE)
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
                    & isDistantSig = TRUE
                )
            ) /*@desc "the signal is an independent signal or a not locked distant signal from any signalplan" */
            & destAspect : {{aspect_proceed}, {aspect_closed}}
            & destAspect /= currAspect
            & ( aspect_closed /: destAspect =>
                #(sig_exit, sloc_sig, sloc_sig_exit).(
                    sig_exit : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(SignalId)
                    & sloc_sig : RailML3_IS_SPOT_LOCATIONS(SignalId)
                    & sloc_sig_exit : RailML3_IS_SPOT_LOCATIONS(sig_exit)
                    & sloc_sig |-> sloc_sig_exit : closure1(IS_next)
                    & !tvd.(tvd : RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(SignalId)(sig_exit) => tvd /: dom(IL_occupiedTvdSections))
                    & !(NXT,nxt).(NXT = RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId)(sig_exit) & nxt : dom(ran(NXT)) => nxt /: union(ran(RS_trainOccupiedLocations)))
                    & ( isDistantSig = FALSE => !route.(route : dom(IL_routes_in_res \/ IL_res_routes) => RailML3_IL_ROUTE_NXT(route) /\ RailML3_IL_SIGNAL_CONTROL_SECTIONS(SignalId)(sig_exit) = {}) )
                )
            ) /*@desc "following track to next signal or trackEnd is passable, i.e. in IS_next, and all tvdSections are free (only if destAspect is not aspect_closed, closing a signal is always possible)" */
        THEN
            IL_signal_states(SignalId) := destAspect
        END;

    RS_trainArrivalRequest(OpenendId, Train, Position) =
        ANY
            tvds
        WHERE
            OpenendId : RailML3_IS_OPENEND_IDS
            & Position : dom(union(RailML3_IS_SPOT_LOCATIONS[{OpenendId}])) * {direction_normal, direction_reverse}
            & Position : dom(IS_next)
            & tvds <: allIdsOfType("tvdSection")
            & tvds /= {} /*@desc "open end must be part of at least one TVD section" */
            & !tvd.(tvd : tvds => Position : dom(RailML3_IL_TVD_SECTIONS(tvd)) or prj1(IS_next(Position)) : dom(dom(RailML3_IL_TVD_SECTIONS(tvd))))
            & tvds /\ dom(IL_occupiedTvdSections) = {} /*@desc "TVD sections of open end are vacant" */
            & !r.(r : dom(IL_routes_in_res \/ IL_res_routes) => union(RailML3_IL_ROUTE_TVD_SECTIONS[{r}]) /\ tvds = {}) /*@desc "TVD sections of open end are not part of reserved route" */
            & !o.(o : dom(IL_res_overlaps) => union(RailML3_IL_OVERLAP_TVD_SECTIONS[{o}]) /\ tvds = {}) /*@desc "TVD sections of open end are not part of reserved overlap" */
            & Train : RS_trains - (RS_arrivedTrains \/ dom(RS_requestingArrivalTrains)) /*@desc "the train has not yet arrived" */
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections \/ (tvds * {Train}) ||
            RS_requestingArrivalTrains := RS_requestingArrivalTrains \/ {Train |-> Position} ||
            LET signals BE
                signals = { i_sig | i_sig : RailML3_IL_SIGNAL_CONTROLLED & OpenendId : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(i_sig) & RailML3_IL_SIGNAL_CONTROL_SECTIONS(i_sig)(OpenendId) <: IS_next }
            IN
                IL_signal_states := IL_signal_states <+ (signals * {{aspect_closed}}) ||
                IL_noted_signal_states := signals <<| IL_noted_signal_states
            END
        END;
    IL_trainAcceptArrival(Train) =
        ANY
            position, route
        WHERE
            Train |-> position : RS_requestingArrivalTrains
            & route : allIdsOfType("route")
            & ( prj1(RailML3_IL_ROUTE_ENTRY(route)) : RailML3_IS_OPENEND_IDS & prj2(RailML3_IL_ROUTE_ENTRY(route)) = position =>
                route : dom(IL_res_routes) & IL_res_routes(route) = Train
            ) /*@desc "if the open end is a route entry, a route must be reserved by the arriving train" */
        THEN            
            RS_requestingArrivalTrains := {Train} <<| RS_requestingArrivalTrains ||
            RS_trainOccupiedLocations := RS_trainOccupiedLocations \/ {Train |-> {prj1(position), prj1(IS_next(position))}} ||
            RS_trainFront := RS_trainFront \/ {Train |-> IS_next(position)} ||
            RS_trainBack := RS_trainBack \/ {Train |-> position} ||
            RS_arrivedTrains := RS_arrivedTrains \/ {Train}
        END;
    IL_trainDeclineArrival(Train) =
        SELECT
            Train : dom(RS_requestingArrivalTrains)
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections |>> {Train} ||
            RS_requestingArrivalTrains := {Train} <<| RS_requestingArrivalTrains ||
            RS_trainOccupiedLocations := {Train} <<| RS_trainOccupiedLocations ||
            RS_trainFront := {Train} <<| RS_trainFront ||
            RS_trainBack := {Train} <<| RS_trainBack
        END;
    RS_trainLeave(Train) =
        ANY
            position, tvdSection
        WHERE
            position : dom(union(RailML3_IS_SPOT_LOCATIONS[RailML3_IS_OPENEND_IDS])) * {direction_normal, direction_reverse}
            & tvdSection : allIdsOfType("tvdSection")
            & tvdSection : IL_occupiedTvdSections~[{Train}]
            & position : ran(RailML3_IL_TVD_SECTIONS(tvdSection))
            & Train : RS_arrivedTrains
            & RS_trainFront(Train) = position /*@desc "front of train is location of an open end" */
            & RS_trainBack(Train) |-> position : RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS /*@desc "front and back of train are directly connected in topology" */
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections |>> {Train} ||
            RS_arrivedTrains := RS_arrivedTrains - {Train} ||
            RS_trainOccupiedLocations := {Train} <<| RS_trainOccupiedLocations ||
            RS_trainFront := {Train} <<| RS_trainFront ||
            RS_trainBack := {Train} <<| RS_trainBack ||
            VAR lastRoutes, NXT, NXT_partOfTwoRoutes
            IN
                lastRoutes := { r | r |-> Train : IL_routes_in_res \/ IL_res_routes };
                NXT := union(RailML3_IL_ROUTE_NXT[lastRoutes]);
                NXT_partOfTwoRoutes := dom({ por, r | #train.(r |-> train : IL_routes_in_res \/ IL_res_routes & por : dom(NXT) \/ ran(NXT) & r /: lastRoutes & train /= Train & por |-> r : IL_res_route_blocks )});

                IL_released_partialRoutes := lastRoutes <<| IL_released_partialRoutes ||
                IL_res_routes := IL_res_routes - (lastRoutes * {Train}) ||
                IL_res_route_blocks := IL_res_route_blocks |>> lastRoutes ||
                IL_res_blocks := IL_res_blocks - ( (RailML3_IL_ROUTE_BLOCKS~)[lastRoutes] /\ ((dom(NXT) \/ ran(NXT)) - NXT_partOfTwoRoutes) ) ||
                IL_crossing_locked_routes := IL_crossing_locked_routes |>> lastRoutes ||
                IL_derailer_locked_routes := IL_derailer_locked_routes |>> lastRoutes ||
                IL_switch_locked_routes := IL_switch_locked_routes |>> lastRoutes ||
                IL_res_overlaps := union(RailML3_IL_ROUTE_OVERLAPS[lastRoutes]) <<| IL_res_overlaps ||
                IL_overlaps_in_release := IL_overlaps_in_release - union(RailML3_IL_ROUTE_OVERLAPS[lastRoutes])
            END
        END;

    RS_trainMoveFront(currFront, newFront, Train) =
        ANY
            tvds, passed_signals, other_signals
        WHERE
            Train : RS_arrivedTrains
            & currFront = RS_trainFront(Train)
            & currFront |-> newFront : RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS /*@desc "allows overrunning of incorrectly set trailing switches" */ 
            & (card(IS_next[{currFront}]) > 0 => currFront |-> newFront : IS_next) /*@desc "for correct overrunning of facing switches" */
            & tvds = dom({ i_t, i_tde | i_t : allIdsOfType("tvdSection") & i_tde : allIdsOfType("trainDetectionElement") & currFront : RailML3_IS_SPOT_LOCATIONS(i_tde)
                & currFront : dom(RailML3_IL_TVD_SECTIONS(i_t)) & i_tde : RailML3_IL_TVD_SECTION_DEMARCATING_ELEMENTS(i_t)})
            & passed_signals = { i_sig | i_sig : RailML3_IL_SIGNAL_CONTROLLED & currFront : RailML3_IS_SPOT_LOCATIONS(i_sig) } /*@desc "contains signals that will be passed by the train" */
            & !i_sig.(i_sig : passed_signals => aspect_closed /: IL_signal_states(i_sig)) /*@desc "all signal that will be passed allow passing" */
            & other_signals = dom({ i_sig, exit | i_sig : RailML3_IL_SIGNAL_CONTROLLED - IL_signal_locked & exit : RailML3_IL_SIGNAL_END_OF_CONTROL_SECTION(i_sig) & RailML3_IL_SIGNAL_CONTROL_SECTIONS(i_sig)(exit) <: IS_next & tvds /\ RailML3_IL_SIGNAL_CONTROL_TVD_SECTIONS(i_sig)(exit) /= {} }) /*@desc "contains other influenced signals by move (of signal control sections which the train enters)" */
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections \/ (tvds * {Train}) ||
            RS_trainOccupiedLocations(Train) := RS_trainOccupiedLocations(Train) \/ {prj1(newFront)} ||
            RS_trainFront(Train) := newFront ||
            IL_signal_states := IL_signal_states <+ ((passed_signals \/ other_signals) * {{aspect_closed}}) ||
            IL_noted_signal_states := other_signals <<| IL_noted_signal_states <+ passed_signals * {{aspect_closed}} ||
            IL_signal_locked := IL_signal_locked - passed_signals
        END;
    RS_trainMoveBack(currBack, newBack, Train) =
        ANY
            tvds
        WHERE
            Train : RS_arrivedTrains
            & currBack = RS_trainBack(Train)
            & newBack /= RS_trainFront(Train)
            & currBack |-> newBack : RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS /*@desc "allows overrunning of incorrectly set trailing switches" */ 
            & (card(IS_next[{currBack}]) > 0 => currBack |-> newBack : IS_next) /*@desc "for correct overrunning of facing switches" */ 
            & prj1(newBack) : RS_trainOccupiedLocations(Train)
            & tvds = dom({ i_t, i_tde | i_t : allIdsOfType("tvdSection") & i_tde : allIdsOfType("trainDetectionElement") & newBack : RailML3_IS_SPOT_LOCATIONS(i_tde)
                & i_tde : RailML3_IL_TVD_SECTION_DEMARCATING_ELEMENTS(i_t) & RS_trainOccupiedLocations(Train) /\ dom(dom(RailML3_IL_TVD_SECTIONS(i_t)) \/ ran(RailML3_IL_TVD_SECTIONS(i_t))) = {prj1(currBack),prj1(newBack)} })
        THEN
            IL_occupiedTvdSections := IL_occupiedTvdSections - (tvds * {Train}) ||
            RS_trainOccupiedLocations(Train) := RS_trainOccupiedLocations(Train) - {prj1(currBack)} ||
            RS_trainBack(Train) := newBack
        END;
    RS_trainChangeDirection(Train) =
        ANY
            newDir, currTvdSection
        WHERE
            Train : RS_arrivedTrains
            & newDir : {direction_normal, direction_reverse}
            & newDir /= prj2(RS_trainFront(Train))
            & prj2(RS_trainFront(Train)) = prj2(RS_trainBack(Train))
            & prj1(RS_trainFront(Train)) |-> newDir : dom(IS_next)
            & prj1(RS_trainBack(Train)) |-> newDir : ran(IS_next)
            & currTvdSection : allIdsOfType("tvdSection")
            & IL_occupiedTvdSections~[{Train}] = {currTvdSection} /*@desc "the berthingTrack is the only occupied TVD section of the train" */
            & RS_trainBack(Train) |-> RS_trainFront(Train) : closure1(RailML3_IL_TVD_SECTIONS(currTvdSection)) /*@desc "the train is completely located on a TVD section" */
            & RailML3_IL_TVD_SECTION_BERTHING_TRACKS(currTvdSection) = TRUE /*@desc "the current TVD section is a berthingTrack" */
            & Train /: ran(IL_routes_in_res \/ IL_res_routes \/ IL_res_overlaps) /*@desc "before changing directions, the train must release all its reserved routes and overlaps" */
        THEN
            RS_trainFront(Train) := prj1(RS_trainBack(Train)) |-> newDir ||
            RS_trainBack(Train) := prj1(RS_trainFront(Train)) |-> newDir
        END
END'''])
    END;

    printValidationMachine =
        PRE printValidation = TRUE THEN
            FPRINTF(outputValidationFile, "~w~n", ['''/*@generated */''']);
            FPRINTF(outputValidationFile, "RULES_MACHINE ~w~n", [validationMachineName]);
            FPRINTF(outputValidationFile, "REFERENCES ~w~n", [dataMachineName]);

            FPRINTF(outputValidationFile, "~w~n", ['''DEFINITIONS
    ignoreDirection(Relation) == dom({ r, cr | cr : Relation & r = prj1(prj1(cr)) |-> prj1(prj2(cr)) });
    inverseDirectedRelation(Relation) == dom({ ir, r | r : Relation & ir = prj1(prj2(r)) |-> oppositeDirection(prj2(prj2(r))) |-> (prj1(prj1(r)) |-> oppositeDirection(prj2(prj1(r)))) });
    inverseDirectedPair(Pair) == prj1(Pair) |-> oppositeDirection(prj2(Pair));
    inverseSwitchPosition(pos) == IF pos = position_left THEN position_right ELSE position_left END;
    oppositeDirection(dir) == IF dir = direction_normal THEN direction_reverse ELSE direction_normal END;
    distanceBetweenPositionsOnRelation(Pos1, Pos2, Relation) ==
        SIGMA(bl, begin).( begin : dom(Relation) & begin /: ran(Relation) & bl = prj1(prj1(begin)) & RailML3_IS_NET_ELEMENT_LENGTHS(bl) /= -1.0 | RailML3_IS_NET_ELEMENT_LENGTHS(bl) )
        + SIGMA(bl, begin, end).( begin : dom(Relation) & begin /: ran(Relation) & end /: dom(Relation) & end : ran(Relation) & bl : dom(dom(ran(Relation))) - dom(dom({begin,end})) & RailML3_IS_NET_ELEMENT_LENGTHS(bl) /= -1.0 | RailML3_IS_NET_ELEMENT_LENGTHS(bl) )
        + SIGMA(bl, end).( end /: dom(Relation) & end : ran(Relation) & bl = prj1(prj1(end)) & RailML3_IS_NET_ELEMENT_LENGTHS(bl) /= -1.0 | RailML3_IS_NET_ELEMENT_LENGTHS(bl) );    
    distanceBetween(a,b) ==
        min(
            dom({ l, nxt | #r.(r = ( railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1[{a}] /\ railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1~[{b}] ) \/ {a, b}
                & nxt = r <| RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS |> r
                //& nxt : dom(RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS) >+> ran(RailML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS)
                //& a |-> b : closure1(nxt)
                & l = SIGMA(c).( c : nxt & prj1(prj1(prj1(c))) = prj1(prj1(prj2(c))) & prj1(prj1(prj1(c))) : dom(RailML3_IS_NET_ELEMENT_LENGTHS) & RailML3_IS_NET_ELEMENT_LENGTHS(prj1(prj1(prj1(c)))) /= -1.0 | RABS(prj2(prj1(prj1(c))) - prj2(prj1(prj2(c)))) * RailML3_IS_NET_ELEMENT_LENGTHS(prj1(prj1(prj1(c)))) )
            )})
        );

    elementOfId(Set, eId) == MU( { e | e : Set & eId = MU(e'Id)} );

    "CHOOSE.def";
    "LibraryMath.def";
    "LibraryMeta.def";
    "LibraryReals.def";
    "LibraryStrings.def";
    "SORT.def";
    SET_PREF_MEMOIZE_FUNCTIONS == TRUE;
    SET_PREF_optimize_enum_set_elems == TRUE;

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
    END;
    RULE MinDistanceOfTDEs // requires all netElements to have an attribute length
    BODY
        RULE_FORALL i_tde1, i_tde2, sloc1, sloc2, dist
            WHERE {i_tde1, i_tde2} <: allIdsOfType("trainDetectionElement") & i_tde1 /= i_tde2
                & sloc1 : RailML3_IS_SPOT_LOCATIONS(i_tde1) & sloc2 : RailML3_IS_SPOT_LOCATIONS(i_tde2) & sloc1 |-> sloc2 : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1
                & !i_ne.(i_ne : dom(RailML3_IS_NET_ELEMENT_LENGTHS) /\ RailML3_IS_NET_RESOURCES_MICRO_LEVEL => RailML3_IS_NET_ELEMENT_LENGTHS(i_ne) /= -1.0)
                & not(#(i_tde3, sloc3).(i_tde3 : allIdsOfType("trainDetectionElement") & sloc3 : RailML3_IS_SPOT_LOCATIONS(i_tde3) & sloc1 |-> sloc3 : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1 & sloc3 |-> sloc2 : railML3_IS_NET_RELATION_SUBSEQUENT_LOCATIONS_closure1))
                & dist = distanceBetween(sloc1, sloc2)
            EXPECT dist > 21.0
        COUNTEREXAMPLE
            STRING_FORMAT("Distance between trainDetectionElements ~w and ~w is ~w (<= 21.0)", i_tde1, i_tde2, dist)
        END
    END
END'''])
    END
END