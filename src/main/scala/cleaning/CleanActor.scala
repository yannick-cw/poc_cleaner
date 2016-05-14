package cleaning

import akka.actor.{Actor, Props}
import rest_connection.{CleanedText, RawText}

/**
  * Created by yannick on 14.05.16.
  */
object CleanActor {
  val props = Props(new CleanActor())
  val name = "clean-actor"
}

class CleanActor extends Actor {

  def receive: Receive = {
    case RawText(text) => sender ! CleanedText(text)
  }

}
