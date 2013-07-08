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
# Minimal Python version is 2.5

"""A REST wrapper to talk to GLUConsole."""

__author__ = ['Manish Dubey', 'Dan Sully', 'Yan Pujante']
__version__ = '@console.version@'

import logging
import re
import restkit
import sys
import time
import urllib
import urlparse


try:
    import progressbar
    use_progressbar = True if sys.stdout.isatty() else False
except ImportError:
    use_progressbar = False


logger = logging.getLogger('gluconsole.rest.Client')    # pylint: disable=C0103
logger.setLevel(logging.INFO)


class Client:
    """A REST wrapper to talk to GLUConsole"""

    def __init__(self, fabric='dev', url='http://localhost:8080',
            username='glua', password='password', version='v1'):
        self.uri_base = url
        self.uri_path = 'console/rest/%s/%s' % (version, fabric)
        self.auth = restkit.BasicAuth(username, password)
        self._action_successful = None

        # Toggle logging for restkit
        if logging.getLogger('gluconsole.rest.Client').level == logging.DEBUG:
            logging.getLogger('restkit').setLevel(logging.DEBUG)
            use_progressbar = False

    @property
    def action_successful(self):
        """Return True if the latest executed action succeeded."""
        return self._action_successful

    # Private method:
    # Actually perform the HTTP request and return the results
    def _do_request(self, action, method, body=None, headers=None):
        url = urlparse.urljoin(self.uri_base, self.uri_path + '/' + action)

        logger.debug('URL via (%s): %s' % (method, url))

        if headers is None:
            headers = {}

        headers['User-Agent'] = '@console.name@-@console.version@'

        if method in ('GET', 'HEAD', 'DELETE', 'POST', 'PUT'):
            response = restkit.request(url, method=method, body=body,
                                       headers=headers, filters=[self.auth])
        else:
            raise restkit.errors.InvalidRequestMethod('Unsupported Method')

        if response.status_int >= 400:
            if response.status_int == 404:
                raise restkit.errors.ResourceNotFound(response.body_string())
            elif response.status_int == 401:
                raise restkit.errors.Unauthorized('Unauthorized Request')
            raise restkit.errors.RequestFailed(
                response.body_string(), response.status_int, response)

        # body_string() can only be called once, since it's a socket read, or
        # will throw an AlreadyRead exception.
        response.body = response.body_string()

        return response

    def generateSystemFilter(self, agent=None,  # pylint: disable=C0103
             instance=None, allTags=None, anyTag=None): # pylint: disable=C0103
        """Deprecated version of generate_system_filter()"""
        logger.warn('DEPRECATED: Client.generateSystemFilter() is deprecated,'
        ' use Client.generate_system_filter() instead.')
        self.generate_system_filter(
            agent=agent, instance=instance, all_tags=allTags, any_tag=anyTag)

    def generate_system_filter(self,
            agent=None, instance=None, all_tags=None, any_tag=None):
        """Create a GLU systemFilter string

        :param action: Action to perform: start, stop, bounce, deploy,
            undeploy, redeploy
        :param agent: agent filter.
        :param instance: Instance filter.
        :param all_tags: all tags filter.
        :param any_tag: any tag filter.
        """
        if agent:
            return "agent='%s'" % agent
        if instance:
            return "key='%s'" % instance
        if all_tags:
            return "tags='%s'" % all_tags
        if any_tag:
            return "tags.hasAny('%s')" % any_tag
        return None

    def generateActionRequest(self,     # pylint: disable=C0103
            action, systemFilter, parallel=False):  # pylint: disable=C0103
        """Deprecated version of generate_action_request()"""
        logger.warn('DEPRECATED: Client.generateActionRequest() is deprecated,'
        ' use Client.generate_action_request() instead.')
        self.generate_action_request(action, systemFilter, parallel=parallel)

    def generate_action_request(self, action, system_filter, parallel=False):
        """Create a GLU action string.

        This contains a system_filter, as well as an action, and order.

        :param action: Action to perform: start, stop, bounce, deploy,
            undeploy, redeploy
        :param system_filter: Filter as created from generate_system_filter.
        :param parallel: True to run stop in parallel, False (default) for
            serial.
        """
        filters = []
        filters.append('planAction=' + action)

        # An empty/None filter implies 'all'
        if system_filter:
            filters.append(
                'systemFilter=' + restkit.util.url_quote(system_filter))

        if parallel:
            filters.append('order=parallel')
        else:
            filters.append('order=sequential')

        return restkit.util.to_bytestring('&'.join(filters))

    def _execute_plan(self, created_plan, dryrun):
        """Execute a created plan."""
        self._action_successful = None
        if created_plan.status_int == 204:
            self._action_successful = True
            return 'GLU Console message: %s' % (
                created_plan.status.split(' ', 1)[-1])

        url2uri_pat = r'https?://[-.:\w]+/(?:.*?/)?%s/' % self.uri_path

        # unique identifier for the plan just created
        plan_url = created_plan['location']
        plan_url = re.sub(url2uri_pat, '', plan_url)
        logger.debug('plan url = %s', plan_url)

        # inspect execution plan here, if you need
        exec_plan = self._do_request(plan_url, 'GET')
        logger.debug('body = %s', exec_plan.body)

        if dryrun:
            self._action_successful = True
            return exec_plan.body

        # execute the plan
        plan_url += '/execution'
        logger.info('executing plan: %s', plan_url)
        plan_status = self._do_request(plan_url, 'POST')

        # check status of plan execution
        status_url = plan_status['location']
        status_url = re.sub(url2uri_pat, '', status_url)
        logger.info('status url = %s', status_url)

        # wait until plan is 100% executed.
        completed = re.compile(r'^100')

        if use_progressbar:
            widgets = [' ', progressbar.Percentage(),
                       ' ', progressbar.Bar(marker='*', left='[', right=']'),
                       ' ', progressbar.ETA(), ' ']

            progress = progressbar.ProgressBar(widgets=widgets, maxval=100)
            progress.start()

        while 1:
            progress_status = self._do_request(status_url, 'HEAD')
            complete_status = progress_status['x-glu-completion']
            percent_complete = re.split(':', complete_status)

            if not completed.match(complete_status):
                if use_progressbar:
                    progress.update(int(percent_complete[0]))
                else:
                    logger.info(
                        'InProgress: %s%% complete', percent_complete[0])

            else:
                if use_progressbar:
                    progress.finish()
                else:
                    logger.info('Completed : %s', complete_status)

                break

            time.sleep(2)

        self._action_successful = complete_status.startswith('100:COMPLETED')
        return complete_status

    def executePlan(self, action, systemFilter, # pylint: disable=C0103
            parallel=False, dryrun=False):  # pylint: disable=C0103
        """Deprecated version of execute_plan()"""
        logger.warn('DEPRECATED: Client.executePlan() is deprecated,'
        ' use Client.execute_plan() instead.')
        self.execute_plan(
            action, systemFilter, parallel=parallel, dryrun=dryrun)

    def execute_plan(self, action, system_filter, parallel=False, dryrun=False):
        """Run the a plan command against the console.

        :param action: Action to perform: start, stop, bounce, deploy,
            undeploy, redeploy
        :param system_filter: Filter as created from generate_system_filter.
        :param parallel: True to run stop in parallel, False (default) for
            serial.
        :param dryrun: Create the plan, but don't execute it.
        """
        self._action_successful = None
        if action not in (
                'start', 'stop', 'bounce', 'deploy', 'undeploy', 'redeploy'):
            raise StandardError('Action %s is invalid.' % action)

        plan_filter = self.generate_action_request(
            action, system_filter, parallel)
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
        }
        plan = self._do_request('plans', 'POST', plan_filter, headers)

        return self._execute_plan(plan, dryrun)

    def loadModel(self, modelUrl=None, modelFile=None): # pylint: disable=C0103
        """Deprecated version of execute_plan()"""
        logger.warn('DEPRECATED: Client.loadModel() is deprecated,'
        ' use Client.execute_plan() instead.')
        self.load_model(model_url=modelUrl, model_filename=modelFile)

    def load_model(self, model_url=None, model_filename=None):
        """Load a model into the console.

        :param model_url: to load the model from a model
        :param model_filename: to load a model directly from a file
        """
        if (model_url is None) and (model_filename is None):
            raise StandardError('model_url or model_filename must be provided')

        status = None
        self._action_successful = None

        if model_url is not None:
            body = {
                'modelUrl': model_url
            }
            response = self._do_request('model/static', 'POST', body)
            status = response.status_int
        else:
            with open(model_filename, 'r') as model_file:
                headers = {
                    'Content-Type': 'text/json'
                }
                response = self._do_request(
                    'model/static', 'POST', model_file, headers)
                status = response.status_int

        # XXX - Should exceptions be thrown here?
        if status == 201:
            self._action_successful = True
            return 'Model loaded successfully: ' + response.body
        if status == 204:
            self._action_successful = True
            return 'Model applied, but was not updated.'

        self._action_successful = False
        if status == 400:
            #"Error: Invalid model."
            return ''
        if status == 404:
            #"Error: model not found."
            return ''
        return ''

    def status(self,
            live=True, beautify=False, system_filter=None, filter=None):
        """Retrieve the model, either currently loaded or live as JSON.

        :param live: If True, retrieve the live model. Otherwise the loaded
            model.
        :param beautify: If True, use a pretty printer on the output.
        :param system_filter: The DSL/filter syntax for the GLU console to parse.
        """
        if filter:
            logger.warn('DEPRECATED: filter argument in Client.status() is'
            ' deprecated, use system_filter instead.')
            system_filter = filter
        uri = 'model/live' if live else 'model/static'
        params = {}

        if beautify:
            params['prettyPrint'] = 'true'
        if system_filter:
            params['systemFilter'] = system_filter

        if params:
            uri = '%s?%s' % (uri, urllib.urlencode(params))

        response = self._do_request(uri, 'GET')

        return response.body
