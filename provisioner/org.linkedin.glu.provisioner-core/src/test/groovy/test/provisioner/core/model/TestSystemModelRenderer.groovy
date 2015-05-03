/*
 * Copyright (c) 2012-2015 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package test.provisioner.core.model

import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.JsonSystemModelRenderer

/**
 * @author yan@pongasoft.com */
public class TestSystemModelRenderer extends GroovyTestCase
{

  public void testPrint()
  {
    def model = m([agent: 'a1', mountPoint: '/m2', script: 's1', initParameters: [z: 1, b: 2]],
                  [agent: 'a1', mountPoint: '/m1', script: 's1'],
                  [agent: 'a1', mountPoint: '/m3', script: 's2', initParameters: [ip1: "iv1", ip0: "iv0"]])

    // under the cover glu uses LinkedHashMap so the order is not random in compact print which
    // respects the order of the map provided! It is just following the order in the code
    // (see SystemModel.toExternalRepresentation() and SystemEntry.toExternalRepresentation())
    assertEquals('{"fabric":"f1","entries":[{"agent":"a1","script":"s1","mountPoint":"/m1"},{"agent":"a1","script":"s1","mountPoint":"/m2","initParameters":{"z":1,"b":2}},{"agent":"a1","script":"s2","mountPoint":"/m3","initParameters":{"ip1":"iv1","ip0":"iv0"}}]}', new JsonSystemModelRenderer().compactPrint(model))

    // the canonical representation sorts the keys (including the ones nested!)
    assertEquals('{"entries":[{"agent":"a1","mountPoint":"/m1","script":"s1"},{"agent":"a1","initParameters":{"b":2,"z":1},"mountPoint":"/m2","script":"s1"},{"agent":"a1","initParameters":{"ip0":"iv0","ip1":"iv1"},"mountPoint":"/m3","script":"s2"}],"fabric":"f1"}',
                 new JsonSystemModelRenderer().canonicalPrint(model))

    // the pretty printed representation is like the canonical representation but with indentation
    // and line returns
    assertEquals("""{
  "entries": [
    {
      "agent": "a1",
      "mountPoint": "/m1",
      "script": "s1"
    },
    {
      "agent": "a1",
      "initParameters": {
        "b": 2,
        "z": 1
      },
      "mountPoint": "/m2",
      "script": "s1"
    },
    {
      "agent": "a1",
      "initParameters": {
        "ip0": "iv0",
        "ip1": "iv1"
      },
      "mountPoint": "/m3",
      "script": "s2"
    }
  ],
  "fabric": "f1"
}""", new JsonSystemModelRenderer().prettyPrint(model))
  }

  /**
   * Make sure that the backward compatibility flag kicks in!
   */
  public void testSystemIdComputation()
  {
    def model = m([agent: 'a1', mountPoint: '/m2', script: 's1', initParameters: [z: 1, b: 2]],
                  [agent: 'a1', mountPoint: '/m1', script: 's1'],
                  [agent: 'a1', mountPoint: '/m3', script: 's2', initParameters: [ip1: "iv1", ip0: "iv0"]])

    assertEquals('e66425e1e4f005c106c05cc2a018a2f3d8cd3aaa',
                 new JsonSystemModelRenderer().computeSystemId(model))
  }

  private SystemModel m(Map... entries)
  {
    SystemModel model = new SystemModel(fabric: "f1")

    entries.each {
      model.addEntry(SystemEntry.fromExternalRepresentation(it))
    }

    return model
  }

}