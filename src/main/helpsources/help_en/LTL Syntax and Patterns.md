([Go back to Verifications](Verification.md))

## Summary of LTL Syntax supported by ProB

| Expression                            | Use                                                               |
|---------------------------------------|-------------------------------------------------------------------|
| {...}                                 | contains B predicates                                             |
| e(op)                                 | check if an operation op is enabled                               |
| deadlock                              | check if a state is deadlocked                                    |
| deadlock(op1,...,opk)  (with k>0)     | check if all operations in the brackets are disabled              |
| controller(op1,...,opk) (with k>0)    | check if exactly one of the operations in the brackets is enabled |
| deterministic(op1,...,opk) (with k>0) | check if maximum one of the operations in the brackets is enabled |
| sink                                  | check if no operation is enabled that leads to another state      |
| brackets                              | check what is the next operation, e.g. [reset] => X{db={}}        |

*   G,F,X,U,W,R,true,false,not,&,or and => are part of the supported LTL syntax,
*   Past-LTL is supported: Y,H,O,S,T are the duals to X,G,F,U,R.

<br>

#### Setting Fairness Constraints
*   Give fairness constraints by means of implication: fair => f, where "fair" are the fairness constraints and "f" is LTL-formula intended to be checked.
*   Use WF(-) and SF(-) to set action-based weak and strong fairness constraints, respectively,
*   use WEF and SEF to search for bad paths that are weakly and strongly fair with respect to all transitions, respectively.

<br>

## Summary of LTL Patterns supported by ProB

* Supported types: num (non-negative whole numbers), seq (sequences), var (LTL formulae)
* Definition of variables: `<type>` `<identifier>`: `<value>`
* Assignment of variables: `<identifier>`: `<value>`
* Scopes for variables: Loop, pattern, global (a lookup for a variable checks the inner scopes first until the variable is found)

* Definition of a Pattern:

```
	def `<name>` ( `<parameters>` ):
		`<body>`
```



> Each parameter is of the form:
* `<identifier>` : `<type>` for num and seq variables
* `<identifier>` for var variables

>The parameters are separated by a comma. The body contains statements such as definition or assignment of variables as well as loops. Patterns can be overloaded.

* Pattern Invocation: `<name>`( `<arguments>` )

>Remark: Pattern Invocations do not depend on the order of the definitions of the used patterns. So patterns can be invoked before they are defined.
Patterns can only be defined in the global scope. It is not possible to define patterns within other patterns.

* Loops:

```
	count `<identifier>`: `<start>` `<up/down>` to `<end>`:
		`<body>`
	end
```

>The body must at least define or assign a variable.

* Definition of a sequence: ( `<formulae>` )

>A sequence contains many formulae that must be true in different states. It is not required that the states must come one after another.
<formulae> must at least contain two formulae.

* Definition of a sequence with condition:

( `<formulae>` without `<condition>` )
`<identifier>` without `<condition>`

>The first use case extends the definition of a sequence by an additional condition. The second use case requires <identifier> to be a name
of a variable with the datatype seq. The sequence stored in the variable are used to define a new sequence where the condition is true.

* Invocation of a sequence: seq( `<argument>` )

><argument> is a variable of the type seq or a definition of a sequence. Invoking a sequence returns a LTL formula.

* Scopes:

>before(`<right endpoint>`, `<property>`)
after(`<left endpoint>`, `<property>`)
between(`<left endpoint>`, `<right endpoint>`, `<property>`)
after_until(`<left endpoint>`, `<right endpoint>`, `<property>`)

>The endpoints and properties must be regular LTL formulae. The return value of a scope is a regular LTL formula where the given conditions are true.

* One-line comment : // comment
* Multiline comment: /* comment */

* Examples:

Example with LTL formula:

```
//To describe a portion of a system's execution which contains only states that have a desired property. Also known as Henceforth and Always.

def universality(p):
  G(p)
```

Example with loops and overloading:

```
//To describe a portion of a system's execution that contains an instance of certain events or states. Also known as Eventually.
//With a given n, you can describe, that a certain state can occur at most n-times.

def existence(p):
  F(p)

def existence(p, n : num):
  var result: G(!p)
  count 0 up to n:
	result: !p W (p W result)
  end
  result
```

Example with sequence invocation and overloading:

```
//To describe cause-effect relationships between a pair of events/states. An occurrence of the first, the cause, must be followed by an occurrence of the second, the effect. Also known as Follows and Leads-to.
//With a given sequence of states, you can describe, that e.g. the sequence of states follows a certain state.

def response(s, p):
  G(p => F(s))
	
def response(s : seq, p):
  G(p => F(seq(s)))
	
def response(s, p : seq):
  G(seq(p) => F(s))
```

[Back](Verification.md)
