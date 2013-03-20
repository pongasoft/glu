#!/usr/bin/python

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

"""This script provides a way to communicate with GLU console via REST API.

It provides high level actions (by constructing lower level REST calls to the
console), such as:
-- start (to start apps in a fabric)
-- stop  (to stop apps in a fabric)
-- bounce (stop + start)
-- deploy (equivalent to start)
-- undeploy (remote apps in a fabric)
-- redeploy (undeploy + deploy)
-- load  (to load a new model for a fabric)
-- status  (to load a new model for a fabric)

It uses gluconsole.rest.Client, which encapsulates these methods.
"""

import getpass
import logging
import os
import site
import sys
from optparse import OptionParser

logging.basicConfig(
    format='%(asctime)s [%(levelname)s] - %(name)s - %(message)s')

bin_path = os.path.abspath((os.path.dirname(sys.argv[0])))
lib_path = os.path.join(os.path.dirname(bin_path), 'lib', 'python')
site.addsitedir(lib_path)
site.addsitedir(os.path.join(lib_path, 'site-packages'))
import gluconsole.rest


def main():
    log = logging.getLogger('glu')

    # current username
    user = getpass.getuser()

    # command line parsing
    usage = ('usage: %prog -f <fabric>'
        ' <start|stop|bounce|deploy|undeploy|redeploy|load|status> [flags]')
    parser = OptionParser(usage=usage,
        version='@console.name@: @console.version@')

    # common options for all actions
    parser.add_option(
        '-d', '--debug', dest='debug', action='store_true', default=False,
        help='Turn on debug output'
    )
    parser.add_option(
        '-c', '--console', dest='consoleurl', action='store',
        default='http://localhost:8080/console',
        help='Url to GLU Console for the given fabric.'
    )
    parser.add_option(
        '-f', '--fabric', dest='fabric', action='store',
        help='Perform action on a fabric'
    )
    parser.add_option(
        '-u', '--user', dest='user', action='store', default=user,
        help='GLU user to use for authentication, defaults to ' + user
    )
    parser.add_option(
        '-x', '--xpassword', dest='password', action='store',
        help='Password. Warning password will appear in clear in ps output.'
            ' Use only for testing.'
    )
    parser.add_option(
        '-X', '--xpasswordfile', dest='passwordfile', action='store',
        help='Read user password from passwordfile specified.'
            ' Make sure to protect this file using unix permissions'
    )

    # controlling start/stop actions
    parser.add_option(
        '-a', '--all', dest='all', action='store_true', default=False,
        help='Perform action on all entries'
    )
    parser.add_option(
        '-A', '--agent', dest='agent', action='store',
        help='Perform action on one or more agent(s)'
    )
    parser.add_option(
        '-t', '--allTags', dest='all_tags', action='store',
        help='Shortcut for querying by tags (all tags must be present):'
            ' frontend;backend'
    )
    parser.add_option(
        '-T', '--anyTag', dest='any_tag', action='store',
        help='Shortcut for querying by tags (any of the tags need to be'
            ' present): frontend;backend'
    )
    parser.add_option(
        '-I', '--instance', dest='instance', action='store',
        help='Perform action on one or more instance(s)'
    )
    parser.add_option(
        '-p', '--parallel', dest='parallel', action='store_true',
        default=False,
        help='Perform action on all instances in parallel. Default is serial.'
    )
    parser.add_option(
        '-n', '--dryrun', dest='dryrun', action='store_true', default=False,
        help='Do a dry run of your plan. No changes will be made.'
            ' Default is false.'
    )

    # options for status action
    parser.add_option(
        '-s', '--systemFilter', dest='filter', action='store',
        help='Filter in DSL sytax for filtering the model. Applicable only'
            ' with "status" command. See "Filter Syntax" section here:'
            ' https://github.com/linkedin/glu/wiki/Console '
    )
    parser.add_option(
        '-S', '--systemFilterFile', dest='filter_filename', action='store',
        help='Filter file with filters in DSL sytax for filtering the model.'
            ' Applicable only with "status" command. See "Filter Syntax"'
            ' section here: https://github.com/linkedin/glu/wiki/Console '
    )

    # options for model loading
    parser.add_option(
        '-m', '--model', dest='model', action='store',
        help='Loads the model pointed to by model (should be a url!)'
    )
    parser.add_option(
        '-M', '--modelFile', dest='model_file', action='store',
        help='Loads the model from the file'
    )
    parser.add_option(
        '-l', '--live', dest='live', action='store_true', default=False,
        help='Show current model instead of expected model.'
            ' Applicable only with "status" command.'
    )
    parser.add_option(
        '-b', '--beautify', dest='beautify', action='store_true',
        default=False, help='Pretty print the model.'
    )

    # get options provided on commandline. Remaining arg is 'action'
    options, args = parser.parse_args()
    if len(args) != 1:
        parser.error('One action must be specified')
    else:
        action = args[0]

    # validate options
    if not options.fabric:
        parser.error('Fabric must be specified')

    if not options.consoleurl:
        parser.error('Console URL must be specified')

    if options.debug:
        log.setLevel(logging.DEBUG)
        gluconsole.rest.logger.setLevel(logging.DEBUG)

    if options.filter and options.filter_filename:
        parser.error('Only --systemFilter or --systemFilterFile must be'
            ' specified, not both.')

    # Only one of the following options can be specified.
    # FWIW, argparse supports mutually exclusive groups.
    exclusive_options = dict()

    if options.agent:
        exclusive_options['agent'] = True

    if options.all_tags:
        exclusive_options['all_tags'] = True

    if options.any_tag:
        exclusive_options['any_tag'] = True

    if options.instance:
        exclusive_options['instance'] = True

    if options.filter or options.filter_filename:
        exclusive_options['filter'] = True

    if options.all:
        exclusive_options['all'] = True

    if len(exclusive_options.keys()) > 1:
        parser.error('Only one of:'
            ' agent, allTags, anyTag, instance, filter or all may be used.')

    # get user's and password
    if options.password:
        password = options.password
    elif options.passwordfile:
        with open(options.passwordfile, 'r') as password_file:
            password = password_file.readline().strip()
    else:
        password = getpass.getpass("Enter %s's password: " % options.user)

    system_filter = None

    if options.filter:
        system_filter = options.filter

    if options.filter_filename:
        with open(options.filter_filename, 'r') as filter_file:
            system_filter = filter_file.read().strip()

    # create client to process the request
    log.info('Will run action "%s" on fabric "%s"', action, options.fabric)

    client = gluconsole.rest.Client(
        fabric=options.fabric, url=options.consoleurl,
        username=options.user, password=password
    )

    # now perform the action
    result = None

    model_loaded = None

    if options.model or options.model_file:
        model_loaded = client.load_model(
            model_url=options.model, model_filename=options.model_file)
        print model_loaded

    if action in ('start', 'stop', 'bounce', 'deploy', 'undeploy', 'redeploy'):
        if system_filter is None:
            system_filter = client.generate_system_filter(options.agent,
                options.instance, options.all_tags, options.any_tag)

        if system_filter is None and not options.all:
            parser.error(
                'you need to specify one of: agent, instance, filter or all!')

        result = client.execute_plan(
            action, system_filter, options.parallel, options.dryrun)

    elif action == 'load':
        if model_loaded is None:
            parser.error('you need to specify which model to load (-m or -M)!')

    elif action == 'status':
        if system_filter is None:
            system_filter = client.generate_system_filter(options.agent,
                options.instance, options.all_tags, options.any_tag)

        result = client.status(options.live, options.beautify, system_filter)

    else:
        parser.error('Unknown action: ' + action)

    if result is not None:
        print(result)
    if client.action_successful is not None:
        return 0 if client.action_successful else 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
