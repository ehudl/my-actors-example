package lev.ehud.actors.game.cluster

import lev.ehud.actors.cluster.ClusterUtils
import akka.actor.{PoisonPill, Actor, Props, ActorSystem}
import lev.ehud.actors.game._
import lev.ehud.actors.game.TextSearch.Printer


/**
 * Created by ehud on 7/21/2014.
 *
 * This main is for starting the game without songs in it!!
 * And with a cluster abilities on default port
 */
object songWithClustersMain extends App{
  val printerName = "Printer"
  ClusterUtils.setProperties(ClusterUtils.port)
  val system = ActorSystem("songSearch")

  val listener = system.actorOf(Props[SongClusterListener],"listener")

  val printer = system.actorOf(Props[Printer], printerName)
  songMainCommons.songSearch(system,false)
  println("shutting down")
  system.shutdown

}

/**
 * This main assumes that there is a game on the default port
 * and try to connect to the game and add songs.
 *
 * This is just to show how 2 actor system can talk with together
 */
object songAdderMain extends App{
  ClusterUtils.setProperties(ClusterUtils.getNextPort())
  val system = ActorSystem("songSearch")
  val listener = system.actorOf(Props[SongClusterJoiner],"joiner")
  val actorFinder = system.actorOf(Props(new ActorFinder(listener)),"finder")
  Thread.sleep(2000)
  val relativePath = "/user/game"

  val addSongsWithRuntimeActor = system.actorOf(Props(new Actor {
    actorFinder ! GetActor(relativePath)
    override def receive: Receive = {
      case a: ActorFromRelativePath =>
        println("got actor "+a.actor)
        songMainCommons.addSongs(a.actor)

    }
  }))


  Thread.sleep(1000 * 2)
  addSongsWithRuntimeActor ! PoisonPill
  listener ! PoisonPill
  Thread.sleep(1000 * 2)

  system.shutdown()




}
