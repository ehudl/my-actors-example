package lev.ehud.actors.game

import lev.ehud.actors.game.ChatSearch.SongInput
import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import Actor._
import collection.mutable.{HashMap, Map}
import akka.actor.ActorSystem
import akka.actor.Props

import akka.actor.ActorRef
import ChatSearch._

/**
 * Created with IntelliJ IDEA.
 * User: ehud1
 * Date: 6/19/14
 * Time: 9:14 PM
 * To change this template use File | Settings | File Templates.
 */
class SongActor extends Actor{

  var songsMap = Map.empty[String,ActorRef]

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

object songMain extends App{
  val printerName = "Printer"
  chatSearch()

  def chatSearch() {

    val source = scala.io.Source.fromFile("resources/test.txt")
    val document = source.getLines()

    val system = ActorSystem("chatSearch")

    val printer = system.actorOf(Props[Printer], printerName)


    val songManager = system.actorOf(Props[SongActor])

    songManager ! SongInput("1",scala.io.Source.fromFile("./src/main/resources/test.txt").getLines)
    songManager ! SongInput("2",scala.io.Source.fromFile("./src/main/resources/test2.txt").getLines)
    var str = ""
    val scanner = new java.util.Scanner(System.in)
    println("stating chat search - please enter <song> <word>:")

    while (!str.equals("exit")){
      str = scanner.nextLine()
      val couple = str.split(" ")
      if (couple.size < 2) {
        songManager ! SearchOnAllSong(str)
      }else{
        songManager ! SearchSong(couple(0),couple(1))
      }
    }
    println("shutting down")

    system.shutdown
    source.close()
  }
}