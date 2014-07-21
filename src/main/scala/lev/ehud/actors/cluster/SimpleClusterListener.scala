package lev.ehud.actors.cluster

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor._
import lev.ehud.actors.game.TextSearch.{FindResult, Dieing, Printer}
import lev.ehud.actors.simple.HelloActor
import lev.ehud.actors.game.TextSearch.PrinterCommon._
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import scala.Some
import akka.cluster.ClusterEvent.UnreachableMember
import scala.concurrent.Await
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import scala.Some
import akka.cluster.ClusterEvent.UnreachableMember
import scala.concurrent.duration._
import scala.util.Random

/**
 * have been taken from  http://doc.akka.io/docs/akka/snapshot/scala/cluster-usage.html
 */
class SimpleClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)



  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self,  classOf[UnreachableMember])
    cluster.join(cluster.selfAddress)
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case x: MemberEvent => println(x)
  }
}

object GetOtherClusters

class SimpleClusterListener2 extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  var otherClusters: List[Address] = Nil



  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self,  classOf[MemberUp])
    cluster.join(cluster.selfAddress.copy(port = Some(12345)))
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) => {
      log.info("Member is Up: {}", member.address)
      otherClusters = member.address :: otherClusters
    }
    case UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
      otherClusters = member.address :: otherClusters
    }
    case MemberRemoved(member, previousStatus) => {
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
      otherClusters = otherClusters.filter(add => add.equals(member.address))
    }
    case x: MemberEvent => println(x)
    case GetOtherClusters => sender ! otherClusters
  }
}
//-Dakka.actor.provider=akka.cluster.ClusterActorRefProvider -Dakka.remote.transport=akka.remote.netty.NettyRemoteTransport -Dakka.remote.netty.tcp.port=12345
/**
 * This Utils are my way to avoid using external configurations since this is some kind of a demo
 */
object ClusterUtils{

  val port: Int = 12345

  val random = new Random()

  def getNextPort() = port + random.nextInt(1000)

  def setProperties(port: Int) {
    System.setProperty("akka.actor.provider", "akka.cluster.ClusterActorRefProvider")
    System.setProperty("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport")
    System.setProperty("akka.remote.netty.tcp.port", port + "")
  }
}

object main1 extends App{
  ClusterUtils.setProperties(ClusterUtils.port)
  val system = ActorSystem("simpleClusterActor")
  val cluster1 = system.actorOf(Props[SimpleClusterListener], "cluster1")
  val helloActor = system.actorOf(Props[HelloActor], name = "helloactor")
  println(helloActor.path)
  Thread.sleep(1000 * 120)
  system.shutdown()

}

//-Dakka.actor.provider=akka.cluster.ClusterActorRefProvider -Dakka.remote.transport=akka.remote.netty.NettyRemoteTransport -Dakka.remote.netty.tcp.port=22345
object main2 extends App{
  ClusterUtils.setProperties(ClusterUtils.getNextPort)
  val system = ActorSystem("simpleClusterActor")
  val cluster2 = system.actorOf(Props[SimpleClusterListener2], "cluster2")
  Thread.sleep(1000 * 10)
  val sender = system.actorOf(Props(new Sender(cluster2)), "sender")
  Thread.sleep(1000 * 5)
  sender ! "hello"
  sender ! "what ever"
  sender ! "we are sending from what ever"
  Thread.sleep(1000 * 60)
  cluster2 ! PoisonPill
  system.shutdown()

}


class Sender(simpleClusterListener: ActorRef) extends Actor{
  
  var helloactors: List[ActorRef] = Nil
  
  override def preStart(){
    simpleClusterListener ! GetOtherClusters
    //helloactor = Await.result(context.actorSelection("akka.tcp://simpleClusterActor@192.168.10.219:12345/user/helloactor").resolveOne(2 seconds),2 seconds)
  }
  

  def receive = {
    case address: List[Address] => {
      try{
        for (ad <- address){
          try{
            println(s"$ad/user/helloactor")
            if (ad != null){
              val helloactor = Await.result(context.actorSelection(s"$ad/user/helloactor").resolveOne(2 seconds),2 seconds)
              if (helloactor != null){}
              helloactors = helloactor:: helloactors
            }
          }catch {
            case t: Throwable => println(s"could not get actor from $ad")
          }
        }
      }

    }
    case x: Any => helloactors.map(actor => actor ! x)
  }
}