package cleaning

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}
import rest_connection.{CleanedText, RawText}
import utils.StopSystemAfterAll

class CleanActorSpec extends TestKit(ActorSystem("testSys"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender
  with StopSystemAfterAll {


  "CleanActor" must {
    "return string with stems of words of inputstring" in {
      val cleanActor = system.actorOf(CleanActor.props)

      cleanActor ! RawText(" darping derpIng      fucking hugging            killing")
      expectMsg(CleanedText(List("darp", "derp", "fuck", "hug", "kill")))
    }
  }
}
