package de.envisia.akka.ipp.services

import de.envisia.akka.ipp.Response.{GetJobAttributesResponse, JobData}
import de.envisia.akka.ipp.{IPPClient, IPPConfig}
import org.apache.pekko.stream.{Attributes, Outlet, SourceShape}
import org.apache.pekko.stream.stage.{AsyncCallback, GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

private[ipp] class JobStateSource(jobId: Int, client: IPPClient, config: IPPConfig)(implicit ec: ExecutionContext)
    extends GraphStage[SourceShape[JobData]] {

  private val out: Outlet[JobData]              = Outlet("JobStatusSource.out")
  override lazy val shape: SourceShape[JobData] = SourceShape.of(out)
  private val logger                            = LoggerFactory.getLogger(this.getClass)
  private final val MAX_POLL_COUNT              = 150
  private var count                             = 0

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {

    val callback: AsyncCallback[Try[GetJobAttributesResponse]] = getAsyncCallback[Try[GetJobAttributesResponse]] {
      case Success(value) =>
        logger.info("Polling")
        if ((6 to 9).contains(value.jobData.jobState)) {
          logger.info("Completed")
          push(out, value.jobData)
          completeStage()
        } else {
          if (count < MAX_POLL_COUNT) {
            scheduleOnce(None, config.pollingInterval)
            count = count + 1
          } else {
            val t = s"Max polling count exceeded: $MAX_POLL_COUNT"
            logger.info(t)
            push(out, value.jobData.copy(jobState = 8, jobStateReasons = "print job timeout"))
            completeStage()
          }
        }
      case Failure(t) =>
        logger.info("Failed")
        fail(out, t)
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit =
        client.getJobAttributes(jobId, config).onComplete(callback.invoke)
    })

    override protected def onTimer(timerKey: Any): Unit =
      client.getJobAttributes(jobId, config).onComplete(callback.invoke)

  }
}
