/*
 * Copyright 2010-2010 LinkedIn, Inc
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

class Script1
{
  long timersCount = 0

  def timer1 = {
    timersCount++

    if(timersCount % 20 == 0)
      log.info "From timer1 ${state}! ${timersCount}"
  }
  def install = {
    log.info "install..."
  }
  
  def configure = { args ->
    log.info "scheduling timer1"
    timers.schedule(timer: timer1, repeatFrequency: args.repeatFrequency ?: '2s')
  }
  
  def unconfigure = {
    log.info "stopping timer1"
    timers.cancel(timer: timer1)
  }
}