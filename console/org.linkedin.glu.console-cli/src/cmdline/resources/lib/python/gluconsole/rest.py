#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2011 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

__author__ = ["Manish Dubey", "Dan Sully", "Yan Pujante"]
__version__ = "@console.version@"

import re
import sys
import time
import logging
import restkit
import urllib
import urlparse

try:
  import progressbar

  if sys.stdout.isatty():
    use_progressbar = True
  else:
    use_progressbar = False

except ImportError:
  use_progressbar = False

log = logging.getLogger("gluconsole.rest.Client")
log.setLevel(logging.INFO)

class Client:
  """ A REST wrapper to talk to GLUConsole """

  def __init__(self, fabric="dev", url="http://localhost:8080", username="glua", password="password"
               , version="v1"):
    self.uriBase = url
    self.uriPath = "/console/rest/" + version + "/" + fabric
    self.auth = restkit.BasicAuth(username, password)

    # Toggle logging for restkit
    if logging.getLogger('gluconsole.rest.Client').level == logging.DEBUG:
      logging.getLogger('restkit').setLevel(logging.DEBUG)
      use_progressbar = False

  # Private method:
  # Actually perform the HTTP request and return the results
  def _doRequest(self, action, method, body=None, headers=None):
    url = urlparse.urljoin(self.uriBase, self.uriPath + '/' + action)

    log.debug("URL via (%s): %s" % (method, url))

    if headers is None:
      headers = {}

    headers['User-Agent'] = '@console.name@-@console.version@'

    if method in ["GET", "HEAD", "DELETE", "POST", "PUT"]:
      response = restkit.request(url, method=method, body=body, headers=headers,
                                 filters=[self.auth])
    else:
      raise restkit.errors.InvalidRequestMethod("Unsupported Method")

    if response.status_int >= 400:
      if response.status_int == 404:
        raise restkit.errors.ResourceNotFound(response.body_string())
      elif response.status_int == 401:
        raise restkit.errors.Unauthorized("Unauthorized Request")
      else:
        raise restkit.errors.RequestFailed(response.body_string(), response.status_int, response)

    # body_string() can only be called once, since it's a socket read, or
    # will throw an AlreadyRead exception.
    response.body = response.body_string()

    return response

  def generateSystemFilter(self, agent=None, instance=None, allTags=None, anyTag=None):
    """
      Create a GLU systemFilter string

      :param action: Action to perform: start, stop, bounce, deploy, undeploy, redeploy
      :param agent: agent filter.
      :param instance: Instance filter.
      :param allTags: all tags filter.
      :param anyTag: any tag filter.
    """

    filter = None

    if agent:
      filter = "agent='%s'" % agent

    elif instance:
      filter = "key='%s'" % instance

    elif allTags:
      filter = "tags='%s'" % allTags

    elif anyTag:
      filter = "tags.hasAny('%s')" % anyTag

    return filter

  def generateActionRequest(self, action, systemFilter, parallel=False):
    """
      Create a GLU action string. This contains a systemFilter, as well as an action, and order.

      :param action: Action to perform: start, stop, bounce, deploy, undeploy, redeploy
      :param systemFilter: Filter as created from generateSystemFilter.
      :param parallel: True to run stop in parallel, False (default) for serial.
    """

    filters = []
    filters.append("planAction=" + action)

    # An empty/None filter implies 'all'
    if systemFilter:
      filters.append("systemFilter=" + restkit.util.url_quote(systemFilter))

    if parallel:
      filters.append("order=parallel")
    else:
      filters.append("order=sequential")

    filter = "&".join(filters)

    return restkit.util.to_bytestring(filter)

  # Private method:
  # executing a created plan requires several REST calls, so this wraps all of them into one.
  def _executePlan(self, createdPlan, dryrun):
    if createdPlan.status_int == 204:
      return "GLU Console message: %s" % createdPlan.status.split(' ', 1)[-1]

    url2UriPat = "https?://[-\.\w:]*" + self.uriPath + "/"

    # unique identifier for the plan just created
    planUrl = createdPlan['location']
    planUrl = re.sub(url2UriPat, "", planUrl)
    log.debug("plan url = " + planUrl)

    # inspect execution plan here, if you need
    execPlan = self._doRequest(planUrl, "GET")
    log.debug("body = " + execPlan.body)

    if dryrun:
      return execPlan.body

    # execute the plan
    planUrl += "/execution"
    log.info("executing plan: " + planUrl)
    planStatus = self._doRequest(planUrl, "POST")

    # check status of plan execution
    statusUrl = planStatus['location']
    statusUrl = re.sub(url2UriPat, "", statusUrl)
    log.info("status url = " + statusUrl)

    # wait until plan is 100% executed.
    completed = re.compile("^100")

    if use_progressbar:
      widgets = [' ', progressbar.Percentage(),
                 ' ', progressbar.Bar(marker='*', left='[', right=']'),
                 ' ', progressbar.ETA(), ' ']

      progress = progressbar.ProgressBar(widgets=widgets, maxval=100)
      progress.start()

    while 1:
      progressStatus = self._doRequest(statusUrl, "HEAD")
      completeStatus = progressStatus['x-glu-completion']
      percentComplete = re.split(':', completeStatus)

      if not completed.match(completeStatus):
        if use_progressbar:
          progress.update(int(percentComplete[0]))
        else:
          log.info("InProgress: " + percentComplete[0] + "% complete")

      else:
        if use_progressbar:
          progress.finish()
        else:
          log.info("Completed : " + completeStatus)

        break

      time.sleep(2)

    return completeStatus

  def executePlan(self, action, systemFilter, parallel=False, dryrun=False):
    """
      Run the a plan command against the console.

      :param action: Action to perform: start, stop, bounce, deploy, undeploy, redeploy
      :param systemFilter: Filter as created from generateSystemFilter.
      :param parallel: True to run stop in parallel, False (default) for serial.
      :param dryrun: Create the plan, but don't execute it.
    """

    if action not in ("start", "stop", "bounce", "deploy", "undeploy", "redeploy"):
      raise StandardError("Action %s is invalid." % action)

    filter = self.generateActionRequest(action, systemFilter, parallel)
    headers = {
      'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
    }
    plan = self._doRequest("plans", "POST", filter, headers)

    return self._executePlan(plan, dryrun)

  def loadModel(self, modelUrl=None, modelFile=None):
    """
      Load a model into the console.

      :param modelUrl: to load the model from a model
      :param modelFile: to load a model directly from a file
    """

    if (modelUrl is None) and (modelFile is None):
      raise StandardError("modelUrl or modelFile must be provided")

    status = None

    if modelUrl is not None:
      body = {
        'modelUrl': modelUrl
      }
      response = self._doRequest("model/static", "POST", body)
      status = response.status_int
    else:
      with open(modelFile, 'r') as mf:
        headers = {
          'Content-Type': 'text/json'
        }
        response = self._doRequest("model/static", "POST", mf, headers)
        status = response.status_int

    # XXX - Should exceptions be thrown here?
    if status == 201:
      return "Model loaded successfully: " + response.body
    elif status == 204:
      return "Model applied, but was not updated."
    elif status == 400:
      #"Error: Invalid model."
      return ""
    elif status == 404:
      #"Error: model not found."
      return ""
    else:
      return ""

  def status(self, live=True, beautify=False, filter=None):
    """
      Retrieve the model, either currently loaded or live as JSON.

      :param live: If True, retrieve the live model. Otherwise the loaded model.
      :param beautify: If True, use a pretty printer on the output.
      :param filter: The DSL/filter syntax for the GLU console to parse.
    """

    if live:
      uri = "model/live"
    else:
      uri = "model/static"

    params = {}

    if beautify:
      params['prettyPrint'] = 'true'
    if filter:
      params['systemFilter'] = filter

    if len(params) > 0:
      uri = uri + "?" + urllib.urlencode(params)

    response = self._doRequest(uri, "GET")

    return response.body
