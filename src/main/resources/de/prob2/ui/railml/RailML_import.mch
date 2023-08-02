MACHINE RailML_import
INCLUDES RailML_validation_flat
DEFINITIONS
  "LibraryIO.def";
  "LibraryMeta.def";
  "LibraryStrings.def";
  "SORT.def";
  SET_PREF_ALLOW_OPERATION_CALLS_IN_EXPRESSIONS == TRUE;

  PRINT_REPORT == FALSE;
  PRINT_FILE == "d:/Users/jangr/OneDrive - hhu.de/[Master] 4. Semester/Masterarbeit/Gitlab/b-railml/Code/validation_report.txt";

  runErrorRule(Rule) ==
    IF no_error = TRUE THEN
      VAR Var1, Var2 IN
        Var1, Var2 <-- Rule;
        IF Var1 = "SUCCESS"
          THEN IF PRINT_REPORT = TRUE THEN FPRINTF(PRINT_FILE, "[SUCCESS ~w] Validation succeeded without any error", {1 |-> TO_STRING(Rule)}) ELSE skip END
          ELSE
            no_error := FALSE;
            IF PRINT_REPORT = TRUE THEN FPRINTF(PRINT_FILE, "--- ERROR ---~nFile import aborted, the specified file does not contain valid railML.~n------~n~w [ERROR ~w] Validation failed with the following errors: ~w", {1 |-> TO_STRING(Rule), 2 |-> TO_STRING(Var2[{1}])}) ELSE skip END;
            ADD_ERROR("RailML: ", "File import aborted, the specified file does not contain valid railML.");
            VAR s, i IN
              s := SORT(Var2[{1}]);
              i := 1;
              WHILE i < card(s) DO
                ADD_ERROR("RailML", s(i));
                ADD_STATE_ERROR("RailML", s(i));
                i := i+1
                INVARIANT btrue
                VARIANT card(s)+1 - i
              END
            END            
        END
      END
    ELSE skip END;
  runWarningRule(Rule) ==
    IF no_error = TRUE THEN
      VAR Var1, Var2 IN
        Var1, Var2 <-- Rule;
        IF Var1 = "SUCCESS"
          THEN IF PRINT_REPORT = TRUE THEN FPRINTF(PRINT_FILE, "[SUCCESS ~w] Validation succeeded without any warning", {1 |-> TO_STRING(Rule)}) ELSE skip END
          ELSE
            IF PRINT_REPORT = TRUE THEN FPRINTF(PRINT_FILE, "[WARNING ~w] Validation succeeded with the following warnings: ~w", {1 |-> TO_STRING(Rule), 2 |-> TO_STRING(Var2[{2}])}) ELSE skip END;
            warnings := warnings \/ Var2[{1}]
            /*VAR s, i IN
              s := SORT(Var2[{1}]);
              i := 1;
              WHILE i < card(s) DO
                ADD_WARNING("RailML: ", s(i));
                i := i+1
                INVARIANT btrue
                VARIANT card(s)+1 - i
              END
            END*/
        END
      END
    ELSE skip END;
  runComputation(Computation) == IF no_error = TRUE THEN Computation ELSE skip END
VARIABLES no_error, warnings
INVARIANT no_error : BOOL & warnings : FIN(STRING)
INITIALISATION no_error := TRUE; warnings := {}
OPERATIONS
  importRailML = 
    BEGIN
      // RailML_readFile
      runErrorRule(is_supported_railml);
      runErrorRule(unique_ids);
      runComputation(read_file);

      // BEGIN RailML3_CO
      runComputation(set_name);
      runErrorRule(is_valid_name);
      runComputation(set_CO_NAME);
      // END RailML3_CO
      
      // BEGIN RailML3_IS
      runComputation(set_infrastructure);
      runErrorRule(is_valid_infrastructure);

      runComputation(set_topology);
      runErrorRule(is_valid_topology);

      runComputation(set_netElements);
      runErrorRule(is_valid_netElements);
      runComputation(set_netElement);
      runErrorRule(is_valid_netElement);
      runComputation(set_NET_ELEMENT);
      runWarningRule(warnings_netElement);

      runComputation(set_netRelations);
      runErrorRule(is_valid_netRelations);
      runComputation(set_netRelation);
      runErrorRule(is_valid_netRelation);      
      runComputation(set_NET_RELATION);
      runWarningRule(warnings_netRelation);

      runComputation(set_networks);
      runErrorRule(is_valid_networks);      
      runComputation(set_network);
      runErrorRule(is_valid_network);      
      runComputation(set_level);
      runErrorRule(is_valid_level);      
      runComputation(set_NETWORK);
      runErrorRule(validate_level);
      runWarningRule(warnings_level);

      runComputation(set_functionalInfrastructure);
      runErrorRule(is_valid_functionalInfrastructure);

      runComputation(set_borders);
      runErrorRule(is_valid_borders);
      runComputation(set_border);
      runErrorRule(is_valid_border);
      runComputation(set_BORDER);

      runComputation(set_bufferStops);
      runErrorRule(is_valid_bufferStops);
      runComputation(set_bufferStop);
      runErrorRule(is_valid_bufferStop);

      runComputation(set_crossings);
      runErrorRule(is_valid_crossings);
      runComputation(set_crossing);
      runErrorRule(is_valid_crossing);
      runComputation(set_CROSSING);

      runComputation(set_derailersIS);
      runErrorRule(is_valid_derailersIS);
      runComputation(set_derailerIS);
      runErrorRule(is_valid_derailerIS);

      runComputation(set_operationalPoints);
      runErrorRule(is_valid_operationalPoints);
      runComputation(set_operationalPoint);
      runErrorRule(is_valid_operationalPoint);

      runComputation(set_signalsIS);
      runErrorRule(is_valid_signalsIS);
      runComputation(set_signalIS);
      runErrorRule(is_valid_signalIS);      

      runComputation(set_switchesIS);
      runErrorRule(is_valid_switchesIS);
      runComputation(set_switchIS);
      runErrorRule(is_valid_switchIS);
      runComputation(set_SWITCH);
      runErrorRule(validate_switchIS);

      runComputation(set_tracks);
      runErrorRule(is_valid_tracks);
      runComputation(set_track);
      runErrorRule(is_valid_track);

      runComputation(set_trainDetectionElements);
      runErrorRule(is_valid_trainDetectionElements);
      runComputation(set_trainDetectionElement);
      runErrorRule(is_valid_trainDetectionElement);

      runComputation(set_linearLocation);
      runErrorRule(is_valid_linearLocation);      
      runComputation(set_LINEAR_LOCATION);
      runWarningRule(warnings_linearLocation);

      runComputation(set_spotLocation);
      runErrorRule(is_valid_spotLocation);
      runComputation(set_SPOT_LOCATION);
      runErrorRule(validate_spotLocation);
      runWarningRule(warnings_spotLocation);

      runComputation(set_linearCoordinate);
      runErrorRule(is_valid_linearCoordinate);
      //runComputation(set_LINEAR_COORDINATE);

      runComputation(set_SIGNAL);      
      runComputation(set_NET_RELATION_SUBSEQUENT_BLOCKS);
      runErrorRule(validate_linearLocation);
      runComputation(set_TRACK);

      runErrorRule(validate_border);
      runErrorRule(validate_bufferStop);
      runErrorRule(validate_track);
      // END RailML3_IS

      // BEGIN RailML3_VIS
      runComputation(set_visualizations);
      runErrorRule(is_valid_visualizations);

      runComputation(set_infrastructureVisualizations);
      runErrorRule(is_valid_infrastructureVisualizations);
      runComputation(set_infrastructureVisualization);
      runErrorRule(is_valid_infrastructureVisualization);

      runComputation(set_spotElementProjection);
      runErrorRule(is_valid_spotElementProjection);

      runComputation(set_linearElementProjection);
      runErrorRule(is_valid_linearElementProjection);

      runComputation(set_NET_ELEMENT_COORDINATES);
      // END RailML3_VIS
      
      // BEGIN RailML3_IL
      runComputation(set_interlocking);
      runErrorRule(is_valid_interlocking);

      runComputation(set_assetsForInterlockings);
      runErrorRule(is_valid_assetsForInterlockings);
      runComputation(set_assetsForInterlocking);
      runErrorRule(is_valid_assetsForInterlocking);      

      runComputation(set_tvdSections);
      runErrorRule(is_valid_tvdSections);
      runComputation(set_tvdSection);
      runErrorRule(is_valid_tvdSection);
      runComputation(set_TVD_SECTION_IDS);

      runComputation(set_derailersIL);
      runErrorRule(is_valid_derailersIL);
      runComputation(set_derailerIL);
      runErrorRule(is_valid_derailerIL);

      runComputation(set_movableCrossings);
      runErrorRule(is_valid_movableCrossings);
      runComputation(set_movableCrossing);
      runErrorRule(is_valid_movableCrossing);
      runComputation(set_IL_MOVABLE_CROSSING_IDS);      

      runComputation(set_signalsIL);
      runErrorRule(is_valid_signalsIL);
      runComputation(set_signalIL);
      runErrorRule(is_valid_signalIL);
      runComputation(set_IL_SIGNAL_IDS);

      runComputation(set_switchesIL);
      runErrorRule(is_valid_switchesIL);
      runComputation(set_switchIL);
      runErrorRule(is_valid_switchIL);
      runComputation(set_IL_SWITCH_IDS);
      //runErrorRule(validate_switchIL);

      runComputation(set_IL_MOVABLE_CROSSING);
      runErrorRule(validate_movableCrossing);
      runWarningRule(warnings_movableCrossing);
      runComputation(set_IL_DERAILER);
      runErrorRule(validate_derailerIL);
      runComputation(set_IL_SWITCH);      

      runComputation(set_routeReleaseGroupsAhead);
      runErrorRule(is_valid_routeReleaseGroupsAhead);
      runComputation(set_routeReleaseGroupAhead);
      runErrorRule(is_valid_routeReleaseGroupAhead);
      runComputation(set_IL_ROUTE_RELEASE_GROUP_AHEAD);

      runComputation(set_routeReleaseGroupsRear);
      runErrorRule(is_valid_routeReleaseGroupsRear);
      runComputation(set_routeReleaseGroupRear);
      runErrorRule(is_valid_routeReleaseGroupRear);
      runComputation(set_IL_ROUTE_RELEASE_GROUP_REAR);

      runComputation(set_routes);
      runErrorRule(is_valid_routes);
      runComputation(set_route);
      runErrorRule(is_valid_route);
      runComputation(set_ROUTE_IDS);

      runComputation(set_overlaps);
      runErrorRule(is_valid_overlaps);
      runComputation(set_overlap);
      runErrorRule(is_valid_overlap);      

      runComputation(set_routeRelations);
      runErrorRule(is_valid_routeRelations);
      runComputation(set_routeRelation);
      runErrorRule(is_valid_routeRelation);
      runComputation(set_IL_ROUTE_RELATION);

      runComputation(set_ROUTE);
      runErrorRule(validate_route);
      runWarningRule(warnings_route);      

      runComputation(set_conflictingRoutes);
      runErrorRule(is_valid_conflictingRoutes);
      runComputation(set_conflictingRoute);
      runErrorRule(is_valid_conflictingRoute);
      runComputation(set_CONFLICTING_ROUTE);      

      runComputation(set_TVD_SECTIONS);
      runComputation(set_ROUTES_TVD_SECTIONS);
      runErrorRule(validate_tvdSection);
      runWarningRule(warnings_tvdSection);

      runComputation(set_IL_OVERLAP);
      runErrorRule(validate_overlap);
      runWarningRule(warnings_overlap);

      runComputation(set_specificInfrastructureManagers);
      runErrorRule(is_valid_specificInfrastructureManagers);
      runComputation(set_specificInfrastructureManager);
      runErrorRule(is_valid_specificInfrastructureManager);
      runComputation(set_IL_SPECIFIC_INFRASTRUCTURE_MANAGER_IDS);
      runComputation(set_IL_SPECIFIC_INFRASTRUCTURE_MANAGER);

      runComputation(set_signalBoxes);
      runErrorRule(is_valid_signalBoxes);
      runComputation(set_signalBox);
      runErrorRule(is_valid_signalBox);
      runComputation(set_IL_SIGNAL_BOX_IDS);
      runComputation(set_IL_SIGNAL_BOX);
      // END RailML3_IL

      IF warnings /= {} THEN ADD_WARNING("RailML: ", warnings) ELSE skip END
      /*WHILE warnings /= {} DO
        VAR s IN
          s :: warnings;
          ADD_WARNING("RailML: ", s);
          warnings := warnings - {s}
        END
        INVARIANT btrue
        VARIANT card(warnings)
      END*/
    END
END//MACHINE