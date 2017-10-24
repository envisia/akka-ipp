package de.envisia

import java.nio.charset.StandardCharsets
import akka.util.ByteString
import de.envisia.Response.IppResponse
import de.envisia.util.IppHelper

import scala.annotation.tailrec

class Response(x: ByteString) {

  //val path = Paths.get("")
  //Files.size(path)
  //FileIO.fromPath()

  def getResponse: IppResponse = {

    val bb      = x.asByteBuffer
    val version = IppHelper.bytes2hex(Array(bb.get, bb.get)) // TODO format
    println(s"Version: $version")
    val statusCode = bb.getShort
    val requestId  = bb.getInt
    println(s"Request ID: $requestId")


    @tailrec
    def parseAttributes(groupByte: Byte, attributes: Map[String, List[String]]): Map[String, List[String]] = {
      val byte = bb.get()

      if (byte == 0x03.toByte) {
        attributes
      } else {
        val (newGroup, attrTag) = {
          if (byte >= 0x00.toByte && byte <= 0x05.toByte) {
            // Gruppe
            val newGroup = byte
            // Attribut Tag
            val attr = bb.get()
            (newGroup, attr)
          } else {
            // Attribut Tag
            (groupByte, byte)
          }
        }

        // Name
        val shortLenName = bb.getShort()
        val name         = new String(IppHelper.fromBuffer(bb, shortLenName), StandardCharsets.UTF_8)

        // Value
        val shortLenValue = bb.getShort()
        val value         = new String(IppHelper.fromBuffer(bb, shortLenValue), StandardCharsets.UTF_8)

        val tag = attributes.get(name)

        parseAttributes(newGroup, attributes + (name -> tag.getOrElse(value :: Nil))) // TODO, get sub list !!!
      }
    }

    /*
      isEnd match {
        case pos if pos == 0x03.toByte => println("end of response")
        case a =>
          if (a != 0x04.toByte)
            bb.position(position)
          // attribute
          val tag = bb.get
          println(s"Tag: $tag")

          // Name
          val shortLenName = bb.getShort()
          println(s"Name Len: $shortLenName")
          val name = new String(IppHelper.fromBuffer(bb, shortLenName), StandardCharsets.UTF_8)

          // Value
          val shortLenValue = bb.getShort()
          val value         = new String(IppHelper.fromBuffer(bb, shortLenValue), StandardCharsets.UTF_8)

          position = bb.position()

          attributesMap += (name -> value)
          parseAttributes(bb.get, bb.position())

      }
     */

    val attrs = parseAttributes(0x01.toByte, Map.empty) //TODO group byte?

    val result = IppResponse(2, statusCode, requestId, attrs)

    println(result)
    println(attrs.size)

    result
  }

}

object Response {
  case class IppResponse(version: Short, statusCode: Short, requestId: Int, attributes: Map[String, List[String]])
}
