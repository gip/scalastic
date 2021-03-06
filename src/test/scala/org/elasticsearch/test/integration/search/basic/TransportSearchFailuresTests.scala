package org.elasticsearch.test.integration.search.basic

import org.elasticsearch._
import org.elasticsearch.action._
import org.elasticsearch.action.admin.cluster.health._
import org.elasticsearch.action.search._
import org.elasticsearch.client.Requests._
import org.elasticsearch.common._, xcontent.XContentFactory._
import com.traackr.scalastic.elasticsearch._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])class TransportSearchFailuresTests extends MultiNodesBasedTests {
  
  test("testFailedSearchWithWrongQuery") {
	startNode("server1")
    indexer("server1").createIndex(indexName, settings = Map("numberOfReplicas" -> "2", "numberOfShards" -> "3", "routing.hash.type" -> "simple"))
    indexer("server1").waitForYellowStatus()
    for (i <- 0 until 100) {
      val id = i.toString
      val nameValue = "test"
      val age = i
      indexer("server1").index(indexName, "type1", "1", source(id, nameValue, age), consistencyLevel = Some(WriteConsistencyLevel.ONE))
    }
    var response = indexer("server1").refresh()
    response.totalShards() should be === (9)
    response.successfulShards() should be === (3)
    response.failedShards() should be === (0)
    for (i <- 0 until 5) {
      try {
        val response = indexer("server1").client.search(searchRequest(indexName).source(Unicode.fromStringAsBytes("{ xxx }"))).actionGet
        response.totalShards() should be === (3)
        response.successfulShards() should be === (0)
        response.failedShards() should be === (3)
        fail("search should fail")
      } catch {
        case e: ElasticSearchException => e.unwrapCause().getClass should be === classOf[SearchPhaseExecutionException]
      }
    }
    
    startNode("server2")
    indexer("server1").waitForNodes(howMany = "2").timedOut() should be === (false)
    val clusterHealth = indexer("server1").health_prepare().setWaitForYellowStatus().setWaitForRelocatingShards(0).setWaitForActiveShards(6).execute.actionGet
    clusterHealth.timedOut() should be === (false)
    clusterHealth.status() should be === (ClusterHealthStatus.YELLOW)
    clusterHealth.activeShards() should be === (6)
    response = indexer("server1").refresh()
    response.totalShards() should be === (9)
    response.successfulShards() should be === (6)
    response.failedShards() should be === (0)
    for (i <- 0 until 5) {
      try {
        val response = indexer("server1").client.search(searchRequest(indexName).source(Unicode.fromStringAsBytes("{ xxx }"))).actionGet
        response.totalShards() should be === (3)
        response.successfulShards() should be === (0)
        response.failedShards() should be === (3)
        fail("search should fail")
      } catch {
        case e: ElasticSearchException => e.unwrapCause().getClass should be === classOf[SearchPhaseExecutionException]
      }
    }
  }

  private def source(id: String, nameValue: String, age: Int) = {
    val buffer = new StringBuilder().append(nameValue)
    for (i <- 0 until age) buffer.append(" ").append(nameValue)
    jsonBuilder().startObject().field("id", id).field("name", nameValue + id)
      .field("age", age)
      .field("multi", buffer.toString)
      .field("_boost", age * 10)
      .endObject()
      .string
  }
}
