package lev.ehud.actors.game.cluster

import akka.actor.{ActorRef, Address, ActorLogging, Actor}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.Cluster
import lev.ehud.actors.cluster.GetOtherClusters
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by ehud on 7/20/2014.
 */

abstract class ClusterListenerTrait extends Actor with ActorLogging {

  lazy val cluster = Cluster(context.system)

  var otherClusters: List[Address] = Nil

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
    case GetOtherClusters => {println("got GetOtherClusters return"+ otherClusters); sender ! otherClusters}
  }
}


class SongClusterListener extends ClusterListenerTrait {

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self,  classOf[UnreachableMember])
    cluster.join(cluster.selfAddress)
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)


}

class SongClusterJoiner extends ClusterListenerTrait {

  //  subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self,  classOf[MemberUp])
    cluster.join(cluster.selfAddress.copy(port = Some(12345)))

  }
  override def postStop(): Unit = cluster.unsubscribe(self)

}

case class GetActor(relativePath : String)

case class ActorFromRelativePath(relativePath : String, actor: ActorRef)

class ActorFinder(clusterListener : ActorRef) extends Actor{

  var requests : Map[GetActor,Set[ActorRef]] = Map.empty



  override def receive: Receive = {
    case g: GetActor => {
      println("got GetActor")
      val setOfActors: Set[ActorRef] = requests.get(g).getOrElse(Set.empty)
      val withSender = setOfActors + sender
      requests = requests + (g -> withSender)
      println("requests :"+requests)
      //addSenderToRequests(g)
      clusterListener ! GetOtherClusters
    }
    case otherClusters : List[Address] =>{
      try{
        for (request <- requests){
          for (ad <- otherClusters){
            if (ad != null && request._1 != null){
              println("trying to get "+request._1 + "from "+ad)
              val actor = Await.result(context.actorSelection(ad + request._1.relativePath).resolveOne(2 seconds),2 seconds)
              if (actor != null) {
                request._2.map(someone => {
                  println("sending to "+ someone + " the actor "+ actor)
                  someone ! ActorFromRelativePath(request._1.relativePath, actor)
                })

              }
            }
          }
        }
      }catch {
        case t: Throwable => println("could not get actor from ")
      }
      requests =  Map.empty
    }

  }

  def addSenderToRequests(g: GetActor) {
    val setOfActors: Set[ActorRef] = requests.get(g).getOrElse(Set.empty)
    val withSender = setOfActors + sender
    requests = requests + (g -> withSender)
    println("requests :"+requests)
  }
}


