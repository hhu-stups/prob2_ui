MACHINE RailML3_import
INCLUDES RailML3_validation_flat
DEFINITIONS
  "LibraryIO.def";
  "LibraryMeta.def";
  "LibraryStrings.def";
  "SORT.def";
  SET_PREF_ALLOW_OPERATION_CALLS_IN_EXPRESSIONS == TRUE;

  runErrorRule(Rule) ==
    IF no_error = TRUE THEN
      VAR Var1, Var2 IN
        Var1, Var2 <-- Rule;
        IF Var1 = "SUCCESS" THEN skip
        ELSE
          no_error := FALSE;
          ADD_ERROR("RailML: ", "File import aborted, the specified file does not contain valid railML.");
          VAR errors IN
            errors := makeRailMLMessages(Var2[{1}]);
            ADD_ERRORS(errors);
            ADD_STATE_ERRORS(errors)
          END
        END
      END
    ELSE skip END;
  runWarningRule(Rule) ==
    IF no_error = TRUE THEN
      VAR Var1, Var2 IN
        Var1, Var2 <-- Rule;
        IF Var1 = "SUCCESS" THEN skip
        ELSE
          defaultWarnings := defaultWarnings \/ Var2[{1}];
          semanticWarnings := semanticWarnings \/ Var2[{2}]
        END
      END
    ELSE skip END;
  runComputation(Computation) == IF no_error = TRUE THEN Computation ELSE skip END;

  makeRailMLMessages(SetOfStringMessages) == dom({ msg, orig_msg | orig_msg : SetOfStringMessages & msg = STRING_APPEND("RailML: ", orig_msg) })
VARIABLES no_error, defaultWarnings, semanticWarnings
INVARIANT no_error : BOOL & {defaultWarnings, semanticWarnings} <: FIN(STRING)
INITIALISATION no_error := TRUE; defaultWarnings := {}; semanticWarnings := {}
OPERATIONS
  importRailML(full_import) = 
    BEGIN
      // RailML_readFile
      runErrorRule(is_supported_railml);
      runErrorRule(unique_ids);
      runComputation(read_file);

      // BEGIN RailML3_CO
      runComputation(set_common);
      runErrorRule(is_valid_common);

      runComputation(set_name);
      runErrorRule(is_valid_name);
      runComputation(set_CO_NAME);

      runComputation(set_speedProfiles);
      runErrorRule(is_valid_speedProfiles);
      runComputation(set_speedProfile);
      runErrorRule(is_valid_speedProfile);
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

      runComputation(set_balises);
      runErrorRule(is_valid_balises);
      runComputation(set_balise);
      runErrorRule(is_valid_balise);

      runComputation(set_baliseGroups);
      runErrorRule(is_valid_baliseGroups);
      runComputation(set_baliseGroup);
      runErrorRule(is_valid_baliseGroup);

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

      runComputation(set_speeds);
      runErrorRule(is_valid_speeds);
      runComputation(set_speedSection);
      runErrorRule(is_valid_speedSection);  

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
          
      runComputation(set_NET_RELATION_SUBSEQUENT_LOCATIONS);
      runErrorRule(validate_NET_RELATION_SUBSEQUENT_LOCATIONS);
      runComputation(set_SIGNAL);
      runErrorRule(validate_linearLocation);

      runComputation(set_DERAILER);
      runComputation(set_SPEED_SECTION);
      runComputation(set_TRACK);

      runErrorRule(validate_border);
      runErrorRule(validate_bufferStop);
      runErrorRule(validate_track);
      runWarningRule(warnings_track);

      runComputation(set_geometry);
      runErrorRule(is_valid_geometry);

      runComputation(set_geometryPoints);
      runErrorRule(is_valid_geometryPoints);
      runComputation(set_geometryPoint);
      runErrorRule(is_valid_geometryPoint);

      runComputation(set_gradientCurves);
      runErrorRule(is_valid_gradientCurves);
      runComputation(set_gradientCurve);
      runErrorRule(is_valid_gradientCurve);

      runComputation(set_horizontalCurves);
      runErrorRule(is_valid_horizontalCurves);
      runComputation(set_horizontalCurve);
      runErrorRule(is_valid_horizontalCurve);
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
      runComputation(set_TVD_SECTIONS);
      runErrorRule(validate_tvdSection);
      runWarningRule(warnings_tvdSection);   

      runComputation(set_derailersIL);
      runErrorRule(is_valid_derailersIL);
      runComputation(set_derailerIL);
      runErrorRule(is_valid_derailerIL);

      runComputation(set_movableCrossings);
      runErrorRule(is_valid_movableCrossings);
      runComputation(set_movableCrossing);
      runErrorRule(is_valid_movableCrossing);  

      runComputation(set_signalsIL);
      runErrorRule(is_valid_signalsIL);
      runComputation(set_signalIL);
      runErrorRule(is_valid_signalIL);
      runComputation(set_IL_SIGNAL);
      runErrorRule(validate_signalIL);

      IF full_import = FALSE THEN skip
      ELSE
        runComputation(set_switchesIL);
        runErrorRule(is_valid_switchesIL);
        runComputation(set_switchIL);
        runErrorRule(is_valid_switchIL);      

        runComputation(set_IL_MOVABLE_CROSSING);
        runErrorRule(validate_movableCrossing);
        runWarningRule(warnings_movableCrossing);

        runComputation(set_IL_DERAILER);
        runErrorRule(validate_derailerIL);
        runWarningRule(warnings_derailerIL);

        runComputation(set_IL_SWITCH);
        runErrorRule(validate_switchIL);
        runWarningRule(warnings_switchIL);

        runComputation(set_routeReleaseGroupsAhead);
        runErrorRule(is_valid_routeReleaseGroupsAhead);
        runComputation(set_routeReleaseGroupAhead);
        runErrorRule(is_valid_routeReleaseGroupAhead);

        runComputation(set_routeReleaseGroupsRear);
        runErrorRule(is_valid_routeReleaseGroupsRear);
        runComputation(set_routeReleaseGroupRear);
        runErrorRule(is_valid_routeReleaseGroupRear);

        runComputation(set_routes);
        runErrorRule(is_valid_routes);
        runComputation(set_route);
        runErrorRule(is_valid_route);

        runComputation(set_overlaps);
        runErrorRule(is_valid_overlaps);
        runComputation(set_overlap);
        runErrorRule(is_valid_overlap);

        runComputation(set_routeRelations);
        runErrorRule(is_valid_routeRelations);
        runComputation(set_routeRelation);
        runErrorRule(is_valid_routeRelation);
        runComputation(set_IL_ROUTE_RELATION);
        runErrorRule(validate_routeRelation);
        runWarningRule(warnings_routeRelation);

        runComputation(set_ROUTE);
        runErrorRule(validate_route);
        runWarningRule(warnings_route);

        runComputation(set_IL_OVERLAP);
        runErrorRule(validate_overlap);
        runWarningRule(warnings_overlap);

        runComputation(set_conflictingRoutes);
        runErrorRule(is_valid_conflictingRoutes);
        runComputation(set_conflictingRoute);
        runErrorRule(is_valid_conflictingRoute);
        runComputation(set_CONFLICTING_ROUTE);         

        runComputation(set_IL_ROUTE_RELEASE_GROUP_AHEAD);
        runErrorRule(validate_routeReleaseGroupAhead);
        runWarningRule(warnings_routeReleaseGroupAhead);

        runComputation(set_IL_ROUTE_RELEASE_GROUP_REAR);
        runErrorRule(validate_routeReleaseGroupRear);
        runWarningRule(warnings_routeReleaseGroupRear);

        runComputation(set_specificInfrastructureManagers);
        runErrorRule(is_valid_specificInfrastructureManagers);
        runComputation(set_specificInfrastructureManager);
        runErrorRule(is_valid_specificInfrastructureManager);
        runComputation(set_IL_SPECIFIC_INFRASTRUCTURE_MANAGER);

        runComputation(set_signalBoxes);
        runErrorRule(is_valid_signalBoxes);
        runComputation(set_signalBox);
        runErrorRule(is_valid_signalBox);
        runComputation(set_IL_SIGNAL_BOX);
        runWarningRule(warnings_signalBox)
      END;
      // END RailML3_IL
      
      IF defaultWarnings /= {} THEN
        ADD_WARNINGS(makeRailMLMessages(defaultWarnings \/ {"DEFAULT VALUES:"} \/ {"----------------------------------------------------------------"}))
      ELSE skip END;
      IF semanticWarnings /= {} THEN
        ADD_WARNINGS(makeRailMLMessages(semanticWarnings \/ {"SEMANTIC WARNINGS:"}))
      ELSE skip END
    END
END//MACHINE