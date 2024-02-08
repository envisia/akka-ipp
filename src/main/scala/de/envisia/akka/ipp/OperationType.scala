package de.envisia.akka.ipp

import de.envisia.akka.ipp.attributes.Attribute
import de.envisia.akka.ipp.attributes.Attributes._
import org.apache.pekko.util.ByteString

sealed abstract class OperationType(val name: String, val operationId: Byte)

private[ipp] object OperationType {

  case object GetPrinterAttributes
      extends OperationType("Get-Printer-Attributes", OPERATION_IDS("Get-Printer-Attributes"))

  case class PrintJob(data: ByteString, attributes: List[Attribute])
      extends OperationType("Print-Job", OPERATION_IDS("Print-Job"))

  case class GetJobAttributes(jobId: Int)
      extends OperationType("Get-Job-Attributes", OPERATION_IDS("Get-Job-Attributes"))

  case class CancelJob(jobId: Int) extends OperationType("Cancel-Job", OPERATION_IDS("Cancel-Job"))

}
