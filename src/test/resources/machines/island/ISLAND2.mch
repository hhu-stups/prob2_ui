MACHINE ISLAND2

CONSTANTS
    maxCars
PROPERTIES
    maxCars = 5

VARIABLES
    cars,
    bark
INVARIANT
    cars : NAT & bark : BOOL
INITIALISATION
    cars := 0 || bark := FALSE

OPERATIONS

    openBark = PRE cars < maxCars THEN bark := TRUE END;

    on = PRE bark = TRUE THEN cars := cars + 1 || bark := FALSE END;

    off = PRE cars > 0 THEN cars := cars - 1 END


END