package org.elasticsearch.test.integration.cluster

import com.traackr.scalastic.elasticsearch._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AbstractZenNodesTests extends MultiNodesBasedTests {
  override def defaultSettings = super.defaultSettings + ("discovery.type" -> "zen")
}