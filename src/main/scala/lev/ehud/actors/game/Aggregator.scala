package lev.ehud.actors.game

import akka.actor.{Actor, PoisonPill}
import Actor._
import collection.mutable.{HashMap, Map}
import akka.actor.ActorSystem
import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import akka.routing.Router
import akka.routing.RoundRobinRouter
import akka.routing.Broadcast
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.util.Try
import akka.util.Timeout
import scala.actors.AskTimeoutException
import akka.actor.ActorRef
import lev.ehud.actors.game.TextSearch.PrinterCommon._

import lev.ehud.actors.game.TextSearch.{Dieing, FindResult}





object Aggregator{
  
     def props( resultNumbers : Int , wordMatch: String): Props = 
       Props(new Aggregator(resultNumbers , wordMatch))
  }

  class Aggregator(resultNumbers : Int , wordMatch: String) extends Actor {
    
    import context.dispatcher
    var _lines = Set.empty[String]    
    var counter = 0
    var printer :ActorRef = context.parent
          
     override def preStart(){
      val futureActor = context.actorSelection(PRINTER_PATH).resolveOne(1 seconds)
      printer = Await.result(futureActor,1 seconds)
      context.system.scheduler.scheduleOnce(3 seconds) (suicide)
    }
    
    def suicide(){
        if (!self.isTerminated){
      	printer ! Dieing(wordMatch,_lines)      	
        self ! PoisonPill 
        }
    	
    }
    
    def receive = {
      case FindResult(word1,lines) if (wordMatch.equals(word1))=>{
        _lines = _lines ++ lines
        counter = counter +1
        if (counter == resultNumbers){
           printer ! FindResult(wordMatch,_lines)
           self ! PoisonPill      
        }        
      }     
    }
    
    override def postStop(){
      println(context.self.path+" stop")
    }
    
    
  }
