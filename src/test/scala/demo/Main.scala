package demo

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.FileIO
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import de.envisia.akka.ipp.{IPPClient, IPPConfig}
import de.envisia.akka.ipp.attributes.Attributes._
import de.envisia.akka.ipp.attributes.CollectionBuilder

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

object Main {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem           = ActorSystem()
    implicit val mat: Materializer                  = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val http = Http()

    val client =
      new IPPClient(http)(mat, executionContext)

    val config = IPPConfig("192.168.179.196", port = 631)

    try {
      val attr = Await.result(client.printerAttributes(config), 10.seconds)
      println(s"Attributes: $attr")
    } catch {
      case NonFatal(t) =>
        println(s"Error: $t")
    }

    val x = ByteString(Files.readAllBytes(Paths.get("/Users/schmitch/Downloads/SHIPMENT_LABEL.pdf")))

    val attribute =
      CollectionBuilder.newBuilder().addMember(0x44.toByte, "media-type", "labels").result(0x34.toByte, "media-col")

    val attributes = attribute :: Nil
    val printJob   = client.printJob(x, config, "dummy", attributes)
    try {
      val y = Await.result(printJob, 10.seconds)
      val x = Await.result(client.poll(y.jobData.jobID, config), 10.seconds)

      println(s"Printed: $y | $x")
    } catch {
      case NonFatal(t) =>
        println("error")
        t.printStackTrace()
    }

    http.shutdownAllConnectionPools().onComplete(_ => actorSystem.terminate())

  }

}
