package de.envisia.akka.ipp.attributes

import org.apache.pekko.util.ByteString

case class Attribute(tag: Byte, name: String, valueLength: Int, value: ByteString)
