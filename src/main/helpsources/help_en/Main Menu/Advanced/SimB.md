![SimB](../../../screenshots/SimB.png)


# General Information

SimB is a simulator built on top of ProB. 
It is available in the latest SNAPSHOT version in the new JavaFX based user interface [ProB2-UI](https://github.com/hhu-stups/prob2_ui). 
The modeler can write SimB annotations for a formal model to simulate it. 
Examples are available at https://github.com/favu100/SimB-examples. 

Furthermore, it is then possible to validate probabilistic and timing properties with 
statistical validation techniques such as hypothesis testing and estimation.

SimB also contains a feature called interactive simulation. 
This feature allows user interaction to trigger a simulation. 
For interactive simulation, a modeler has to encode SimB listeners on events, triggering a SimB simulation. 
Interactive Simulation examples are available at https://github.com/favu100/SimB-examples/tree/main/Interactive_Examples.


More recently, SimB is extended by a new feature which makes it possible to load an Reinforcement learning Agent. 
Technically, each step of the RL agent is converted into a SimB activation. 
In order to simulate a RL agent in SimB, one must (1) create a formal B model for the RL agent, and 
(2) create a mapping between the state in the RL agent and the formal model, and 
(3) provide information to the formal B model again. 
Reinforcement Learning examples are available at: https://github.com/hhu-stups/reinforcement-learning-b-models

# First Steps

## Open a Simulation

## Creating a Simulation

# Simulation with SimB

Therefore, a SimB-file in .json-format must be provided.
As soon as a SimB-file ist loaded, the machine can be animated and the resulting trace can be saved, including its timing properties.

## SimB Activation Diagram

### Default Simulation

### Probabilistic and Timing Elements

### Interactive Elements

## External Simulation

## Validation

Via the plus-button, different kinds of simulations can be added,
to validate probabilistic and timing properties of the machine:
* Monte-Carlo-simulation
* Hypothesis-test and
* Estimation

![SimB](../../../screenshots/SimulationChoice.png)

### Real-Time Simulation

### Monte Carlo Simulation

#### Hypothesis Testing

#### Estimation

## Editor for SimB Activation Diagram


Further explanation on how to create SimB-files as well as information about the different kinds of simulations can be found [here](https://prob.hhu.de/w/index.php?title=SimB).
