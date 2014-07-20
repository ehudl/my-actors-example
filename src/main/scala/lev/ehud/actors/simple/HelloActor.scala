package lev.ehud.actors.simple

/**
 * from http://alvinalexander.com/scala/simple-scala-akka-actor-examples-hello-world-actors
 */
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * This is a very simple example that shows how to create an actor,
 * how to send him a message
 * how to change it's behaviour
 */
class HelloActor extends Actor {

  def receive = {
    case "hello" => println("hello back at you")
    case _       => context.become(huh)
  }

  def huh: Receive = {
    case s : String => println(s"what is [$s] ?")
  }

}

object Main extends App {
  val system = ActorSystem("HelloSystem")
  // default Actor constructor
  val helloActor = system.actorOf(Props[HelloActor], name = "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
  helloActor ! "buenos dias"
  Thread.sleep(100)
  system.shutdown()
}