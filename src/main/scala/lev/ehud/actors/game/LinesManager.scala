package lev.ehud.actors.game

import akka.actor.{ OneForOneStrategy, Props, ActorRef, Actor }
import Actor._
import collection.mutable.{HashMap, Map}
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.Router
import akka.routing.RoundRobinRouter
import akka.routing.Broadcast
import ChatSearch._


object mian extends App{
   // val source = scala.io.Source.fromFile("resources/test.txt")
   // val document = source.getLines()
    
    val system = ActorSystem("managerExample")
    
    val printer = system.actorOf(Props[Printer], "printer")    
    
    val linerManager = system.actorOf(Props[LinesManager])
    
    linerManager ! LinesInput(scala.io.Source.fromFile("./src/main/resources/test.txt").getLines)
    
    var str = ""
    val scanner = new java.util.Scanner(System.in)
    println("stating word search - please enter word:")
    
    while (!str.equals("exit")){
      str = scanner.nextLine()
      linerManager ! Find(str)
    }
    println("shutting down")
    
    system.shutdown
  //  source.close()
  
  
}

class LinesManager extends Actor {
	
    val nrOfWorkers = 6    
    
    val router1 = context.actorOf(Props[LineSearchActor].withRouter(RoundRobinRouter(nrOfInstances = nrOfWorkers)))
    
    def receive = {
      case LinesInput(lines : Iterator[String]) =>
        for (line <- lines){
          if (!line.isEmpty){            
            router1 ! Line(line)
          }
        }
      case Find(word: String) =>  {
          val p: Props = Aggregator1.props(nrOfWorkers, word)
          val aggregator = context.actorOf(p,"AggFor_"+word)       
          router1 ! Broadcast(FindToLiners(word,aggregator))          
      }  
    }
    
    
}

