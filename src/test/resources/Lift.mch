MACHINE Lift
VARIABLES  curfloor, set

INVARIANT  curfloor : 0..99 /* NAT */ & set <: NATURAL

INITIALISATION curfloor := 0 || set := (1..1000 \/ 2000..3000)

OPERATIONS

	increment = PRE curfloor<99 THEN curfloor := curfloor + 1 END ;
	
	decrement = BEGIN curfloor := curfloor - 1 END


	
	/*below=PRE curfloor <0 THEN curfloor := curfloor -1 END */

END
