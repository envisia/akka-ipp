package de.envisia.akka.ipp

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import de.envisia.akka.ipp.Response._
import de.envisia.akka.ipp.attributes.Attributes._
import de.envisia.akka.ipp.attributes.IPPValue
import de.envisia.akka.ipp.attributes.IPPValue.{NumericVal, TextVal}
import de.envisia.akka.ipp.util.IppHelper
import org.apache.pekko.util.ByteString

import scala.reflect.runtime.universe._
import scala.annotation.tailrec

private[ipp] class Response(bs: ByteString) {

  private[this] val bb = bs.asByteBuffer

  def get[A <: IppResponse](o: OperationType)(implicit tTag: TypeTag[A]): A = {

    val version    = Array(bb.get, bb.get)(0)
    val statusCode = bb.getShort
    val requestId  = bb.getInt

    val attrs = parseAttributes(0x01.toByte, Map.empty) // TODO group byte? groupbyte not yet used
    val result = o.operationId match {
      case x if x == OPERATION_IDS("Get-Printer-Attributes") =>
        GetPrinterAttributesResponse(o.operationId, version.toShort, statusCode, requestId, attrs)
      case x if x == OPERATION_IDS("Print-Job") =>
        PrintJobResponse(
          o.operationId,
          version.toShort,
          statusCode,
          requestId,
          attrs,
          JobData(
            attrs("job-id").headOption.map(_.asInstanceOf[NumericVal].value).getOrElse(-1),
            attrs("job-state").headOption.map(_.asInstanceOf[NumericVal].value).getOrElse(-1),
            attrs("job-uri").headOption.map(_.asInstanceOf[TextVal].value).getOrElse(""),
            attrs("job-state-reasons").headOption.map(_.asInstanceOf[TextVal].value).getOrElse("")
          )
        )
      case x if x == OPERATION_IDS("Get-Job-Attributes") =>
        GetJobAttributesResponse(
          o.operationId,
          version.toShort,
          statusCode,
          requestId,
          attrs,
          JobData(
            attrs("job-id").headOption.map(_.asInstanceOf[NumericVal].value).getOrElse(-1),
            attrs("job-state").headOption.map(_.asInstanceOf[NumericVal].value).getOrElse(-1),
            attrs("job-uri").headOption.map(_.asInstanceOf[TextVal].value).getOrElse(""),
            attrs("job-state-reasons").headOption.map(_.asInstanceOf[TextVal].value).getOrElse("")
          )
        )
      case x if x == OPERATION_IDS("Cancel-Job") =>
        CancelJobResponse(o.operationId, version.toShort, statusCode, requestId, attrs)
    }
    typeOf[A] match {
      case t if t =:= typeOf[GetPrinterAttributesResponse] => result.asInstanceOf[A]
      case t if t =:= typeOf[GetJobAttributesResponse]     => result.asInstanceOf[A]
      case t if t =:= typeOf[PrintJobResponse]             => result.asInstanceOf[A]
      case t if t =:= typeOf[CancelJobResponse]            => result.asInstanceOf[A]
      case _                                               => throw new IllegalStateException("wrong response type found")
    }
  }

  @tailrec
  private[this] final def parseAttributes(
      groupByte: Byte,
      attributes: Map[String, List[IPPValue]]
  ): Map[String, List[IPPValue]] = {
    val byte = bb.get()
    if (byte == ATTRIBUTE_GROUPS("end-of-attributes-tag")) {
      attributes
    } else {
      val (newGroup, attrTag) = {
        if ((0 to 5).contains(byte.toInt)) { // delimiter tag values: https://tools.ietf.org/html/rfc8010#section-3.5.1
          // group
          val newGroup = byte
          // attribute tag
          val attr = bb.get()
          (newGroup, attr)
        } else {
          // attribute tag
          (groupByte, byte)
        }
      }
      // name
      val shortLenName = bb.getShort()
      val name         = new String(IppHelper.fromBuffer(bb, shortLenName.toInt), StandardCharsets.UTF_8)
      // value
      val shortLenValue = bb.getShort()
      val value = attrTag match {
        case b if !NUMERIC_TAGS.contains(b) =>
          TextVal(new String(IppHelper.fromBuffer(bb, shortLenValue.toInt), StandardCharsets.UTF_8))
        case _ =>
          NumericVal(ByteBuffer.wrap(IppHelper.fromBuffer(bb, shortLenValue.toInt)).getInt) // TODO find out the value-tags of other types and continue pattern matching on them
      }
      val tag = attributes.get(name)
      parseAttributes(newGroup, attributes + (name -> tag.map(v => value :: v).getOrElse(value :: Nil)))
    }
  }

}

object Response {

  sealed trait IppResponse

  case class CancelJobResponse(
      oid: Byte,
      version: Short,
      statusCode: Short,
      requestId: Int,
      attributes: Map[String, List[IPPValue]]
  ) extends IppResponse

  case class GetPrinterAttributesResponse(
      oid: Byte,
      version: Short,
      statusCode: Short,
      requestId: Int,
      attributes: Map[String, List[IPPValue]]
  ) extends IppResponse

  case class GetJobAttributesResponse(
      oid: Byte,
      version: Short,
      statusCode: Short,
      requestId: Int,
      attributes: Map[String, List[IPPValue]],
      jobData: JobData
  ) extends IppResponse

  case class PrintJobResponse(
      oid: Byte,
      version: Short,
      statusCode: Short,
      requestId: Int,
      attributes: Map[String, List[IPPValue]],
      jobData: JobData
  ) extends IppResponse

  case class JobData(
      jobID: Int,
      jobState: Int,
      jobURI: String,
      jobStateReasons: String
  )

}
