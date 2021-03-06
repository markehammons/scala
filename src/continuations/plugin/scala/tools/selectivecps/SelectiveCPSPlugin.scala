// $Id$

package scala.tools.selectivecps

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

class SelectiveCPSPlugin(val global: Global) extends Plugin {
  val name = "continuations"
  val description = "applies selective cps conversion"

  val anfPhase = new {val global = SelectiveCPSPlugin.this.global } with SelectiveANFTransform() {
    val runsAfter = List("pickler")
  }

  val cpsPhase = new {val global = SelectiveCPSPlugin.this.global } with SelectiveCPSTransform() {
    val runsAfter = List("selectiveanf")
    override val runsBefore = List("uncurry")
  }

  val components = List[PluginComponent](anfPhase, cpsPhase)

  val checker = new { val global: SelectiveCPSPlugin.this.global.type = SelectiveCPSPlugin.this.global } with CPSAnnotationChecker
  global.addAnnotationChecker(checker.checker)
  global.analyzer.addAnalyzerPlugin(checker.plugin)

  global.log("instantiated cps plugin: " + this)

  def setEnabled(flag: Boolean) = {
    checker.cpsEnabled = flag
    anfPhase.cpsEnabled = flag
    cpsPhase.cpsEnabled = flag
  }

  // TODO: require -enabled command-line flag

  override def processOptions(options: List[String], error: String => Unit) = {
    var enabled = false
    for (option <- options) {
      if (option == "enable") {
        enabled = true
      } else {
        error("Option not understood: "+option)
      }
    }
    setEnabled(enabled)
  }

  override val optionsHelp: Option[String] =
    Some("  -P:continuations:enable        Enable continuations")
}
