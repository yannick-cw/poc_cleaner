package rest_connection

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cleaning.CleanActor
import spray.json.DefaultJsonProtocol
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.util.{Failure, Success}
import scala.concurrent.duration._


case class RawText(text: String)
case class CleanedText(cleanedText: String)

trait Protocols extends DefaultJsonProtocol {
  implicit val rawTextFormat = jsonFormat1(RawText.apply)
  implicit val cleanTextFormat = jsonFormat1(CleanedText.apply)
}

trait Service extends Protocols with SprayJsonSupport {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val cleanActor: ActorRef
  val logger: LoggingAdapter

  val cleanDoc = path("clean") {
    logRequestResult("cleaner") {
      (post & entity(as[RawText])) { rawText =>

        implicit val timeout = Timeout(2.seconds)
        val futureRes = cleanActor ? rawText

        onComplete(futureRes) {
          case Success(cleaned: CleanedText) =>
            complete(cleaned.toJson)
          case Failure(ex) =>
            println("!!!!!!!!COMPLETED WITH BADREQUEST!!!!!!!!!!")
            complete(StatusCodes.BadRequest -> "Something went wrong while cleaning, whups!")
        }
      }
    }
  }
}


object AkkaHttpMicroservice extends App with Service {
  implicit val system = ActorSystem("cleaner-system")
  implicit val materializer = ActorMaterializer()
  val logger = Logging(system, getClass)

  val cleanActor: ActorRef = system.actorOf(CleanActor.props, CleanActor.name)

  Http().bindAndHandle(cleanDoc, "0.0.0.0", 4321)
}
