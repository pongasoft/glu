1.7.0 (2011/01/17)
------------------
* Implemented [glu-12](https://github.com/linkedin/glu/issues#issue/12): better packaging
* fixed [glu-1](https://github.com/linkedin/glu/issues#issue/1): Agent name and fabric are not preserved upon restart
* fixed [glu-9](https://github.com/linkedin/glu/issues#issue/9): Using http://name:pass@host:port is broken when uploading a model to /system/model
* Implemented [glu-16](https://github.com/linkedin/glu/issues#issue/16): Use ip address instead of canonical name for Console->Agent communication
* Updated Copyright

1.6.0 (2011/01/11)
------------------
* changed the tutorial to deploy jetty and the sample webapps to better demonstrate the capabilities of glu
* added jetty glu script which demonstrates a 'real' glu script and allows to deploy a webapp container with webapps and monitor them
* added sample webapp with built in monitoring capabilities
* added `replaceTokens` and `httpHead` to `shell` (for use in glu script)
* added `Help` tab in the console with embedded forum
* Implemented [glu-12](https://github.com/linkedin/glu/issues#issue/12) (partially): better packaging
* fixed [glu-13](https://github.com/linkedin/glu/issues#issue/13): missing connection string in setup-zookeeper.sh

1.5.1 (2010/12/28)
------------------
* fixed [glu-10](https://github.com/linkedin/glu/issues#issue/10): missing -s $GLU_ZK_CONNECT_STRING in setup-agent.sh (thanks to Ran)
* fixed [glu-11](https://github.com/linkedin/glu/issues#issue/11): missing glu.agent.port when not using default value

1.5.0 (2010/12/24)
------------------
* fixed [glu-8](https://github.com/linkedin/glu/issues#issue/8): added support for urls with basic authentication (thanks to Ran)
* added console cli (`org.linkedin.glu.console-cli`) which talks to the REST api of the console
* changed tutorial to add a section which demonstrates the use of the new cli
* added the glu logo (thanks to Markus for the logos)

1.4.0 (2010/12/20)
------------------
* use of [gradle-plugins 1.5.0](https://github.com/linkedin/gradle-plugins/tree/REL_1.5.0) which now uses gradle 0.9
* added packaging for all clis
* added `org.linkedin.glu.packaging-all` which contains all binaries + quick tutorial
* added `org.linkedin.glu.console-server` for a standalone console (using jetty under the cover)
* moved keys to a top-level folder (`dev-keys`)
* minor change in the console to handle the case where there is no fabric better
* new tutorial based on pre-built binaries (`org.linkedin.glu.packaging-all`)

1.3.2 (2010/12/07)
------------------
* use of [linkedin-utils 1.2.1](https://github.com/linkedin/linkedin-utils/tree/REL_1.2.1) which fixes the issue of password not being masked properly
* use of [linkedin-zookeeper 1.2.1](https://github.com/linkedin/linkedin-zookeeper/tree/REL_1.2.1)

1.3.1 (2010/12/02)
------------------
* use of [gradle-plugins 1.3.1](https://github.com/linkedin/gradle-plugins/tree/REL_1.3.1)
* fixes issue in agent cli (exception when parsing configuration)

1.0.0 (2010/11/07)
------------------
* First release