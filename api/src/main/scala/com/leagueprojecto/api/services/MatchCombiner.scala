package com.leagueprojecto.api.services

import akka.actor.{Props, Actor}
import com.leagueprojecto.api.domain.MatchHistory
import com.leagueprojecto.api.services.MatchCombiner.{AllMatches, GetAllMatches}
import com.leagueprojecto.api.services.riot.MatchHistoryService
import com.leagueprojecto.api.services.riot.MatchHistoryService.{MatchHistoryList, GetMatchHistory}

object MatchCombiner {
  case object GetAllMatches
  case class AllMatches(list: List[MatchHistory])

  def props(region: String, summonerId: Long, queueType: String, championList: String) =
    Props(new MatchCombiner(region, summonerId, queueType, championList))
}
class MatchCombiner(region: String, summonerId: Long, queueType: String, championList: String) extends Actor {
  val matchHistoryService = context.actorOf(MatchHistoryService.props(region, summonerId, queueType, championList))

  var matches: List[MatchHistory] = List.empty
  var originalSender = Actor.noSender

  val maxCalls = context.system.settings.config.getInt("matches-combiner.max-calls")

  override def receive: Receive = {

    case GetAllMatches =>
      originalSender = sender()
      matchHistoryService ! GetMatchHistory(0, 15)

    case MatchHistoryList(matchesResponse) if matchesResponse.length == 15 && (matches.length + 15) < (maxCalls * 15) =>
      matches = matches ::: matchesResponse
      matchHistoryService ! GetMatchHistory(matches.length, matches.length + 15)

    case MatchHistoryList(matchesResponse) =>
      matches = matches ::: matchesResponse
      originalSender ! AllMatches(matches)
  }
}
