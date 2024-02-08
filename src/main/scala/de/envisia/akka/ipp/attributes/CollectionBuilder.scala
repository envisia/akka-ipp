package de.envisia.akka.ipp.attributes

import org.apache.pekko.util.{ByteString, ByteStringBuilder}

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object CollectionBuilder {

  private implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

  class Builder(builder: ByteStringBuilder) {
    private var size: Int = 0

    def addMember(tag: Byte, name: String, value: String): Builder = {
      size += 1
      builder
        .putByte(0x4a.toByte)
        .putShort(0)
        .putShort(name.length)
        .putBytes(name.getBytes(StandardCharsets.UTF_8))
        .putByte(tag)
        .putShort(0)
        .putShort(value.length)
        .putBytes(value.getBytes(StandardCharsets.UTF_8))
      this
    }

    def result(tag: Byte, name: String): Attribute = {
      val data = {
        if (size == 0) ByteString.empty
        else builder.putByte(0x37.toByte).putShort(0).putShort(0).result()
      }
      Attribute(tag, name, 0, data)
    }
  }

  def newBuilder(): CollectionBuilder.Builder = {
    val builder = ByteString.newBuilder

    new Builder(builder)
  }

}
