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