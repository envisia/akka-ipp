package de.envisia.akka.ipp.services

import de.envisia.akka.ipp.{IPPClient, IPPConfig}
import de.envisia.akka.ipp.Response.JobData
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import org.apache.pekko.stream.{Materializer, SharedKillSwitch}

import scala.concurrent.{ExecutionContext, Future}

private[ipp] class PollingService(client: IPPClient, killSwitch: SharedKillSwitch)(
    implicit mat: Materializer,
    val ec: ExecutionContext
) {

  private[ipp] def poll(jobId: Int, config: IPPConfig): Future[JobData] =
    Source.fromGraph(new JobStateSource(jobId, client, config)).viaMat(killSwitch.flow)(Keep.left).runWith(Sink.head)

}
