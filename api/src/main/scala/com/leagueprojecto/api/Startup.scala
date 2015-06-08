package com.leagueprojecto.api

import akka.actor._
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorFlowMaterializer
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import com.leagueprojecto.api.services.CacheService
import com.leagueprojecto.api.services.riot.{MatchHistoryService, SummonerService}
import com.typesafe.config.ConfigFactory

object Startup extends App with Routes {
  override implicit val system: ActorSystem = ActorSystem("api")
  override implicit val executor = system.dispatcher
  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  implicit val materializer = ActorFlowMaterializer()

  // Services
  val summonerService: ActorRef = system.actorOf(SummonerService.props)
  val matchHistoryService: ActorRef = system.actorOf(MatchHistoryService.props)

  // Service caches
  val summonerCacheTime = config.getLong("riot.services.summonerbyname.cacheTime")
  val matchhistoryCacheTime = config.getLong("riot.services.matchhistory.cacheTime")
  override val cachedSummonerService: ActorRef =
    system.actorOf(CacheService.props[Summoner](summonerService, summonerCacheTime))
  override val cachedMatchHistoryService: ActorRef =
    system.actorOf(CacheService.props[List[MatchHistory]](matchHistoryService, matchhistoryCacheTime))

  // Bind the HTTP endpoint. Specify http.interface and http.port in the configuration
  // to change the address and port to bind to.
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
