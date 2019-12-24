package org.ergoplatform.appkit.ergotool

import java.util.Arrays

import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.ErgoToolConfig
import org.ergoplatform.appkit.console.Console
import org.ergoplatform.appkit.ergotool.ErgoTool.RunContext

/** Base class for all commands which can be executed by ErgoTool.
  * Inherit this class to implement a new command.
  * @see [[RunWithErgoClient]] if your command need to communicate with blockchain.
  */
abstract class Cmd {
  /** @return current tool configuration parameters */
  def toolConf: ErgoToolConfig

  /** @return the name of this command (Example: `send`, `mnemonic` etc.) */
  def name: String

  /** @return the url of the Ergo blockchain node used to communicate with the network. */
  def apiUrl: String = toolConf.getNode.getNodeApi.getApiUrl

  /** ApiKey which is used for Ergo node API authentication.
    * This is a secrete key whose hash was used in Ergo node config.
    * This is only necessary to communicate to the protected parts of node API.
    */
  def apiKey: String = toolConf.getNode.getNodeApi.getApiKey

  /** Returns the network type (MAINNET or TESTNET) [[ErgoTool]] is expected to communicate.
    * This parameter should correspond to the real network type of the node pointed to by [[apiUrl]].
    */
  def networkType: NetworkType = toolConf.getNode.getNetworkType

  /** Runs this command using given [[ErgoTool.RunContext]].
    * @param ctx context information of this command execution collected from command line,
    * configuration file etc.
    */
  def run(ctx: RunContext): Unit
}

/** This trait can be used to implement commands which need to communicate with Ergo blockchain.
  * The default [[Cmd.run]] method is implemented and the new method with additional [[ErgoClient]]
  * parameter is declared, which is called from the default implementation.
  * To implement new command mix-in this train and implement [[RunWithErgoClient.runWithClient]] method.
  */
trait RunWithErgoClient extends Cmd {
  override def run(ctx: RunContext): Unit = {
    val ergoClient = ctx.clientFactory(ctx)
    runWithClient(ergoClient, ctx)
  }

  /** Called from [[run]] method with ErgoClient instance ready for Ergo blockchain communication. */
  def runWithClient(ergoClient: ErgoClient, ctx: RunContext): Unit
}

/** Base class for all Cmd factories (usually companion objects)
 */
abstract class CmdDescriptor(
     /** Command name used in command line. */
     val name: String,

     /** parameters syntax specification */
     val cmdParamSyntax: String,
     val description: String) {

  /** Creates a new command instance based on the given [[ErgoTool.RunContext]] */
  def parseCmd(ctx: RunContext): Cmd

  def error(msg: String) = {
    sys.error(s"Error executing command `$name`: $msg")
  }

  def parseNetwork(network: String): NetworkType = network match {
    case "testnet" => NetworkType.TESTNET
    case "mainnet" => NetworkType.MAINNET
    case _ => error(s"Invalid network type $network")
  }

  /** Secure entry of the new password.
   *
   * @param nAttemps number of attempts
   * @param block  code block which can request the user to enter a new password twice
   * @return password returned by `block`
   */
  def readNewPassword(nAttemps: Int, console: Console)(block: => (Array[Char], Array[Char])): Array[Char] = {
    var i = 0
    do {
      val (p1, p2) = block
      i += 1
      if (Arrays.equals(p1, p2)) {
        Arrays.fill(p2, ' ') // cleanup duplicate copy
        return p1
      }
      else {
        Arrays.fill(p1, ' ') // cleanup sensitive data
        Arrays.fill(p2, ' ')
        if (i < nAttemps) {
          console.println(s"Passwords are different, try again [${i + 1}/$nAttemps]")
          // and loop
        } else
          error(s"Cannot continue without providing valid password")
      }
    } while (true)
    error("should never go here due to exhaustive `if` above")
  }
}

