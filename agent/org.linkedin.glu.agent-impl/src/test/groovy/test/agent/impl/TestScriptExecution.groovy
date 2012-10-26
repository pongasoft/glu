/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package test.agent.impl

import org.linkedin.glu.agent.impl.script.ScriptExecution
import junit.framework.Assert
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.clock.Timespan
import org.linkedin.util.clock.SettableClock
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Clock
import java.util.concurrent.TimeoutException

/**
 * @author ypujante@linkedin.com */
class TestScriptExecution extends GroovyTestCase
{
  public static final String MODULE = TestScriptExecution.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  Clock clock = SystemClock.INSTANCE

  void testScriptExecution()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse('5s'))
    def source = new ScriptExecutionTest1(tc: tc)

    withScriptExecution(source) { ScriptExecution se ->

      // timeline: t0
      def t0 = se.clock.currentTimeMillis()

      def expectedExecutions = []

      // execute action1 with v1
      def fe1 = se.executeAction('action1',
                                 [p1: 'v1'],
                                 null)
      tc.waitForBlock('action1.v1')
      // the timeline should be empty
      assertEquals(0, se.timeline.size())
      // the future execution time should be 0 (= now)
      assertEquals(0, se.current.futureExecutionTime)
      expectedExecutions << se.current
      tc.unblock('action1.v1')
      assertEquals('v1', fe1.get('5s'))

      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      def timerFiringTime = Timespan.parse('10s').futureTimeMillis(se.clock)

      // schedule a timer with an initial frequency of 10s and a repeat frequency of 1m
      se.scheduleTimer('timer1', '10s', '1m') {
        tc.block('timer1.cancel')
      }

      // there should be one entry in the timeline (the timer)
      waitForScriptExecution(se) { se.timeline.size() == 1 }
      // should be scheduled to start in 10s
      assertEquals(timerFiringTime, se.timeline[0].futureExecutionTime)

      // timeline: t0 + 9s990
      se.clock.addDuration('9s990')

      // here the timer should not have fired yet
      shouldFail(TimeoutException) { tc.waitForBlock('timer1.0', Timespan.parse('20')) }

      // a new action is 'inserted' before the timer gets a chance to fire and it should execute
      // first
      def fe2 = se.executeAction('action1',
                                 [p1: 'v2'],
                                 null)
      tc.waitForBlock('action1.v2')
      // the timeline should have the timer only
      assertEquals(1, se.timeline.size())
      // the future execution time should be 0 (= now)
      assertEquals(timerFiringTime, se.timeline[0].futureExecutionTime)
      assertEquals(0, se.current.futureExecutionTime)
      expectedExecutions << se.current
      tc.unblock('action1.v2')
      assertEquals('v2', fe2.get('5s'))

      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 10s
      se.clock.addDuration('10')

      // now the timer should kick in
      tc.waitForBlock('timer1.0')
      assertEquals(timerFiringTime, se.current.futureExecutionTime)
      expectedExecutions << se.current
      tc.unblock('timer1.0')

      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // the next iterations happen every 1m (we do this 3 times)
      (1..3).each {
        timerFiringTime = Timespan.parse('1m').futureTimeMillis(timerFiringTime)
        waitForScriptExecution(se) { se.timeline && se.timeline[0].futureExecutionTime == timerFiringTime }
        se.clock.addDuration('59s990')
        shouldFail(TimeoutException) { tc.waitForBlock('timer1.0', Timespan.parse('20')) }
        se.clock.addDuration('10')
        tc.waitForBlock('timer1.0')
        assertEquals(timerFiringTime, se.current.futureExecutionTime)
        expectedExecutions << se.current
        tc.unblock('timer1.0')
        waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }
      }

      // timeline: t0 + 3m10s
      assertEquals("sanity check!",
                   Timespan.parse('3m10s'), 
                   new Timespan(se.clock.currentTimeMillis() - t0))

      timerFiringTime = Timespan.parse('1m').futureTimeMillis(timerFiringTime)

      def fe3 = se.executeAction('action1',
                                 [p1: 'v3'],
                                 null)
      tc.waitForBlock('action1.v3')

      def fe4 = se.executeAction('action2',
                                 [p1: 'v4']) {
        tc.block('action2.cancel')
      }

      def fe5 = se.executeAction('action1',
                                 [p1: 'v5'],
                                 null)

      // there should be 3 entries in the timeline: fe4, fe5 and timer
      waitForScriptExecution(se) { se.timeline.size() == 3 }
      assertEquals(0, se.current.futureExecutionTime)
      assertEquals(0, se.timeline[0].futureExecutionTime) // fe4
      assertEquals(0, se.timeline[1].futureExecutionTime) // fe5
      assertEquals(timerFiringTime, se.timeline[2].futureExecutionTime) // timer
      expectedExecutions << se.current

      // cancelling fe4 (before it starts)
      Thread.start { fe4.cancel(true) }

      tc.waitForBlock('action2.cancel')
      assertTrue(fe4.isCancelled())
      tc.unblock('action2.cancel')

      tc.unblock('action1.v3')
      assertEquals('v3', fe3.get('5s'))

      // timeline: t0 + 3m40s
      se.clock.addDuration('30s')

      shouldFail(TimeoutException) { tc.waitForBlock('action2.v4', Timespan.parse('20')) }

      tc.waitForBlock('action1.v5')

      // there should be 1 entry in the timeline: timer
      waitForScriptExecution(se) { se.timeline.size() == 1 }
      assertEquals(0, se.current.futureExecutionTime)
      assertEquals(timerFiringTime, se.timeline[0].futureExecutionTime) // timer
      expectedExecutions << se.current
      tc.unblock('action1.v5')
      assertEquals('v5', fe5.get('5s'))

      // timeline: t0 + 4m10s
      se.clock.addDuration('30s')
      tc.waitForBlock('timer1.0')
      expectedExecutions << se.current

      // cancelling timer1
      Thread.start { se.cancelTimer('timer1', true) }
      tc.unblock('timer1.cancel')
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // adding another minute.. the timer should not fire because it has been cancelled
      // timeline: t0 + 5m10s
      se.clock.addDuration('1m')

      waitForScriptExecution(se) { se.timeline.size() == 0 }

      assertEquals("sanity check!",
                   Timespan.parse('5m10s'), 
                   new Timespan(se.clock.currentTimeMillis() - t0))

      ////////////////////////////////
      // now we check for expiration (kicks in at 10m)

      // timeline: t0 + 6m
      se.clock.addDuration('50s')
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 9m59s990
      se.clock.addDuration('3m59s990')
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 10m1
      se.clock.addDuration('11')
      // first action removed
      expectedExecutions = expectedExecutions[1..-1]
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 3m10s1
      se.clock.addDuration('3m10s')
      // timer fired 4 times + action1.v3 + action2.v4
      expectedExecutions = expectedExecutions[6..-1]
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 3m40s1
      se.clock.addDuration('30s')
      // action1.v5
      expectedExecutions = expectedExecutions[1..-1]
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // timeline: t0 + 4m10s1
      se.clock.addDuration('30s')
      // timer1 (canceled)
      expectedExecutions = []
      waitForScriptExecution(se) { se.pastExecutions == expectedExecutions }

      // resetting ThreadControl as it gets in a weird state after being interrupted during the 
      // cancel
      tc = tc = new ThreadControl(Timespan.parse('5s'))
      source.tc = tc

      ////////////////////////////////
      // now we check for expiration (max elements based)

      // schedule a timer with a repeat frequency of 1m
      se.scheduleTimer('timer1', null, '1s') {
        tc.block('timer1.cancel')
      }

      // there should be one entry in the timeline (the timer)
      waitForScriptExecution(se) { se.timeline.size() == 1 }

      timerFiringTime = se.clock.currentTimeMillis()

      (1..30).each {
        timerFiringTime = Timespan.parse('1s').futureTimeMillis(timerFiringTime)
        waitForScriptExecution(se) { se.timeline && se.timeline[0].futureExecutionTime == timerFiringTime }
        se.clock.addDuration('1s')
        tc.waitForBlock('timer1.0')
        assertEquals(timerFiringTime, se.current.futureExecutionTime)
        expectedExecutions << se.current
        tc.unblock('timer1.0')
        if(it > se.expiryMaxElements)
          expectedExecutions = expectedExecutions[-se.expiryMaxElements..-1]
        waitForScriptExecution(se) {
          // the removeOldExpirations methods runs only when awaken... so we force awakening
          synchronized(se.lock) { se.lock.notifyAll() }
          se.pastExecutions == expectedExecutions
        }
      }

    }
  }

  void waitForScriptExecution(ScriptExecution se, Closure closure)
  {
    GroovyConcurrentUtils.waitForCondition(clock, '5s', '10', closure)
  }

  void withScriptExecution(def source, Closure closure)
  {
    if(!(source instanceof Map))
    {
      source = [invocable: source, name: name, checkValidTransitionForAction: { }]
    }
    ScriptExecution se = new ScriptExecution(source, name, log)
    se.heartbeat = Timespan.parse('2s')
    se.expiryDuration = Timespan.parse('10m')
    se.expiryMaxElements = 20
    def clock = new SettableClockWithNotification(currentTimeMillis: 100000, lock: se.lock)
    se.clock = clock
    se.start()

    try
    {
      closure(se)
    }
    finally
    {
      se.shutdown()
      se.waitForShutdown(Timespan.parse('10s').durationInMilliseconds)
    }
  }
}

class ScriptExecutionTest1
{
  ThreadControl tc

  def timer1 = { args ->
    Assert.assertNull('no args for timers', args)
    tc.block('timer1.0')
  }

  def action1 = { args ->
    tc.block("action1.${args.p1}".toString())
    return args.p1
  }

  def action2 = { args ->
    tc.block("action2.${args.p1}".toString())
    return args.p1
  }
}

class SettableClockWithNotification extends SettableClock
{
  public static final String MODULE = SettableClockWithNotification.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  def lock

  def void setCurrentTimeMillis(long currentTimeMillis)
  {
    if(log.isDebugEnabled())
      log.debug("Setting time to ${new Date(currentTimeMillis)} [${currentTimeMillis}]")

    if(lock != null)
    {
      synchronized(lock)
      {
        super.setCurrentTimeMillis(currentTimeMillis);
        lock.notifyAll()
      }
    }
    else
    {
      super.setCurrentTimeMillis(currentTimeMillis);
    }
  }

  public void addDuration(duration)
  {
    addDuration(Timespan.parse(duration.toString()))
  }
}