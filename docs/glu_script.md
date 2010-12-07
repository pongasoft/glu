Introduction
============
A GLU script is a set of instructions backed by a state machine that the agent knows how to run. In general, and by default, a GLU script represents the set of instructions which defines the lifecycle of what it means to install, configure, start, stop, unconfigure and uninstall an application.

Groovy Class
============

<img src="https://github.com/linkedin/linkedin.github.com/raw/master/images/glu/MyGluScript.png" alt="MyGluScript" style="align: center">

A GLU script is a groovy class which contains a set of closures where the name of each closure matches the name of the actions defined in the state machine. This example shows the default closure names. The script can also store state in attributes (like port and pid in this example). The code of each closure can be any arbitrary groovy/java code but remember that the agent offers some capabilities to help you in writing more concise code.

State machine
=============
Each GLU script is backed by a state machine which is an instance of `org.linkedin.groovy.util.state.StateMachine`. The default state machine is the following:

<img src="https://github.com/linkedin/linkedin.github.com/raw/master/images/glu/state_machine_diagram.png" alt="State Machine diagram" style="align: center">

This is how the default state machine is defined.

<img src="https://github.com/linkedin/linkedin.github.com/raw/master/images/glu/state_machine.png" alt="State Machine Definition" style="align: center">


You can define your own state machine by defining a static attribute called stateMachine

This is how the `AutoUpgradeScript` GLU script defines different states and actions:

    class AutoUpgradeScript {
      def static stateMachine =
      [
          NONE: [ [to: 'installed', action: 'install'] ],
          installed: [ [to: 'NONE', action: 'uninstall'], [to: 'prepared', action: 'prepare'] ],
          prepared: [ [to: 'upgraded', action: 'commit'], [to: 'installed', action: 'rollback'] ],
          upgraded: [ [to: 'NONE', action: 'uninstall'] ]
      ]
    …
    }

The minimum (usefull) state machine that you can define could look like:

    def static stateMachine =
    [
        NONE: [ [to: 'running', action: 'start'] ],
        running: [ [to: 'NONE', action: 'stop'] ]
    ]

Note: If an action is empty you don't even have to define its equivalent action but you still need to call all prior actions to satisfy the state machine.

An example of GLU script
========================

<img src="https://github.com/linkedin/linkedin.github.com/raw/master/images/glu/glu_script_example.png" alt="GLU script example" style="align: center">

