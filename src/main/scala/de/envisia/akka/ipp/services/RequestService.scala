package de.envisia.akka.ipp.services

import akka.util.ByteString
import de.envisia.akka.ipp.attributes.Attribute
import de.envisia.akka.ipp.request.RequestBuilder.Request._
import de.envisia.akka.ipp.attributes.Attributes._
import de.envisia.akka.ipp.request.RequestBuilder

private[ipp] class RequestService(
    path: Option[String],
    uri: String,
    lang: String = "de",
    user: String = "dummy",
    queue: String,
    jobName: String = "",
    charset: String = "utf-8",
    format: String = "application/pdf",
    requestId: Int = 1
) {

  def simplePath: String = path.map(p => s"/$p").getOrElse("")

  def printPath: String = simplePath + "/" + queue

  private def generatedUri = s"$uri:$WELL_KNOWN_PORT"

  def cancelJob(operationId: Byte, jobId: Int): ByteString =
    new RequestBuilder[CancelJob]()
      .setCharset(charset)
      .setUri(generatedUri + printPath)
      .setLanguage(lang)
      .setUser(user)
      .setJobUri(generatedUri + printPath + "/job-" + jobId)
      .build[CancelJob](operationId, requestId)
      .request

  def getPrinterAttributes(operationId: Byte): ByteString =
    new RequestBuilder[GetPrinterAttributes]()
      .setCharset(charset)
      .setUri(generatedUri + simplePath)
      .setLanguage(lang)
      .build[GetPrinterAttributes](operationId, requestId)
      .request

  def getJobAttributes(operationId: Byte, jobId: Int): ByteString =
    new RequestBuilder[GetJobAttributes]()
      .setCharset(charset)
      .setUri(generatedUri + printPath)
      .setLanguage(lang)
      .askWithJobId(jobId)
      .setUser(user)
      .build[GetJobAttributes](operationId, requestId)
      .request

  def printJob(operationId: Byte, data: ByteString, attributes: List[Attribute]): ByteString =
    new RequestBuilder[PrintJob]()
      .setCharset(charset)
      .setUri(generatedUri + printPath)
      .setLanguage(lang)
      .setUser(user)
      .setJobName(jobName)
      .setFormat(format)
      .setJobAttributes(attributes)
      .build[PrintJob](operationId, requestId)
      .request ++ data

}
