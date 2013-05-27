package org.pongasoft.glu.packaging.setup

/**
 * Contains a set of utilities methods
 *
 * @author yan@pongasoft.com  */
public class SetupUtils
{
  static String executeCommand(def cmd)
  {
    println cmd
    Process process = cmd.execute()
    Thread.start {
      System.err << process.errorStream
    }
    Thread.start {
      process.outputStream.close()
    }
    def res = process.text

    if(process.waitFor() != 0)
      throw new Exception("error while executing command")

    return res
  }

}