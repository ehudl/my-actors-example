package lev.ehud.actors.game

import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import Actor._
import akka.actor.ActorSystem
import akka.actor.Props

import akka.actor.ActorRef


object ChatSearch extends App{
  
  case class SongInput(song: String ,document: Iterator[String])
  case class SearchSong(song: String ,word: String)
  case class SearchOnAllSong(word: String)  
  case class LinesInput(document: Iterator[String]) 
  case class Line(line: String)  
  case class Find(word: String)
  case class FindToLiners(word: String , ref : ActorRef)
  case class FindResult(word: String,lines: Set[String])
  case class Dieing(word: String,lines: Set[String])   
  object Finish 
  
  
  simpleRun
  
  def simpleRun(){ 
    
  val system = ActorSystem("onlyLiners")
  
  val liner = system.actorOf(Props[LineSearchActor])
  
  liner ! Line("this is my first line")
  liner ! Line("this is my second line")
  
  
  val printer = system.actorOf(Props[Printer])
  
  liner ! FindToLiners("first", printer)
  
  
  Thread.sleep(300)
  system.shutdown
  }
  
  
  
  //Actor that counts the words for a single line
  class LineSearchActor extends Actor {
    
    var lines :Set[String] = Set.empty[String]
 
    def receive = {      
       case Line(line) =>{
         lines = lines+line
       }
       case FindToLiners(word, targetActor) =>{
         val ans :Set[String] = lines.filter(line => line.contains(word)).toSet[String]
         targetActor ! FindResult(word,ans)
        
       }
         
    }   
  }
    
  object PrinterCommon{
    
    var PRINTER_PATH : String = "" 
    
  }
    
    class Printer extends Actor{
     
      override def preStart(){
        PrinterCommon.PRINTER_PATH = context.self.path.toString()     
      }
      
      def receive = {
        case f : FindResult =>{
          print(f.word,f.lines)        
        } 
        case f: Dieing => {
          println("actor did not finish it's work")
          print(f.word,f.lines)        
        }
      }   
      
      def print(word: String, lines: Set[String]){
          println("searched for string \""+word+"\" got ["+lines.size+"] lines:")
          for (x <- lines) println(x)
      }
     }
    


}
  
  
  
  
   