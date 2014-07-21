package lev.ehud.actors.game

import lev.ehud.actors.game.TextSearch.SongInput
import akka.actor._
import akka.actor.Actor._
import collection.mutable.{HashMap, Map}

import TextSearch._
import lev.ehud.actors.cluster.Utils

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.actors.Future
import lev.ehud.actors.game.TextSearch.SongInput
import lev.ehud.actors.game.TextSearch.LinesInput
import lev.ehud.actors.game.TextSearch.SearchOnAllSong
import lev.ehud.actors.game.TextSearch.Find
import lev.ehud.actors.game.TextSearch.SearchSong
import lev.ehud.actors.game.TextSearch.LinesInput
import lev.ehud.actors.game.TextSearch.SearchOnAllSong
import lev.ehud.actors.game.TextSearch.SearchSong
import lev.ehud.actors.game.TextSearch.SongInput
import lev.ehud.actors.game.TextSearch.Find
import lev.ehud.actors.game.TextSearch.LinesInput
import lev.ehud.actors.game.TextSearch.SearchOnAllSong
import lev.ehud.actors.game.TextSearch.SearchSong
import lev.ehud.actors.game.TextSearch.SongInput
import lev.ehud.actors.game.TextSearch.Find
import lev.ehud.actors.game.TextSearch.LinesInput
import lev.ehud.actors.game.TextSearch.SearchOnAllSong
import lev.ehud.actors.game.TextSearch.SearchSong
import lev.ehud.actors.game.TextSearch.SongInput
import lev.ehud.actors.game.TextSearch.Find

/**
 * Created with IntelliJ IDEA.
 * User: ehud1
 * Date: 6/19/14
 * Time: 9:14 PM
 * To change this template use File | Settings | File Templates.
 */
class SongActor extends Actor{

  var songsMap = Map.empty[String,ActorRef]



  override def preStart(): Unit = {
    super.preStart()
    println(context.self.path)
  }

  def receive = {
    case s: SongInput => {
    	songsMap.get(s.song) match  {
    	  case None => {
    	    val linesManager = context.actorOf(Props[LinesManager],s.song)
    	    println ("created a song with name " + s.song)
    	    songsMap = songsMap.updated(s.song,linesManager)
    	    linesManager ! LinesInput(s.document)
    	  }
    	  case _ => println("can not add "+s.song+" already exists")
    	}
    }
    case s: SearchSong =>{
      println("searching on song "+ s.song)
      val liners:ActorRef = songsMap.getOrElse(s.song, {println("no such song" + s.song) ;  context.parent})
      liners ! Find(s.word)
    }
    case s: SearchOnAllSong =>{
      println("searching on all song ")
      for (liner <- songsMap.values) liner ! Find(s.word)
    }
  }
}


object songMainCommons{

  def addSongs(songManager : ActorRef){
    songManager ! SongInput("1",scala.io.Source.fromFile("./src/main/resources/test.txt").getLines.toList)
    songManager ! SongInput("2",scala.io.Source.fromFile("./src/main/resources/test2.txt").getLines.toList)
    songManager ! SongInput("3",scala.io.Source.fromFile("./src/main/resources/test3.txt").getLines.toList)
  }

  def songSearch(system: ActorSystem, addSongFlag: Boolean) {
    val songManager = system.actorOf(Props[SongActor],"game")
    if (addSongFlag) addSongs(songManager)
    def runInteractive {
      var str = ""
      val scanner = new java.util.Scanner(System.in)
      println("stating chat search - please enter <song> <word>:")

      while (!str.equals("exit")) {
        str = scanner.nextLine()
        val couple = str.split(" ")
        if (couple.size < 2) {
          if (str.length > 3){
            songManager ! SearchOnAllSong(str)
          } else{
            println(s"got [$str]word must be bigger than 3 characters ")
          }

        } else {
          if (couple(1).length > 3) {
            songManager ! SearchSong(couple(0), couple(1))
          } else {
            val word = couple(1)
            println(s"got [$word]word must be bigger than 3 characters ")
          }
        }
      }
    }
    runInteractive


  }
}

object songMain extends App{
  val printerName = "Printer"
  val system = ActorSystem("songSearch")

  val printer = system.actorOf(Props[Printer], printerName)
  songMainCommons.songSearch(system,true)
  println("shutting down")
  system.shutdown


}

object songWithClustersMain extends App{
  val printerName = "Printer"
  Utils.setProperties(Utils.port)
  val system = ActorSystem("songSearch")

  val listener = system.actorOf(Props[SongClusterListener],"listener")

  val printer = system.actorOf(Props[Printer], printerName)
  songMainCommons.songSearch(system,false)
  println("shutting down")
  system.shutdown

}

object songAdderMain extends App{

  Utils.setProperties(Utils.getNextPort())
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


