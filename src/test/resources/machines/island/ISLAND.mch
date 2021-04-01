MACHINE ISLAND

CONSTANTS
    maxCars
PROPERTIES
    maxCars = 5

VARIABLES
    cars
INVARIANT
    cars : NAT
INITIALISATION
    cars := 0

OPERATIONS
    on = PRE cars < maxCars THEN cars := cars + 1 END;

    off = PRE cars > 0 THEN cars := cars - 1 END

END