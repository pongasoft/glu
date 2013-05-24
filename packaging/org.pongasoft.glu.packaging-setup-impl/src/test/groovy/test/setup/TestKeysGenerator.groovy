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

package test.setup

import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.pongasoft.glu.packaging.setup.KeysGenerator

/**
 * @author yan@pongasoft.com  */
public class TestKeysGenerator extends GroovyTestCase
{
  public void testKeysGenerator()
  {
    FileSystemImpl.createTempFileSystem { FileSystem fs ->

      def generator = new KeysGenerator(outputFolder: fs.toResource('/keys').getFile().toPath(),
                                        masterPassword: "abcdefgh".toCharArray())

      println generator.generateAgentKeystore()
    }
  }
}