/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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

package test.agent.perf

/**
 * Example of running the performance test:
 * groovy groovy/test/agent/perf/TestPerfTimers.groovy /export/content/glu/org.linkedin.glu.packaging-all-xxx/bin/agent-cli.sh resources/TimersTestScript.groovy
 * @author ypujante@linkedin.com */
class TestPerfTimers
{
  File gluc
  File scriptFile
  String server = "https://localhost:12906"
  //String server = "https://localhost:13906"

  int nbThreads = 5

  def threads = []

  boolean shutdown = false

  Random rnd = new Random()

  void run()
  {

    (1..nbThreads).each { i ->
      threads << Thread.start {
        long iterationsCount = 0
        def mountPoint = "/script/${i}"

        while(!shutdown)
        {
          if(!shutdown) installScript(mountPoint)
          if(!shutdown) executeAction(mountPoint, 'install')
          if(!shutdown) executeAction(mountPoint, 'configure', '[repeatFrequency:50]')
          if(!shutdown) Thread.sleep(rnd.nextInt(1500) + 500)
          if(!shutdown) uninstallScript(mountPoint)
          println "${new Date()}: ${i} completed iteration #${++iterationsCount} "
        }
        uninstallScript(mountPoint)
        println "${new Date()}: ${i} done"
      }
    }

    addShutdownHook {
      println "Terminating..."
      shutdown = true
      threads.each { it.join() }
    }

    threads.each { it.join() }
    
    println "Terminated."
  }

  private String executeAction(mountPoint, action)
  {
    executeCommand("${gluc} -s ${server} -m ${mountPoint} -e ${action}")
  }

  private String executeAction(mountPoint, action, actionArgs)
  {
    executeCommand("${gluc} -s ${server} -m ${mountPoint} -e ${action} -a ${actionArgs}")
  }

  private String installScript(mountPoint)
  {
    executeCommand("${gluc} -s ${server} -m ${mountPoint} -i ${scriptFile.toURI()}")
  }

  private String uninstallScript(mountPoint)
  {
    executeCommand("${gluc} -s ${server} -m ${mountPoint} -U")
  }

  private String executeCommand(String command)
  {
    def out
    def err
    def process = command.execute()

    Thread.start {
      out = process.in.text
    }

    Thread.start {
      err = process.err.text
    }

    if(process.waitFor() != 0)
    {
      print "${new Date()}: ${command}: ${err}"
      return err.toString()
    }
    
    return null
  }

  public static void main(String[] args)
  {
    def tpt = new TestPerfTimers(gluc: new File(args[0]),
                                 scriptFile: new File(args[1]))
    tpt.run()
  }
}
