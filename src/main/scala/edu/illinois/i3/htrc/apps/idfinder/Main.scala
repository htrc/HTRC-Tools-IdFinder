package edu.illinois.i3.htrc.apps.idfinder

import java.io.File
import java.net.{URL, URLEncoder}

import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import org.json4s._
import org.rogach.scallop.{ScallopConf, singleArgConverter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App with LazyLogging {
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val (appTitle, appVersion, appVendor) = {
      val p = Option(getClass.getPackage)
      val nameOpt = p.map(_.getImplementationTitle)
      val versionOpt = p.map(_.getImplementationVersion)
      val vendorOpt = p.map(_.getImplementationVendor)
      (nameOpt, versionOpt, vendorOpt)
    }

    version(appTitle.flatMap(
      name => appVersion.flatMap(
        version => appVendor.map(
          vendor => s"$name $version\n$vendor"))).getOrElse("idfinder"))

    implicit val urlConverter = singleArgConverter[URL](new URL(_))

    val inputFile = opt[File]("input",
      descr = "The CSV file containing the title/author of the volumes to look up",
      required = true
    )

    val outputFile = opt[String]("output",
      descr = "The output CSV file containing the extra ID column",
      required = true
    )

    val authorIdx = opt[Int]("author",
      descr = "The (zero-based) index of the column containing the authors",
      default = Some(0)
    )

    val titleIdx = opt[Int]("title",
      descr = "The (zero-based) index of the column containing the titles",
      default = Some(1)
    )

    val maxResults = opt[Int]("max-results",
      descr = "The max number of results to retrieve per query",
      default = Some(1000000)
    )

    val solrUrl = opt[URL]("solr",
      descr = "The SOLR endpoint to use",
      default = Some(new URL("http://chinkapin.pti.indiana.edu:9994/solr/meta"))
    )
  }

  // Parse the command line args and extract values
  val conf = new Conf(args)
  val inputFile = conf.inputFile()
  val outputFile = conf.outputFile()
  val authorIdx = conf.authorIdx()
  val titleIdx = conf.titleIdx()
  val maxResults = conf.maxResults()
  val solrUrl = conf.solrUrl()

  val SOLR_QUERY_TPL = s"$solrUrl/select?q=%s&fl=id&wt=json&omitHeader=true&rows=$maxResults"

  val csvIn = CSVReader.open(inputFile, "utf-8")
  val data = csvIn.iterator
    .map(fields => fields.map(_.trim).map(v => if (v.isEmpty) None else Some(v)))
    .withFilter(fields => fields.exists(_.isDefined))

  val csvOut = CSVWriter.open(outputFile, "utf-8")

  val outputData = data.map(fields => {
    val titleEnc = if (titleIdx < fields.size) fields(titleIdx).map(t => URLEncoder.encode(s""""$t"""", "utf-8")) else None
    val authorEnc = if (authorIdx < fields.size) fields(authorIdx).map(a => URLEncoder.encode(a, "utf-8")) else None
    val q = (titleEnc, authorEnc) match {
      case (Some(t), Some(a)) => s"title:$t+AND+author:$a"
      case (Some(t), None) => s"title:$t"
      case (None, Some(a)) => s"author:$a"
      case _ => throw new Exception("One of 'title' or 'author' must be present")
    }

    val query = SOLR_QUERY_TPL.format(q)
    val svc = url(query)
    val resp = Http(svc OK as.json4s.Json)

    logger.debug("Query: " + query)

    // Variant 1
    resp.map {
      case respJson =>
        val docIds: List[String] = for {
          JObject(docs) <- respJson \\ "docs"
          JField("id", JString(id)) <- docs
        } yield id

        fields :+ Some(docIds.mkString(";"))
    }

    // Variant 2
    // Through the use of 'either', the commented code block below keeps all results inside successful Futures,
    // where the result is of type 'Either', with Right(success) and Left(failure). This way one can know all errors
    // that were encountered during the processing (in subsequent use of Future.sequence(...))
    //    resp.map {
    //      case respJson =>
    //        val docIds: List[String] = for {
    //          JObject(docs) <- respJson \\ "docs"
    //          JField("id", JString(id)) <- docs
    //        } yield id
    //
    //        fields :+ Some(docIds.mkString(";"))
    //    }.either
  })

  Future.sequence(outputData) andThen {
    case Success(docs) =>
      for (d <- docs)
        // Variant 1
        csvOut.writeRow(d.map(_.getOrElse("")))

      // Variant 2
      // Commented code below to be used with commented code above.
      //        d match {
      //          case Right(doc) => csvOut.writeRow(doc.map(_.getOrElse("")))
      //          case Left(e) => logger.error(e.getMessage, e)
      //        }

      csvOut.close()
      println("All done.")

    case Failure(e) => logger.error(e.getMessage, e)
  } onComplete {
    case _ => Http.shutdown()
  }
}
