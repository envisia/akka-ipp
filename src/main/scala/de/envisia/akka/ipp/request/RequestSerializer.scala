package de.envisia.akka.ipp.request

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

import scala.reflect.runtime.universe._
import akka.util.ByteString
import de.envisia.akka.ipp.attributes.Attribute
import de.envisia.akka.ipp.attributes.Attributes.{ATTRIBUTE_GROUPS, IPP_MINOR_VERSION, IPP_VERSION, RESERVED}
import de.envisia.akka.ipp.request.RequestBuilder.Request._

private[request] class RequestSerializer(
    attributes: Map[String, (Byte, String)] = Map.empty[String, (Byte, String)],
    jobAttributes: List[Attribute] = Nil
) {

  private[this] implicit val bO: ByteOrder = ByteOrder.BIG_ENDIAN

  // generic byte strings
  @inline protected[request] final def putHeader(operationId: Byte, requestId: Int): ByteString =
    ByteString.newBuilder
      .putBytes(Array(IPP_VERSION, IPP_MINOR_VERSION))
      .putBytes(Array(RESERVED, operationId))
      .putInt(requestId)
      .putByte(ATTRIBUTE_GROUPS("operation-attributes-tag"))
      .result()
  protected[request] final def putAttribute(name: String): ByteString =
    attributes(name) match {
      case (byte, value) =>
        ByteString.newBuilder
          .putByte(byte)
          .putShort(name.length)
          .putBytes(name.getBytes(StandardCharsets.UTF_8))
          .putShort(value.length)
          .putBytes(value.getBytes(StandardCharsets.UTF_8))
          .result()
      case _ => throw new IllegalStateException("could not serialize malformed request")
    }

  protected final def putByte(byte: Byte): ByteString = ByteString.newBuilder.putByte(byte).result()

  protected final def renderAttributes: ByteString = {
    println(s"Job Attributes: ${jobAttributes.nonEmpty}")
    if (jobAttributes.nonEmpty) {
      putByte(0x02) ++ jobAttributes
        .foldLeft(ByteString.newBuilder) {
          case (builder, attribute) =>
            builder
              .putByte(attribute.tag)
              .putShort(attribute.name.length)
              .putBytes(attribute.name.getBytes(StandardCharsets.UTF_8))
              .putShort(attribute.valueLength)
              .append(attribute.value)
        }
        .result()
    } else {
      ByteString.empty
    }
  }

  /**
    * method for inserting the jobId
    * @param name
    * @return
    */
  protected[request] final def putInteger(name: String): ByteString =
    attributes(name) match {
      case (byte, value) =>
        ByteString.newBuilder
          .putByte(byte)
          .putShort(name.length)
          .putBytes(name.getBytes(StandardCharsets.UTF_8))
          .putShort(4) // MAX INT
          .putInt(value.toInt)
          .result()
      case _ => throw new IllegalStateException("could not serialize malformed request")
    }
  @inline protected[request] val putEnd: ByteString =
    ByteString.newBuilder
      .putByte(ATTRIBUTE_GROUPS("end-of-attributes-tag"))
      .result()

  protected[request] def serialize[A](oid: Byte, reqId: Int)(implicit tag: TypeTag[A]): ByteString = {
    val base = putHeader(oid, reqId) ++
      putAttribute("attributes-charset") ++
      putAttribute("attributes-natural-language") ++
      putAttribute("printer-uri")
    tag match {
      case t if t == typeTag[CancelJob] =>
        base ++ putAttribute("job-uri") ++ putAttribute("requesting-user-name") ++ putEnd
      case t if t == typeTag[GetPrinterAttributes] => base ++ putEnd
      case t if t == typeTag[PrintJob] =>
        base ++
          putAttribute("requesting-user-name") ++
          putAttribute("job-name") ++
          putAttribute("document-format") ++
          renderAttributes ++ // job attributes
          putEnd
      case t if t == typeTag[GetJobAttributes] =>
        base ++ putInteger("job-id") ++ putAttribute("requesting-user-name") ++ putEnd
      case _ => throw new IllegalArgumentException("wrong request type")
    }

  }

}
