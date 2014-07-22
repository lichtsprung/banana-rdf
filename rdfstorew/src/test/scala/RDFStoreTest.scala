package org.w3.banana.rdfstorew

import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.diesel._
import org.w3.banana.binder._
import scalaz.Scalaz._
import scala.util._
import org.joda.time.DateTime
import scala.collection.immutable.ListMap


import scala.scalajs.js
import scala.scalajs.test.JasmineTest

object IsomorphismTest extends JasmineTest {

  import org.w3.banana.rdfstorew.RDFStore._
  import Ops._
  import org.w3.banana.diesel._
  import org.w3.banana.rdfstorew.GraphEquivalence._


  def foaf(tag: String) = URI("http://xmlns.com/foaf/0.1/"+tag)

  val hjs = URI("http://bblfish.net/people/henry/card#me")

  val timbl = URI("http://www.w3.org/People/Berners-Lee/card#i")
  def alex(i: Int) = BNode("alex"+i)
  def antonio(i: Int) = BNode("antonio"+i)

  val groundedGraph = (
    toPointedGraphW[RDFStore](hjs)
      -- foaf("knows") ->- timbl
      -- foaf("name") ->- "Henry Story"
    ).graph

  //  val bnodeGraph = (
  //      toPointedGraphW[Plantain](URI("#me"))
  //        -- foaf("knows") ->- toPointedGraphW[Plantain](bnode("alex"))
  //    ).graph union (
  //      toPointedGraphW[Plantain](bnode("alex"))
  //        -- foaf("name") ->- "Alexandre Bertails"
  //    ).graph


  def bnAlexRel1Graph(i: Int=1) = Graph(Triple(alex(i), foaf("homePage"), URI("http://bertails.org/")))

  def bnAlexRel2Graph(i: Int=1) = Graph(
    Triple(hjs, foaf("knows"), alex(i)),
    Triple(alex(i), foaf("name"), "Alexandre Bertails".toNode)
  )


  def bnAntonioRel1Graph(i: Int=1) = Graph(Triple(antonio(i), foaf("homePage"), URI("https://github.com/antoniogarrote/")))

  def bnAntonioRel2Graph(i: Int=1) = Graph(
    Triple(hjs, foaf("knows"), antonio(i)),
    Triple(antonio(i), foaf("name"), "Antonio Garrote".toNode)
  )

  def xbn(i: Int) = BNode("x"+i)

  def bnKnowsBN(i:Int, j: Int) = Graph(
    Triple(xbn(i),foaf("knows"),xbn(j))
  )

  def symmetricGraph(i: Int, j: Int) = bnKnowsBN(i,j) union bnKnowsBN(j,i)

  def owlSameAs(node1: RDFStoreRDFNode,node2: RDFStoreRDFNode) =
    Graph(Triple(node1,URI("http://www.w3.org/2002/07/owl#sameAs"),node2))


  describe("test groundTripleFilter(graph)") {

    it("a completely grounded graph ( no blank nodes ) ") {
      val (grounded, nongrounded) = groundTripleFilter(groundedGraph)
      expect(grounded == groundedGraph).toEqual(true)
      expect(nongrounded == emptyGraph).toEqual(true)
    }

    it("an ungrounded graph ( all statements contain a bnode )") {
      val (grounded, nongrounded) = groundTripleFilter(bnAlexRel2Graph())
      expect(grounded == emptyGraph).toEqual(true)
      expect(nongrounded == nongrounded).toEqual(true)
    }

    it("a graph with grounded and ungrounded statements ") {
      val (grounded, nongrounded) = groundTripleFilter(groundedGraph union bnAlexRel2Graph())
      expect(grounded == groundedGraph).toEqual(true)
      expect(nongrounded == nongrounded).toEqual(true)
    }

  }

  describe("test categorisation of bnodes") {

    it("one bnode with 1 relation") {
      val clz = bnodeClassify(bnAlexRel1Graph())
      expect(clz.size).toEqual(1)
      expect(clz.head._2.size).toEqual(1) // only one bnode in this graph
      expect(clz.head._1 == (new VerticeType(List((foaf("homePage"), 1)), List()))).toEqual(true)

      val clz2 = bnodeClassify(bnAntonioRel1Graph())
      expect(clz2.size).toEqual(1)
      expect(clz2.head._2.size).toEqual(1) // only one bnode in this graph
      expect(clz2.head._1 == (new VerticeType(List((foaf("homePage"), 1)), List()))).toEqual(true)

    }

    it("one bnode with 2 relations") {
      val clz = bnodeClassify(bnAlexRel2Graph())
      expect(clz.size).toEqual(1)
      expect(clz.head._2.size).toEqual(1) // only one bnode in this classification
      expect(clz.head._1 == (new VerticeType(List((foaf("name"), 1)), List((foaf("knows"), 1))))).toEqual(true)

      val clz2 = bnodeClassify(bnAntonioRel2Graph())
      expect(clz2.size).toEqual(1)
      expect(clz2.head._2.size).toEqual(1) // only one bnode in this classification
      expect(clz2.head._1 == (new VerticeType(List((foaf("name"), 1)), List((foaf("knows"), 1))))).toEqual(true)
    }

    it("one bnode with 3 relations") {
      val clz = bnodeClassify(bnAlexRel1Graph() union bnAlexRel2Graph())
      expect(clz.size).toEqual(1)
      expect(clz.head._2.size).toEqual(1) // only one bnode in this classification
      expect(clz.head._1 == (new VerticeType(List((foaf("name"), 1), (foaf("homePage"), 1)), List((foaf("knows"), 1))))).toEqual(true)

      val clz2 = bnodeClassify(bnAntonioRel1Graph() union bnAntonioRel2Graph())
      expect(clz2.size).toEqual(1)
      expect(clz2.head._2.size).toEqual(1) // only one bnode in this classification
      expect(clz2.head._1 == (new VerticeType(List((foaf("name"), 1), (foaf("homePage"), 1)), List((foaf("knows"), 1))))).toEqual(true)
    }

    it("two bnodes with each same type of relation") {
      val bnGr = bnAlexRel1Graph() union bnAntonioRel1Graph()
      val clz = bnodeClassify(bnGr)
      expect(clz.size).toEqual(1)
      expect(clz.head._2.size).toEqual(2) // 2 bnodes in this classification
      expect(clz.head._1 == (new VerticeType(List((foaf("homePage"), 1)), List()))).toEqual(true)
    }

    it("two bnodes with each 2 relations of same type") {
      val bnGr = bnAlexRel2Graph() union bnAntonioRel2Graph()
      val clz = bnodeClassify(bnGr)
      expect(clz.size).toEqual(1)
      expect(clz.head._2.size).toEqual(2) // 2 bnodes in this classification
      expect(clz.head._1 == (new VerticeType(List((foaf("name"), 1)), List((foaf("knows"),1))))).toEqual(true)
    }


  }

  describe("test bnode mapping solutions ") {
    it("two graphs with 1 relation and 1 bnode") {
      val maps = bnodeMappingGenerator(bnAlexRel1Graph(1),bnAlexRel1Graph(2))
      expect(maps == Success(ListMap(alex(1)->Set(alex(2))))).toEqual(true)
      val answer = findAnswer(bnAlexRel1Graph(1),bnAlexRel1Graph(2))
      expect(answer == Success(List(alex(1)->(alex(2))))).toEqual(true)
    }

    it("two graphs with 2 relation and 1 bnode each") {
      val maps = bnodeMappingGenerator(bnAlexRel2Graph(1),bnAlexRel2Graph(2))
      expect(maps == Success(ListMap(alex(1)->Set(alex(2))))).toEqual(true)
      val answer = findAnswer(bnAlexRel2Graph(1),bnAlexRel2Graph(2))
      expect(answer == Success(List(alex(1)->(alex(2))))).toEqual(true)
    }

    it("two graphs with 3 relations and 1 bnode each ") {
      val maps = bnodeMappingGenerator(
        bnAlexRel1Graph(1) union bnAlexRel2Graph(1),
        bnAlexRel1Graph(2) union bnAlexRel2Graph(2))
      expect(maps == Success(ListMap(alex(1)->Set(alex(2))))).toEqual(true)
      val answer = findAnswer(
        bnAlexRel1Graph(1) union bnAlexRel2Graph(1),
        bnAlexRel1Graph(2) union bnAlexRel2Graph(2))
      expect(answer == Success(List(alex(1)->(alex(2))))).toEqual(true)
    }

    it("two graphs with 2 relations and 2 bnodes each") {
      for (l <- findPossibleMappings(
        bnAlexRel1Graph(1) union bnAntonioRel1Graph(1),
        bnAlexRel1Graph(2) union bnAntonioRel1Graph(2))) {
        //with this system of categorisation the categories are very light
        // and they don't distinguish the literals
        //also the returned set covers symmetric results - this can also be optimised!
        expect(l.size).toEqual(4)
      }

      val answer = findAnswer(
        bnAlexRel1Graph(1) union bnAntonioRel1Graph(1),
        bnAlexRel1Graph(2) union bnAntonioRel1Graph(2)
      )
      expect(answer.isSuccess).toEqual(true)
      expect(answer.get.size).toEqual(2)
      expect(answer.get.contains(alex(1)->alex(2))).toEqual(true)
      expect(answer.get.contains(antonio(1)->antonio(2))).toEqual(true)
    }

    it("two graphs with 3 relations each. | But one category has 1 solution the other that has two. | The category with 1 solutions must be shown first") {
      val g1 = bnAlexRel1Graph(1) union bnAntonioRel1Graph(1) union bnAlexRel2Graph(2)    union bnAlexRel1Graph(0) union bnAlexRel2Graph(0)
      val g2 = bnAlexRel1Graph(3) union bnAntonioRel1Graph(3) union bnAntonioRel2Graph(4) union bnAlexRel1Graph(5) union bnAlexRel2Graph(5)
      val answers = findPossibleMappings(g1,g2)
      val answer= findAnswer(g1,g2)
      expect(answer.isFailure).toEqual(true)

    }
  }

  describe("test bnode mapping") {

    it("graphs mapped to themselves") {
      val a1g = bnAlexRel1Graph(0)
      expect(mapVerify(a1g, a1g, Map(alex(0) -> alex(0))) == Nil).toEqual(true)

      expect(mapVerify(bnAntonioRel1Graph(0), bnAntonioRel1Graph(0), Map((antonio(0), antonio(0)))) == Nil).toEqual(true)

    }

    it("1 bnode mapped") {
      expect(mapVerify(bnAlexRel1Graph(0), bnAlexRel1Graph(1), Map((alex(0), alex(1)))) == Nil).toEqual(true)

      expect(mapVerify(bnAntonioRel2Graph(0), bnAntonioRel1Graph(1), Map((antonio(0), antonio(1)))).empty).toEqual(false)
    }

    it("2 bnodes mapped") {
      val r2g1 = bnAlexRel1Graph(0) union bnAntonioRel1Graph(0)
      val r2g2 = bnAlexRel1Graph(1) union bnAntonioRel1Graph(1)

      mapVerify(r2g1, r2g1, Map(alex(0) -> alex(0), antonio(0) -> antonio(0)))

      expect(mapVerify(
        r2g1,
        r2g2,
        Map(alex(0) -> alex(1), antonio(0) -> antonio(1))
      ) == Nil).toEqual(true)

      //an incorrect mapping
      val v = mapVerify(
        r2g1,
        r2g2,
        Map(alex(0) -> antonio(1), antonio(0) -> alex(1))
      )
      expect(v.empty).toEqual(false)

      //reverse test

      expect(mapVerify(
        r2g2,
        r2g1,
        Map(alex(1) -> alex(0), antonio(1) -> antonio(0))
      ) == Nil).toEqual(false)

      //an incorrect mapping
      val v2 = mapVerify(
        r2g2,
        r2g1,
        Map(alex(1) -> antonio(0), antonio(1) -> alex(0))
      )
      expect(v2.empty).toEqual(false)

    }

    it("some symmetric graphs can have more than one mapping - which are thus isomorphic") {

      //some graphs have two mappings
      val symgrph01 = symmetricGraph(0, 1)
      val symgrph23 = symmetricGraph(2, 3)
      expect(mapVerify(symgrph01, symgrph23, Map(xbn(0) -> xbn(2), xbn(1) -> xbn(3))) == Nil).toEqual(true)
      expect(mapVerify(symgrph01, symgrph23, Map(xbn(0) -> xbn(3), xbn(1) -> xbn(2))) == Nil).toEqual(true)

      val symgraph01ext = symgrph01 union owlSameAs(xbn(0), xbn(0)) union owlSameAs(xbn(1), xbn(1))
      val symgraph23ext = symgrph23 union owlSameAs(xbn(2), xbn(2)) union owlSameAs(xbn(3), xbn(3))
      expect(mapVerify(symgraph01ext, symgraph23ext, Map(xbn(0) -> xbn(2), xbn(1) -> xbn(3))) == Nil).toEqual(true)
      expect(mapVerify(symgraph01ext, symgraph23ext, Map(xbn(0) -> xbn(3), xbn(1) -> xbn(2))) == Nil).toEqual(true)

      val oneThing01 = symgraph01ext union owlSameAs(xbn(0), xbn(1)) union owlSameAs(xbn(1), xbn(0))
      val oneThing23 = symgraph23ext union owlSameAs(xbn(2), xbn(3)) union owlSameAs(xbn(3), xbn(2))
      expect(mapVerify(oneThing01, oneThing23, Map(xbn(0) -> xbn(2), xbn(1) -> xbn(3))) == Nil).toEqual(true)
      expect(mapVerify(oneThing01, oneThing23, Map(xbn(0) -> xbn(3), xbn(1) -> xbn(2))) == Nil).toEqual(true)
    }

    it("3 bnodes mapped") {
      val knows3bn = bnKnowsBN(0,1) union bnKnowsBN(1,2) union bnKnowsBN(2,0)

      //three different isomorphic mappings
      expect(mapVerify(knows3bn,knows3bn,Map(xbn(0)->xbn(0),xbn(1)->xbn(1),xbn(2)->xbn(2))) == Nil).toEqual(true)
      expect(mapVerify(knows3bn,knows3bn,Map(xbn(0)->xbn(1),xbn(1)->xbn(2),xbn(2)->xbn(0))) == Nil).toEqual(true)
      expect(mapVerify(knows3bn,knows3bn,Map(xbn(0)->xbn(2),xbn(1)->xbn(0),xbn(2)->xbn(1))) == Nil).toEqual(true)

      val asymmetric = knows3bn union Graph(Triple(xbn(0),foaf("name"),Literal("Tim")))
      expect(mapVerify(asymmetric,asymmetric,Map(xbn(0)->xbn(0),xbn(1)->xbn(1),xbn(2)->xbn(2))) == Nil).toEqual(true)
      expect(mapVerify(asymmetric,asymmetric,Map(xbn(0)->xbn(1),xbn(1)->xbn(2),xbn(2)->xbn(0))).empty).toEqual(false)
      expect(mapVerify(asymmetric,asymmetric,Map(xbn(0)->xbn(2),xbn(1)->xbn(0),xbn(2)->xbn(1))).empty).toEqual(false)

    }
  }

}

class PointedGraphJasmineTester[Rdf <: RDF]()(implicit ops: RDFOps[Rdf])
  extends JasmineTest {

  import ops._

  val henryURI: String = "http://bblfish.net/people/henry/card#me"
  val henry = URI(henryURI)

  describe("Two similarly constructed PointedGraphs only have plain object identity") {

    it("should work correctly with uris") {


      val u1 = URI("http://test.com/something")
      val u2 = URI("http://test.com/something")

      expect(u1.equals(u2)).toEqual(true)
      expect(u1 == u2).toEqual(true)
    }

    it("with URI pointers") {
      val pg1 = PointedGraph(henry, Graph.empty)
      val pg2 = PointedGraph(URI(henryURI))
      val pointer1: RDF#Node = pg1.pointer
      val pointer2: RDF#Node = pg2.pointer

      expect(pg1.pointer == pg2.pointer).toEqual(true)
      expect(pg1.graph.equals(pg2.graph)).toEqual(true)
      //yet
      expect(pg1.equals(pg2)).toEqual(false)
    }

    it("with bnode pointers") {
      val bnode = BNode()
      val graph = Graph(Triple(bnode, rdf.first, Literal.tagged("Henry", Lang("en"))))
      val pg1 = PointedGraph(bnode, graph)
      val pg2 = PointedGraph(bnode, graph)

      expect(pg1.equals(pg2)).toEqual(false)
    }

  }

}

object PointedGraphJasmineTesterRDFStore extends PointedGraphJasmineTester[RDFStore]

//class RDFStoreWTurtleTest extends TurtleTestSuite[RDFStore]

class GraphUnionJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf])
  extends JasmineTest {

  import ops._

  val foo = (
    URI("http://example.com/foo")
      -- rdf("foo") ->- "foo"
      -- rdf("bar") ->- "bar"
    ).graph

  val fooReference = (
    URI("http://example.com/foo")
      -- rdf("foo") ->- "foo"
      -- rdf("bar") ->- "bar"
    ).graph

  val bar = (
    URI("http://example.com/foo")
      -- rdf("bar") ->- "bar"
      -- rdf("baz") ->- "baz"
    ).graph

  val barReference = (
    URI("http://example.com/foo")
      -- rdf("bar") ->- "bar"
      -- rdf("baz") ->- "baz"
    ).graph

  val foobar = (
    URI("http://example.com/foo")
      -- rdf("foo") ->- "foo"
      -- rdf("bar") ->- "bar"
      -- rdf("baz") ->- "baz"
    ).graph

  describe("Graph union ops") {
    it("union must compute the union of two graphs, and should not touch the graphs") {
      val result = union(foo :: bar :: Nil)
      expect(isomorphism(foo, fooReference)).toEqual(true)
      expect(isomorphism(bar, barReference)).toEqual(true)
      expect(isomorphism(foo, bar)).toEqual(false)
      expect(isomorphism(foobar, result)).toEqual(true)
    }

    it("union of Nil must return an empty graph") {
      val result: Rdf#Graph = union(Nil)
      expect(isomorphism(result, emptyGraph)).toEqual(true)
    }

    it("union of a single graph must return an isomorphic graph") {
      val result = union(foo :: Nil)
      expect(isomorphism(result, foo)).toEqual(true)
    }
  }
}

object GraphUnionJasmineTest extends GraphUnionJasmineTest[RDFStore]


class DieselGraphConstructJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf])
  extends JasmineTest {

  import ops._

  val foaf = FOAFPrefix[Rdf]

  describe("Diesel ops") {

    it("Diesel must accept a GraphNode in the object position") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre".lang("fr")
          -- foaf.title ->- "Mr"
        )

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexandre", Lang("fr"))),
          Triple(bnode("betehess"), foaf.title, Literal("Mr")))

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)

    }

    it("Diesel must construct a simple GraphNode") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre".lang("fr")
          -- foaf.knows ->- (
          URI("http://bblfish.net/#hjs")
            -- foaf.name ->- "Henry Story"
            -- foaf.currentProject ->- URI("http://webid.info/")
          )
        )

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexandre", Lang("fr"))),
          Triple(bnode("betehess"), foaf.knows, URI("http://bblfish.net/#hjs")),
          Triple(URI("http://bblfish.net/#hjs"), foaf.name, Literal("Henry Story")),
          Triple(URI("http://bblfish.net/#hjs"), foaf.currentProject, URI("http://webid.info/")))

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must accept triples written in the inverse order o-p-s using <--") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre".lang("fr")
          -<- foaf.knows -- (
          URI("http://bblfish.net/#hjs") -- foaf.name ->- "Henry Story"
          )
        )

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexandre", Lang("fr"))),
          Triple(URI("http://bblfish.net/#hjs"), foaf.knows, bnode("betehess")),
          Triple(URI("http://bblfish.net/#hjs"), foaf.name, Literal("Henry Story"))
        )

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must allow easy use of rdf:type through the method 'a'") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess").a(foaf.Person)
          -- foaf.name ->- "Alexandre".lang("fr")
        )

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), rdf("type"), foaf.Person),
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexandre", Lang("fr"))))

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must allow objectList definition with simple syntax") {

      val g: PointedGraph[Rdf] =
        bnode("betehess") -- foaf.name ->-("Alexandre".lang("fr"), "Alexander".lang("en"))

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexandre", Lang("fr"))),
          Triple(bnode("betehess"), foaf.name, Literal.tagged("Alexander", Lang("en")))
        )

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must allow explicit objectList definition") {
      val alexs = Seq(
        bnode("a") -- foaf.name ->- "Alexandre".lang("fr"),
        bnode("b") -- foaf.name ->- "Alexander".lang("en")
      )

      val g = (
        URI("http://bblfish.net/#hjs")
          -- foaf.name ->- "Henry Story"
          -- foaf.knows ->- ObjectList(alexs)
        )

      val expectedGraph =
        Graph(
          Triple(URI("http://bblfish.net/#hjs"), foaf.name, Literal("Henry Story")),
          Triple(URI("http://bblfish.net/#hjs"), foaf.knows, bnode("a")),
          Triple(URI("http://bblfish.net/#hjs"), foaf.knows, bnode("b")),
          Triple(bnode("a"), foaf.name, Literal.tagged("Alexander", Lang("en"))),
          Triple(bnode("b"), foaf.name, Literal.tagged("Alexandre", Lang("fr")))
        )

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel with empty explicit objectList definition") {
      val g =
        (
          URI("http://bblfish.net/#hjs")
            -- foaf.name ->- "Henry Story"
            -- foaf.knows ->- ObjectList(Seq.empty[Int])
          )

      val expectedGraph =
        Graph(
          Triple(URI("http://bblfish.net/#hjs"), foaf.name, Literal("Henry Story"))
        )

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must understand Scala's native types") {

      val g = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre"
          -- foaf.age ->- 29
          -- foaf.height ->- 1.80
        ).graph

      val expectedGraph =
        Graph(
          Triple(bnode("betehess"), foaf.name, Literal("Alexandre", xsd.string)),
          Triple(bnode("betehess"), foaf.age, Literal("29", xsd.int)),
          Triple(bnode("betehess"), foaf.height, Literal("1.8", xsd.double)))

      expect(g isIsomorphicWith expectedGraph).toEqual(true)
    }

    it("Diesel must support RDF collections") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess")
          -- foaf.name ->- List(1, 2, 3)
        )

      val l: PointedGraph[Rdf] = (
        bnode()
          -- rdf.first ->- 1
          -- rdf.rest ->- (
          bnode()
            -- rdf.first ->- 2
            -- rdf.rest ->- (
            bnode()
              -- rdf.first ->- 3
              -- rdf.rest ->- rdf.nil
            )
          )
        )

      val expectedGraph = (
        bnode("betehess") -- foaf.name ->- l
        )
      expect(g.graph isIsomorphicWith expectedGraph.graph).toEqual(true)
    }

    it("Diesel must support RDF collections (empty list)") {

      val g: PointedGraph[Rdf] = (
        bnode("betehess") -- foaf.name ->- List[String]()
        )

      val expectedGraph = (
        bnode("betehess") -- foaf.name ->- rdf.nil
        )

      expect(g.graph isIsomorphicWith expectedGraph.graph).toEqual(true)
    }

    it("providing a None as an object does not emit a triple") {

      val g = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre"
          -- foaf.age ->- none[Int]
        ).graph

      val expectedGraph = (
        bnode("betehess") -- foaf.name ->- "Alexandre"
        ).graph

      expect(g isIsomorphicWith expectedGraph).toEqual(true)

    }

    it("providing a Some(t) as an object just emits the triple with t as an object") {

      val g = (
        bnode("betehess")
          -- foaf.name ->- "Alexandre"
          -- foaf.age ->- some(42)
        ).graph

      val expectedGraph = (
        bnode("b")
          -- foaf.name ->- "Alexandre"
          -- foaf.age ->- 42
        ).graph

      expect(g isIsomorphicWith expectedGraph).toEqual(true)

    }

    it("disconnected graph construction") {

      val g = (
        bnode("a") -- foaf.name ->- "Alexandre"
          -- foaf.age ->- 29
        ).graph union (
        bnode("h") -- foaf.name ->- "Henry"
          -- foaf.height ->- 1.92
        ).graph

      val expectedGraph =
        Graph(
          Triple(bnode("a"), foaf.name, Literal("Alexandre", xsd.string)),
          Triple(bnode("a"), foaf.age, Literal("29", xsd.int)),
          Triple(bnode("h"), foaf.name, Literal("Henry", xsd.string)),
          Triple(bnode("h"), foaf.height, Literal("1.92", xsd.double))
        )

      expect(g.graph isIsomorphicWith expectedGraph).toEqual(true)

    }

    it("Diesel must support sets") {

      val pg: PointedGraph[Rdf] = (
        bnode("betehess") -- foaf.name ->- Set(1.toPG,
          "blah".toPG,
          bnode("foo") -- foaf.homepage ->- URI("http://example.com"))
        )

      val expectedGraph = Graph(Set(
        Triple(bnode("betehess"), foaf.name, Literal("1", xsd.int)),
        Triple(bnode("betehess"), foaf.name, Literal("blah")),
        Triple(bnode("betehess"), foaf.name, bnode("foo")),
        Triple(bnode("foo"), foaf.homepage, URI("http://example.com"))
      ))

      expect(pg.graph isIsomorphicWith expectedGraph).toEqual(true)
    }
  }

}

object DieselGraphConstructJasmineTest extends DieselGraphConstructJasmineTest[RDFStore]

abstract class DieselGraphExplorationJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf])
  extends JasmineTest {

  import ops._

  val foaf = FOAFPrefix[Rdf]

  val betehess: PointedGraph[Rdf] = (
    URI("http://bertails.org/#betehess").a(foaf.Person)
      -- foaf.name ->- "Alexandre".lang("fr")
      -- foaf.name ->- "Alexander".lang("en")
      -- foaf.age ->- 29
      -- foaf("foo") ->- List(1, 2, 3)
      -- foaf.knows ->- (
      URI("http://bblfish.net/#hjs").a(foaf.Person)
        -- foaf.name ->- "Henry Story"
        -- foaf.currentProject ->- URI("http://webid.info/")
      )
    )

  describe("Traversals") {

    it("'/' method must traverse the graph") {
      val names = betehess / foaf.name
      expect(names.map(_.pointer).toSet == Set(Literal.tagged("Alexandre", Lang("fr")), Literal.tagged("Alexander", Lang("en"))))
    }

    it("'/' method must work with uris and bnodes") {

      val name = betehess / foaf.knows / foaf.name

      expect(name.head.pointer.equals(Literal("Henry Story"))).toEqual(true)

    }

    it("we must be able to project nodes to Scala types") {

      expect((betehess / foaf.age).as[Int] == Success(29)).toEqual(true)

      expect((betehess / foaf.knows / foaf.name).as[String] == Success("Henry Story")).toEqual(true)

    }

    it("betehess should have three predicates: foaf:name foaf:age foaf:knows") {

      val predicates = betehess.predicates.toList
      List(foaf.name, foaf.age, foaf.knows) foreach { p => expect(predicates.contains(p))}

    }

    it("we must be able to get rdf lists") {

      expect((betehess / foaf("foo")).as[List[Int]] == Success(List(1, 2, 3))).toEqual(true)

    }

    it("we must be able to optionally get objects") {

      expect((betehess / foaf.age).asOption[Int] == Success(Some(29))).toEqual(true)

      expect((betehess / foaf.age).asOption[String] == Success(None)).toEqual(true)

      expect((betehess / foaf("unknown")).asOption[Int] == Success(None)).toEqual(true)

    }

    it("asking for one (or exactly one) node when there is none must fail") {

      expect((betehess / foaf("unknown")).takeOnePointedGraph.isFailure).toEqual(true)

      expect((betehess / foaf("unknown")).exactlyOnePointedGraph.isFailure).toEqual(true)

    }

    it("asking for exactly one node when there are more than one must fail") {

      expect((betehess / foaf.name).exactlyOnePointedGraph.isFailure).toEqual(true)

    }

    it("asking for one node when there is at least one must be a success") {

      expect((betehess / foaf.name).takeOnePointedGraph.isSuccess).toEqual(true)

      expect((betehess / foaf.age).takeOnePointedGraph.isSuccess).toEqual(true)

    }

    it("asking for exactly one pointed graph when there is none must fail") {

      expect((betehess / foaf("unknown")).exactlyOnePointedGraph.isFailure).toEqual(true)

    }

    it("asking for exactly one pointed graph when there are more than one must fail") {

      expect((betehess / foaf.name).exactlyOnePointedGraph.isFailure).toEqual(true)

    }

    it("getAllInstancesOf must give all instances of a given class") {

      val persons = betehess.graph.getAllInstancesOf(foaf.Person).nodes

      expect(persons.toSet == Set(URI("http://bertails.org/#betehess"), URI("http://bblfish.net/#hjs"))).toEqual(true)

    }

    it("isA must test if a node belongs to a class") {

      expect(betehess.isA(foaf.Person)).toEqual(true)

      expect(betehess.isA(foaf("SomethingElse"))).toEqual(false)

    }
  }
}

object RDFStoreWDieselGraphExplorationJasmineTest extends DieselGraphExplorationJasmineTest[RDFStore]


abstract class CommonBindersJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf])
  extends JasmineTest {

  import ops._

  describe("common binders") {

    it("serializing and deserialiazing Joda DateTime") {
      val dateTime = DateTime.now()
      expect(dateTime.toPG.as[DateTime].get.compareTo(dateTime)).toEqual(0)
    }

    it("serializing and deserialiazing a Boolean") {
      val truePg = true.toPG
      expect(truePg.pointer == Literal("true", xsd.boolean)).toEqual(true)
      expect(truePg.graph == Graph.empty).toEqual(true)
      expect(true.toPG.as[Boolean] == Success(true)).toEqual(true)

      val falsePg = false.toPG
      expect(truePg.pointer == Literal("true", xsd.boolean)).toEqual(true)
      expect(truePg.graph == Graph.empty).toEqual(true)
      expect(false.toPG.as[Boolean] == Success(false)).toEqual(true)

    }

    it("serializing and deserializing an Integer") {
      val pg123 = 123.toPG
      expect(pg123.pointer == Literal("123", xsd.int)).toEqual(true)
      expect(pg123.graph == Graph.empty).toEqual(true)
      expect(pg123.toPG.as[Int] == Success(123)).toEqual(true)
    }

    it("serializing and deserializing a List of simple nodes") {
      val bn1 = BNode()
      val bn2 = BNode()
      val bn3 = BNode()
      val constructedListGr = Graph(
        Triple(bn1, rdf.first, Literal("1", xsd.int)),
        Triple(bn1, rdf.rest, bn2),
        Triple(bn2, rdf.first, Literal("2", xsd.int)),
        Triple(bn2, rdf.rest, bn3),
        Triple(bn3, rdf.first, Literal("3", xsd.int)),
        Triple(bn3, rdf.rest, rdf.nil)
      )
      val binder = PGBinder[Rdf, List[Int]]
      val list = List(1, 2, 3)
      val listPg = binder.toPG(list)
      expect(listPg.graph isIsomorphicWith (constructedListGr)).toEqual(true)
      expect(binder.fromPG(listPg) == Success(list)).toEqual(true)
    }

    it("serializing and deserializing a List of complex types") {
      val binder = implicitly[PGBinder[Rdf, List[List[Int]]]]
      val list = List(List(1, 2), List(3))
      expect(binder.fromPG(binder.toPG(list)) == Success(list)).toEqual(true)
    }

    it("serializing and deserializing a Tuple2") {
      val binder = PGBinder[Rdf, (Int, String)]
      val tuple = (42, "42")
      expect(binder.fromPG(binder.toPG(tuple)) == Success(tuple)).toEqual(true)
    }

    it("serializing and deserializing a Map") {
      val binder = PGBinder[Rdf, Map[String, List[Int]]]
      val map = Map("1" -> List(1, 2, 3), "2" -> List(4, 5))
      expect(binder.fromPG(binder.toPG(map)) == Success(map)).toEqual(true)
      expect(binder.fromPG(binder.toPG(Map.empty)) == Success(Map.empty)).toEqual(true)
    }

    it("serializing and deserializing an Either") {
      val binder = PGBinder[Rdf, Either[String, List[Int]]]
      val StringPGBinder = PGBinder[Rdf, String]
      val left = Left("foo")
      val right = Right(List(1, 2, 3))
      expect(binder.fromPG(binder.toPG(left)) == Success(left)).toEqual(true)
      expect(binder.fromPG(binder.toPG(right)) == Success(right)).toEqual(true)
      expect(binder.fromPG(StringPGBinder.toPG("foo")).isFailure).toEqual(true)
    }

    it("serializing and deserialiazing Option") {
      val opts: Option[String] = Some("foo")
      implicit val binder = PGBinder[Rdf, Option[String]]
      expect(opts.toPG.as[Option[String]] == Success(opts)).toEqual(true)
      expect((None: Option[String]).toPG.as[Option[String]] == Success(None)).toEqual(true)
    }

    it("the implicit chains must be complete") {
      implicitly[PGBinder[Rdf, Rdf#URI]]
      implicitly[NodeBinder[Rdf, Rdf#URI]]
      implicitly[PGBinder[Rdf, Rdf#Node]]
      implicitly[ToURI[Rdf, Rdf#URI]]
      implicitly[FromURI[Rdf, Rdf#URI]]
    }

  }

}

object CommonBindersJasmineTest extends CommonBindersJasmineTest[RDFStore]


class ObjectExamplesJasmine[Rdf <: RDF]()(implicit ops: RDFOps[Rdf], recordBinder: RecordBinder[Rdf]) {

  import ops._
  import recordBinder._

  val foaf = FOAFPrefix[Rdf]
  val cert = CertPrefix[Rdf]

  case class Person(name: String, nickname: Option[String] = None)

  object Person {

    val clazz = URI("http://example.com/Person#class")
    implicit val classUris = classUrisFor[Person](clazz)

    val name = property[String](foaf.name)
    val nickname = optional[String](foaf("nickname"))
    val address = property[Address](foaf("address"))

    implicit val container = URI("http://example.com/persons/")
    implicit val binder = pgb[Person](name, nickname)(Person.apply, Person.unapply)

  }

  sealed trait Address

  object Address {

    val clazz = URI("http://example.com/Address#class")
    implicit val classUris = classUrisFor[Address](clazz)

    // not sure if this could be made more general, nor if we actually want to do that
    implicit val binder: PGBinder[Rdf, Address] = new PGBinder[Rdf, Address] {
      def fromPG(pointed: PointedGraph[Rdf]): Try[Address] =
        Unknown.binder.fromPG(pointed) orElse VerifiedAddress.binder.fromPG(pointed)

      def toPG(address: Address): PointedGraph[Rdf] = address match {
        case va: VerifiedAddress => VerifiedAddress.binder.toPG(va)
        case Unknown => Unknown.binder.toPG(Unknown)
      }
    }

  }

  // We need to get rid of all cryptographic code as it is not supported in JS
  case object Unknown extends Address {

    val clazz = URI("http://example.com/Unknown#class")
    implicit val classUris = classUrisFor[Unknown.type](clazz, Address.clazz)

    // there is a question about constants and the classes they live in
    implicit val binder: PGBinder[Rdf, Unknown.type] = constant(this, URI("http://example.com/Unknown#thing")) withClasses classUris

  }

  case class VerifiedAddress(label: String, city: City) extends Address

  object VerifiedAddress {

    val clazz = URI("http://example.com/VerifiedAddress#class")
    implicit val classUris = classUrisFor[VerifiedAddress](clazz, Address.clazz)

    val label = property[String](foaf("label"))
    val city = property[City](foaf("city"))

    implicit val ci = classUrisFor[VerifiedAddress](clazz)

    implicit val binder = pgb[VerifiedAddress](label, city)(VerifiedAddress.apply, VerifiedAddress.unapply) withClasses classUris

  }

  case class City(cityName: String, otherNames: Set[String] = Set.empty)

  object City {

    val clazz = URI("http://example.com/City#class")
    implicit val classUris = classUrisFor[City](clazz)

    val cityName = property[String](foaf("cityName"))
    val otherNames = set[String](foaf("otherNames"))

    implicit val binder: PGBinder[Rdf, City] =
      pgbWithId[City](t => URI("http://example.com/" + t.cityName))
        .apply(cityName, otherNames)(City.apply, City.unapply) withClasses classUris

  }

  case class Me(name: String)

  object Me {
    val clazz = URI("http://example.com/Me#class")
    implicit val classUris = classUrisFor[Me](clazz)

    val name = property[String](foaf.name)

    implicit val binder: PGBinder[Rdf, Me] =
      pgbWithConstId[Me]("http://example.com#me")
        .apply(name)(Me.apply, Me.unapply) withClasses classUris
  }

}

abstract class RecordBinderJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf], recordBinder: RecordBinder[Rdf])
  extends JasmineTest {

  import ops._

  println("-- OBJECTS??")
  val objects = new ObjectExamplesJasmine
  println(" == CREATED")

  import objects._

  println(" == IMPORTED")

  val city = City("Paris", Set("Panam", "Lutetia"))
  val verifiedAddress = VerifiedAddress("32 Vassar st", city)
  val person = Person("Alexandre Bertails")
  val personWithNickname = person.copy(nickname = Some("betehess"))
  val me = Me("Name")

  describe("record binders") {


    it("serializing and deserializing a City") {
      expect(city.toPG.as[City] == Success(city)).toEqual(true)

      val expectedGraph = (
        URI("http://example.com/Paris").a(City.clazz)
          -- foaf("cityName") ->- "Paris"
          -- foaf("otherNames") ->- "Panam"
          -- foaf("otherNames") ->- "Lutetia"
        ).graph
      expect(city.toPG.graph.isIsomorphicWith(expectedGraph)).toEqual(true)
    }

    /*
    "serializing and deserializing a public key" in {
      import Cert._
      val rsaPg = rsa.toPG
      //todo: there is a bug below. The isomorphism does not work, even though it should.
      //    System.out.println(s"rsag=${rsaPg.graph}")
      //    val expectedGraph = (
      //      URI("#k") -- cert.modulus ->- rsa.getModulus.toByteArray
      //              -- cert.exponent ->- rsa.getPublicExponent
      //      ).graph
      //    System.out.println(s"expectedGraph=${expectedGraph}")
      //    rsaPg.graph.isIsomorphicWith(expectedGraph) must be(true)
      rsaPg.as[RSAPublicKey] should be(Success(rsa))
    }
    */

    it("graph constant pointer") {
      expect(me.toPG.pointer == URI("http://example.com#me")).toEqual(true)
    }

    it("graph pointer based on record fields") {
      expect(city.toPG.pointer == URI("http://example.com/Paris")).toEqual(true)
    }

    it("serializing and deserializing a VerifiedAddress") {
      expect(verifiedAddress.toPG.as[VerifiedAddress] == Success(verifiedAddress)).toEqual(true)
    }

    it("serializing and deserializing a VerifiedAddress as an Address") {
      expect(verifiedAddress.toPG.as[Address] == Success(verifiedAddress)).toEqual(true)
    }

    it("serializing and deserializing an Unknown address") {
      expect(Unknown.toPointedGraph.as[Unknown.type] == Success(Unknown)).toEqual(true)
    }

    it("serializing and deserializing an Unknown address as an Address") {
      expect(Unknown.toPointedGraph.as[Address] == Success(Unknown)).toEqual(true)
    }

    it("serializing and deserializing a Person") {
      expect(person.toPointedGraph.as[Person] == Success(person)).toEqual(true)
    }

    it("serializing and deserializing a Person with a nickname") {
      expect(personWithNickname.toPointedGraph.as[Person] == Success(personWithNickname)).toEqual(true)
    }

  }

}

object RecordBinderJasmineTest extends RecordBinderJasmineTest[RDFStore]

abstract class UriSyntaxJasmineTest[Rdf <: RDF]()(implicit ops: RDFOps[Rdf]) extends JasmineTest {

  import ops._

  describe("URI syntax") {


    it(".fragmentLess should remove the fragment part of a URI") {
      val uri = URI("http://example.com/foo#bar")
      expect(uri.fragmentLess == URI("http://example.com/foo")).toEqual(true)
    }

    it(".fragment should set the fragment part of a URI") {
      val uri = URI("http://example.com/foo")
      expect(uri.withFragment("bar") == URI("http://example.com/foo#bar")).toEqual(true)
    }

    it(".fragment should return the fragment part of a URI") {
      val uri = URI("http://example.com/foo#bar")
      expect(uri.fragment == Some("bar")).toEqual(true)
      val uriNoFrag = URI("http://example.com/foo")
      expect(uriNoFrag.fragment == None).toEqual(true)
    }

    it("isPureGragment should should say if a URI is a pure fragment") {
      expect(URI("http://example.com/foo").isPureFragment).toEqual(false)
      expect(URI("http://example.com/foo#bar").isPureFragment).toEqual(false)
      expect(URI("#bar").isPureFragment).toEqual(true)
    }

    it("/ should create a sub-resource uri") {
      expect((URI("http://example.com/foo") / "bar") == URI("http://example.com/foo/bar")).toEqual(true)
      expect((URI("http://example.com/foo/") / "bar") == URI("http://example.com/foo/bar")).toEqual(true)
    }

    it("resolve should resolve the uri against the passed string") {
      expect(URI("http://example.com/foo").resolve(URI("bar")) == URI("http://example.com/bar")).toEqual(true)
      expect(URI("http://example.com/foo/").resolve(URI("bar")) == URI("http://example.com/foo/bar")).toEqual(true)
    }

    it("resolveAgainst should work like resolve, just the other way around") {
      // the following test does not make sense as the resolution base Uri must be absolute
      // URI("http://example.com/foo").resolveAgainst(URI("#bar")) should be(URI("http://example.com/foo"))
      expect(URI("bar").resolveAgainst(URI("http://example.com/foo")) == URI("http://example.com/bar")).toEqual(true)
      expect(URI("#bar").resolveAgainst(URI("http://example.com/foo")) == URI("http://example.com/foo#bar")).toEqual(true)
      expect(URI("#bar").resolveAgainst(URI("http://example.com/foo/")) == URI("http://example.com/foo/#bar")).toEqual(true)
      expect(URI("bar").resolveAgainst(URI("http://example.com/foo")) == URI("http://example.com/bar")).toEqual(true)
      expect((URI("bar"): Rdf#Node).resolveAgainst(URI("http://example.com/foo")) == URI("http://example.com/bar")).toEqual(true)
    }

    it(".relativize() should relativize the uri against the passed string") {
      expect(URI("http://example.com/foo").relativize(URI("http://example.com/foo#bar")) == URI("#bar")).toEqual(true)
      expect((URI("http://example.com/foo"): Rdf#Node).relativize(URI("http://example.com/foo#bar")) == URI("#bar")).toEqual(true)
      expect(URI("http://example.com/foo#bar").relativizeAgainst(URI("http://example.com/foo")) == URI("#bar")).toEqual(true)
    }

    it("should be able to create and work with relative URIs") {
      val me = URI("/people/card/henry#me")
      expect(me.fragment == Some("me")).toEqual(true)
      expect(me.fragmentLess == URI("/people/card/henry")).toEqual(true)
      val host = URI("http://bblfish.net")
      expect(me.resolveAgainst(host) == URI("http://bblfish.net/people/card/henry#me")).toEqual(true)
    }

    /*
    it("transforming java URIs and URLs to Rdf#URI") {
      import syntax.URIW
      val card = "http://bblfish.net/people/henry/card"
      val uri: Rdf#URI = URI(card)

      new URL(card).toUri should be(uri)
      new java.net.URI(card).toUri should be(uri)
    }
    */
  }
}


object UriSyntaxJasmineTest extends UriSyntaxJasmineTest[RDFStore]
