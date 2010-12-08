#
# Copyright 2010-2010 LinkedIn, Inc
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

__author__  = [ "Manish Dubey", "Dan Sully" ]
__version__ = "0.1"

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

    def __init__(self, fabric="dev", url="http://localhost:8080", username="glua", password="password", version="v1"):
        self.uriBase = url
        self.uriPath = "/console/rest/" + version + "/" + fabric
        self.auth    = restkit.BasicAuth(username, password)

        # Toggle logging for restkit
        if logging.getLogger('gluconsole.rest.Client').level == logging.DEBUG:
            logging.getLogger('restkit').setLevel(logging.DEBUG)
            use_progressbar = False

    # Private method:
    # Actually perform the HTTP request and return the results
    def _doRequest(self, action, method, body = None):

        url = urlparse.urljoin(self.uriBase, self.uriPath + '/' + action)

        log.debug("URL via (%s): %s" % (method, url))

        headers = {
            'User-Agent': 'Python-GLU-REST-Client-v%f' % 0.1
        }

        if body and len(body) > 0:
            log.debug("Form body: [%s]" % body)

            headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=utf-8'

        if method in [ "GET", "HEAD", "DELETE", "POST", "PUT" ]:
            response = restkit.request(url, method = method, body = body, headers = headers, filters = [self.auth])
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

    def generateSystemFilter(self, appname = None, cluster = None, host = None, instance = None):
        """
          Create a GLU systemFilter string

          :param action: Action to perform: start, stop, bounce, deploy, undeploy, redeploy
          :param appname: Appnames filter.
          :param cluster: Clusters filter.
          :param host: Host filter.
          :param instance: Instance filter.
        """

        filter = None

        if appname:
            filter = "metadata.container.name='%s'" % appname

        elif cluster:
            filter = "metadata.cluster='%s'" % cluster

        elif host:
            filter = "agent='%s'" % host

        elif instance:
            filter = "key='%s'" % instance

        return filter

    def generateActionRequest(self, action, systemFilter, parallel = False):
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
                     ' ', progressbar.Bar(marker='*',left='[',right=']'),
                     ' ', progressbar.ETA(), ' ']

          progress = progressbar.ProgressBar(widgets = widgets, maxval = 100)
          progress.start()

        while 1:
            progressStatus  = self._doRequest(statusUrl, "HEAD")
            completeStatus  = progressStatus['x-linkedin-glu-completion']
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

    def executePlan(self, action, systemFilter, parallel = False, dryrun = False):
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
        plan   = self._doRequest("plans", "POST", filter)

        return self._executePlan(plan, dryrun)

    def loadModel(self, release, rootUrl, product):
        """
          Load a manifest into the console.

          :param release: Release of the model to load.
          :param rootUrl: Location of the model to load.
          :param product: Product to load the model into.
        """

        params = {
            'release': release,
            'rootUrl': rootUrl,
            'product': product,
        }

        response = self._doRequest("system/model", "PUT", params)
        status   = response.status_int

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

    def status(self, live = True, legacy = False, beautify = False, filter = None):
        """
          Retrieve the model, either currently loaded or live as JSON.

          :param live: If True, retrieve the live model. Otherwise the loaded model.
          :param legacy: If True, keep the legacy section in the output.
          :param beautify: If True, use a pretty printer on the output.
          :param filter: The DSL/filter syntax for the GLU console to parse.
        """

        if live:
            uri = "system/live"
        else:
            uri = "system/model"

        params = {}

        if beautify:
            params['prettyPrint'] = 'true'
        if legacy:
            params['legacy'] = 'true'
        if filter:
            params['systemFilter'] = filter

        if len(params) > 0:
            uri = uri + "?" + urllib.urlencode(params)

        response = self._doRequest(uri, "GET")

        return response.body
