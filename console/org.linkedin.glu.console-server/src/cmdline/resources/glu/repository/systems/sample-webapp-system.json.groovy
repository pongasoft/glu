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
name = "Tutorial System Model"

def products = [
  product1: '1.0.0',
  product2: '2.0.0'
]

def agent = 'agent-1'

def skeleton = 'http://localhost:8080/glu/repository/tgzs/@jetty.archive@'
def war = 'http://localhost:8080/glu/repository/wars/@sample.webapp@'
def script = 'http://localhost:8080/glu/repository/scripts/org.linkedin.glu.script-jetty-@version@/JettyGluScript.groovy'


// global model metadata
metadata = [
  product: products.collectEntries { k, v -> [(k): [name: k, version: v]] }
]

// agent tags
agentTags = [
  (agent): ['osx']
]

// entries
[
  [instance: 'i001', webapps: ['cp1', 'cp2'], port: 9000, cluster: 'c1', product: 'product1', tags: ['frontend', 'webapp']],
  [instance: 'i002', webapps: ['cp1'],        port: 9001, cluster: 'c1', product: 'product1', tags: ['frontend', 'webapp']],
  [instance: 'i003', webapps: ['cp4'],        port: 9002, cluster: 'c2', product: 'product1', tags: ['backend', 'webapp']],
].each {  m ->
  entries << [
    agent: agent,
    mountPoint: "/sample/${m.instance}",
    script: script,
    initParameters: [
      skeleton: skeleton,
      port: m.port,
      webapps: m.webapps.collect { cp ->
        [
          war: war,
          contextPath: "/${cp}",
          monitor: "/monitor"
        ]
      }
    ],
    metadata: [
      cluster: m.cluster,
      container: [ name: "sample" ],
      product: m.product,
      version: products[m.product]
    ],
    tags: m.tags
  ]
}