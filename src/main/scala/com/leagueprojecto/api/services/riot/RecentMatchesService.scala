package com.leagueprojecto.api.services.riot

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.leagueprojecto.api.JsonProtocols
import com.leagueprojecto.api.domain.Match
import spray.json._

object RecentMatchesService {
  case class GetRecentMatchIds(region: String, summonerId: Long, queueType: String, championList: String, amount: Int)
  case class Result(matchIds: Seq[Long])

  def props = Props(new RecentMatchesService)
}

class RecentMatchesService extends Actor with RiotService with ActorLogging with JsonProtocols {
  import RecentMatchesService._

  override def receive: Receive = {
    case GetRecentMatchIds(regionParam, summonerId, queueType, championList, amount) =>
      implicit val origSender = sender()

      val queryparams: Map[String, String] = Map("beginIndex" -> (0 toString), "endIndex" -> (amount toString))
      val matchlistEndpoint: Uri = endpoint(regionParam, matchlistBySummonerId + summonerId, queryparams)

      val future = riotRequest(RequestBuilding.Get(matchlistEndpoint))
      future onSuccess successHandler(origSender).orElse(defaultSuccessHandler(origSender))
      future onFailure failureHandler(origSender)
  }

  def successHandler(origSender: ActorRef): PartialFunction[HttpResponse, Unit] = {
    case HttpResponse(OK, _, entity, _) =>
      Unmarshal(entity).to[String].onSuccess {
        case result: String =>
          val matchlist = transform(result.parseJson.asJsObject)
          println(s"${matchlist.size} matches found!")
          val matchIds = matchlist.map(_.matchId)
          origSender ! Result(matchIds)
      }
  }

  def failureHandler(origSender: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: Exception =>
      log.error(e, s"request failed for some reason")
  }

  private def transform(riotResult: JsObject): List[Match] = {
    val firstKey = riotResult.fields.keys.head
    riotResult.fields.get(firstKey).get.convertTo[List[Match]]
  }
}
