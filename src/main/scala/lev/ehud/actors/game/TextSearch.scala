package lev.ehud.actors.game

import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import Actor._
import akka.actor.ActorSystem
import akka.actor.Props

import akka.actor.ActorRef
import java.io.File
import lev.ehud.actors.game.TextSearch.PrinterCommon.{ OnlyPrint, WriteToFile}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._


object TextSearch extends App{

  case class SongInput(song: String ,document: Iterator[String])
  case class SearchSong(song: String ,word: String)
  case class SearchOnAllSong(word: String)
  case class LinesInput(document: Iterator[String])
  case class Line(line: String)
  case class Find(word: String)
  case class FindInLines(word: String , ref : ActorRef)
  case class FindResult(word: String,lines: Set[String])
  case class Dieing(word: String,lines: Set[String])
  object Finish


  simpleRun

  def simpleRun(){
    implicit val timeout = Timeout(5 seconds)

    val system = ActorSystem("onlyLiners")

    val liner = system.actorOf(Props[LineSearchActor])

    liner ! Line("this is my first line")
    liner ! Line("this is my second line")
    liner ! Line("this is my third line")
    liner ! Line("this is my fourth line")



    //create actor printer
    val printer = system.actorOf(Props[Printer])

    // send to liner with printer
    liner ! FindInLines("first", printer)
//    Thread.sleep(10)
//
//    printer ! WriteToFile(new File("./printer.log"))
//    Thread.sleep(10)
//
//    liner ! FindInLines("second", printer)
//    Thread.sleep(10)
//
//    printer ! OnlyPrint
//    Thread.sleep(10)
//
//    liner ! FindInLines("third", printer)

    Thread.sleep(10)

    system.shutdown
  }



  //Actor that counts the words for a single line
  class LineSearchActor extends Actor {

    var lines :Set[String] = Set.empty[String]

    def receive = {
      case Line(line) =>{
        lines = lines+line
      }
      case FindInLines(word, targetActor) =>{
        val ans :Set[String] = lines.filter(line => line.contains(word)).toSet[String]
        targetActor ! FindResult(word,ans)

      }

    }
  }

  object PrinterCommon{

    case class WriteToFile(file : File)

    object OnlyPrint

    var PRINTER_PATH : String = ""


  }

  class Printer extends Actor{

    override def preStart(){
      PrinterCommon.PRINTER_PATH = context.self.path.toString()
    }

    var currentFilePrinter: java.io.PrintWriter = null

    def receive = {
      case f : FindResult =>{
        print(f.word,f.lines)
      }
      case f: Dieing => {
        println("actor did not finish it's work")
        print(f.word,f.lines)
      }
      case w : WriteToFile => {
        val file = w.file
        if (!file.exists()) {
          file.createNewFile()
        }
        currentFilePrinter = new java.io.PrintWriter(file)
        println(s"printer will now start writing to file [$file]")
        context.become(receiveWithFile)
      }

    }

    def receiveWithFile : Receive = {
      case f : FindResult =>{
        writeToFile(f.lines)
        print(f.word,f.lines)
      }
      case f: Dieing => {
        println("actor did not finish it's work")
        writeToFile(f.lines)
        print(f.word,f.lines)
      }
      case OnlyPrint => {
        currentFilePrinter.close()
        println(s"printer will now return to only write to console")
        context.unbecome()
      }
    }

    def writeToFile(lines: Set[String]){
      lines.map(line => currentFilePrinter.write(line))
      currentFilePrinter.flush()
    }

    def print(word: String, lines: Set[String]){
      println("searched for string \""+word+"\" got ["+lines.size+"] lines:")
      for (x <- lines) println(x)
    }
  }



}
  
  
  
  
   