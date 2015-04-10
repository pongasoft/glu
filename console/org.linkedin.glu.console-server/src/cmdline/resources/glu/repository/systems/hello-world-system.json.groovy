/*
 * Copyright (c) 2013 Yan Pujante
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

fabric = "glu-dev-1"
name = "Hello World System Model"

def products = [
  product1: '1.0.0',
  product2: '2.0.0'
]

def script = "http://localhost:8080/glu/repository/scripts/org.linkedin.glu.script-hello-world-@version@/HelloWorldScript.groovy"
def agent = 'agent-1'

// global model metadata
metadata = [
  product: products.collectEntries { k, v -> [(k): [name: k, version: v]] }
]

// entries
[
  [container: 'm1', product: 'product1', cluster: 'c1'],
  [container: 'm2', product: 'product2', cluster: 'c2'],
  [container: 'm3', product: 'product1', cluster: 'c1'],
  [container: 'm4', product: 'product2', cluster: 'c1'],
].each { m ->
  entries << [
    agent: agent,
    mountPoint: "/${m.container}/i001",
    script: script,
    initParameters: [ message: 'Hello World' ],
    metadata: [
      cluster: m.cluster,
      container: [ name: m.container ],
      product: m.product,
      version: products[m.product]
    ]
  ]
}
