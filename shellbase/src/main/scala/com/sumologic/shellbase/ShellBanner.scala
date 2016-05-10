package com.sumologic.shellbase

import org.apache.commons.io.IOUtils

class ShellBanner(resource: String) {

  def load(): String = {
    val in = getClass.getClassLoader.getResourceAsStream(resource)
    val banner = IOUtils.toString(in)
    in.close()
    banner
  }

}

object ShellBanner {
  lazy val Warning = new ShellBanner("banners/warning.txt").load()
}
