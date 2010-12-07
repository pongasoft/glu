Agent
-----
The GLU agent is an active process that needs to run on every host where applications need to be deployed

Closure
-------
In a GLU script, a closure is a [groovy closure](http://groovy.codehaus.org/Closures) which is essentially a piece of groovy/java code (between curly braces) assigned to an attribute.

Console
-------
The webapp/REST api built on top of ZooKeeper which is the orchestrator of the system.

Fabric
------
A fabric defines a group of agents.

GLU script
----------
A GLU script is a set of instructions backed by a state machine that the agent knows how to run.

Metadata
--------
Metadata in the context of GLU represents a map that can be represented as a json object.

    def goodMetadata = 
    [
      p1: 'v1',
      p2: [1, 2, 3], // array
      p3: [p31: 'v31'] // another nested map
    ]

    // bad because the value is a java object
    def badMetadata =
    [
      color: java.awt.Color.BLACK
    ]

Mount Point
-----------
The unique key on which a GLU scripts get 'mounted' on a given agent. It is a String which has a path like syntax (must start with a '/'). 

    Example: /a/b/c

ZooKeeper
---------
View more information about [ZooKeeper](http://hadoop.apache.org/zookeeper/)