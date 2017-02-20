package slowotok

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import play.api.libs.ws._
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

import spray.json._
import DefaultJsonProtocol._

import scala.io.Source

import java.security.MessageDigest

import com.typesafe.config.{Config,ConfigFactory}


object Slowotok extends App {
    def md5(ab: Array[Byte]) = {
      // taken from http://stackoverflow.com/a/5992852/118587 and adapted
      MessageDigest.getInstance("MD5").digest(ab).map { "%02x".format(_) }.foldLeft(""){_ + _} 
    }


  def loadWordsFromDictionary(path:String) :List[String] = {
    val properWord = "[a-ząćęłńóśźż]{3,16}".r
    val properWords = Source.fromFile(path).getLines.flatMap( { line =>
       {
         val firstWord = line.split(",")(0)
         val ret : Option[String] = if ((properWord unapplySeq firstWord).isDefined) Some(firstWord.toUpperCase) else None
         ret
       }
    }).toList.distinct   
    println(s"Loaded ${properWords.length} words")
    properWords
  } 
  
  def calculateMd5ForWords(words:List[String]):Map[String,String] = {
    val listOfPairs = words.map({word => md5(word.getBytes) -> word})
    listOfPairs.toMap
  }


  println("########################################################### Start")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val client = AhcWSClient()
  try {
      println("=================== Retrieving parameters/credentials")
      val conf = ConfigFactory.load()
      val email = conf.getString("email")
      val password = conf.getString("password")
      val dictionaryPath = conf.getString("dictionary.path")
      
      println("=================== Logging in...")
      val response = client.url("http://slowotok.pl/account/logon")
        .withFollowRedirects(false)
    	.post(Map("Email" -> Seq(email), // "vi.mot.varlden2@mailinator.com"), 
    		  "Password" -> Seq(password), // "vi.mot.varlden2@mailinator.com"),
  		  "RememberMe" -> Seq("false")
    		  ))
      val result = Await.result(response, Duration.Inf)
      val sessionCookies = result.cookies

      println("=================== Fetching the board")
      val resp2 = client.url("http://slowotok.pl/play/board")
                .withHeaders("Cookie" -> sessionCookies.mkString)
                .get()
      println("=================== Analyzing the board")
      val result2 = Await.result(resp2, Duration.Inf)
      val jsonFromServer = JsonParser(result2.body).asJsObject
      val hashes = jsonFromServer.getFields("Hashs")(0).convertTo[List[String]]
      println(s"The board has ${hashes.length} words to be found")


      println("=================== Loading the dictionary")
      val dictionary = calculateMd5ForWords(loadWordsFromDictionary(dictionaryPath))

      println(s"Calculated MD5 hashes for ${dictionary.size} words")

      println("=================== Matching...")
      hashes.foreach {
        hash =>
         if (dictionary.contains(hash)) {
           println(dictionary(hash))
         } else {
           println(hash)
         }  
      }
      
      
  } finally {      
      println("=================== before close")
      client.close()
      system.shutdown
      println("=================== after close")
  }      
}

