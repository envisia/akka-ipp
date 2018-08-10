package de.envisia.akka.ipp

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.MediaType.NotCompressible
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{KillSwitches, Materializer}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import de.envisia.akka.ipp.OperationType._
import de.envisia.akka.ipp.Response._
import de.envisia.akka.ipp.attributes.Attribute
import de.envisia.akka.ipp.services.{PollingService, RequestService}
import org.slf4j.LoggerFactory

import scala.reflect.runtime.universe._
import scala.concurrent.{ExecutionContext, Future}

class IPPClient(http: HttpExt)(
    implicit mat: Materializer,
    val ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(getClass)
  logger.debug("IPPClient initialized")

  private val killSwitch = KillSwitches.shared("printer")

  private val atomicInt = new AtomicInteger(0)

  private def getRequestId: Int =
    atomicInt.updateAndGet(
      x => if (x + 1 == Int.MaxValue) 1 else x + 1
    )

  private val ippContentType = ContentType(MediaType.customBinary("application", "ipp", NotCompressible))

  def cancelJob(jobId: Int, config: IPPConfig): Future[CancelJobResponse] =
    dispatch[CancelJobResponse](CancelJob(jobId), config)

  def printJob(
      data: ByteString,
      config: IPPConfig,
      jobName: String = "",
      attributes: List[Attribute] = Nil
  ): Future[PrintJobResponse] =
    dispatch[Response.PrintJobResponse](PrintJob(data, attributes), config, jobName)

  def printerAttributes(config: IPPConfig): Future[GetPrinterAttributesResponse] =
    dispatch[GetPrinterAttributesResponse](GetPrinterAttributes, config)

  def getJobAttributes[T <: IppResponse](jobId: Int, config: IPPConfig): Future[GetJobAttributesResponse] =
    dispatch[GetJobAttributesResponse](GetJobAttributes(jobId), config)

  def poll(jobId: Int, config: IPPConfig): Future[Response.JobData] =
    new PollingService(this, killSwitch).poll(jobId, config)

  final protected[ipp] def dispatch[A <: IppResponse](ev: OperationType, config: IPPConfig, jobName: String = "")(
      implicit tag: TypeTag[A]
  ): Future[A] = {

    val service = new RequestService(
      path = config.path,
      "ipp://" + config.host,
      queue = config.queue,
      requestId = getRequestId,
      jobName = jobName
    )

    val body: ByteString = ev match {
      case CancelJob(jobId)        => service.cancelJob(CancelJob(jobId).operationId, jobId)
      case p: PrintJob             => service.printJob(p.operationId, p.data, p.attributes)
      case GetPrinterAttributes    => service.getPrinterAttributes(GetPrinterAttributes.operationId)
      case GetJobAttributes(jobId) => service.getJobAttributes(GetJobAttributes(jobId).operationId, jobId)
    }

    val pathValue = ev match {
      case GetPrinterAttributes => ""
      case _                    => service.printPath
    }

    val ntt      = HttpEntity(ippContentType, Source.single(body))
    val request  = HttpRequest(HttpMethods.POST, uri = s"http://${config.host}:${config.port}$pathValue", entity = ntt)
    val response = this.execute(request)
    val result = response.flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[ByteString]
      case resp => Future.failed(new Exception(s"Unexpected status code ${resp.status}"))
    }
    result.map(bs => new Response(bs).get[A](ev))
  }

  def execute(request: HttpRequest): Future[HttpResponse] = http.singleRequest(request)

  def shutdown(): Future[Unit] =
    Future.successful(killSwitch.shutdown())

}
