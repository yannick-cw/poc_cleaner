package cleaning

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}
import rest_connection.{CleanedText, RawText}
import util.StopSystemAfterAll

class CleanActorSpec extends TestKit(ActorSystem("testSys"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender
  with StopSystemAfterAll {


  "CleanActor" must {
    "return a CleanedMessage with the same text as the input text" in {
      val cleanActor = system.actorOf(CleanActor.props)

      cleanActor ! RawText("Hallo")
      expectMsg(CleanedText("Hallo"))
    }
  }
}
