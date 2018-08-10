package de.envisia.akka.ipp.attributes

import akka.util.ByteString

case class Attribute(tag: Byte, name: String, valueLength: Int, value: ByteString)
