import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.Breaks
import scala.util.{Failure, Success}

object Main extends App {

  val db = Database.forConfig("mysqlDB")
  val peopleTable = TableQuery[People]
  val dropPeopleCmd = DBIO.seq(peopleTable.schema.drop)
  val initPeopleCmd = DBIO.seq(peopleTable.schema.create)
  var connected = true

  def dropDB = {
    val dropFuture = Future {
      db.run(dropPeopleCmd)
    }
    Await.result(dropFuture, Duration.Inf).andThen {
      case Success(_) => initialisePeople
      case Failure(error) =>
        println("Dropping the table failed due to: " + error.getMessage)
        initialisePeople
    }
  }

  def initialisePeople = {
    val setupFuture = Future {
      db.run(initPeopleCmd)
    }
    Await.result(setupFuture, Duration.Inf).andThen {
      case Success(_) => true
      case Failure(error) =>
        println("Initialising the table failed due to: " + error.getMessage)
    }
  }

  def runQuery(): Unit = {
    while (connected) {
      Thread.sleep(1000)
      val userChoice = scala.io.StdIn.readLine("Please enter an option from create, read, update, delete or exit: ").toLowerCase()

      userChoice match {
        case "create" => createPeople()
        case "read" => listPeople
        case "update" => updatePeople()
        case "delete" => deletePeople()
        case "exit" =>
          println("Goodbye!")
          connected = false
        case _ => println("Please input a valid selection")
      }
    }
    //    }
  }

  def createPeople(): Unit = {
    val insertPeople = Future {
      val query = peopleTable ++= Seq(
        (10, "Jack", "Wood", 36),
        (20, "Tim", "Brown", 24)
      )
      println(query.statements.head)
      db.run(query)
    }
    Await.result(insertPeople, Duration.Inf).andThen {
      case Success(_) => listPeople
      case Failure(error) => println("Welp! Something went wrong! " + error.getMessage)
    }
  }


  def listPeople = {
    val queryFuture = Future {
      db.run(peopleTable.result).map(_.foreach {
        case (id, fName, lName, age) => println(s" $id $fName $lName $age")
      })
    }
    Await.result(queryFuture, Duration.Inf).andThen {
      case Success(_) => println("hello")
      case Failure(error) =>
        println("Listing people failed due to: " + error.getMessage)
    }
  }

  def updatePeople() = {
    val query = Future {
      db.run((for {people <- peopleTable if people.lName === "Wood"} yield people.lName).update("Wozniak"))
    }
    Await.result(query, Duration.Inf).andThen {
      case Success(_) => listPeople
      case Failure(error) => println("The update failed because of: " + error.getMessage)
    }
  }

  def deletePeople() = {
    val query = Future {
      db.run {
        peopleTable.filter(_.id === 2).delete
      }
    }

    Await.result(query, Duration.Inf).andThen {
      case Success(_) => listPeople
      case Failure(error) => println("The delete failed because of: " + error.getMessage)
    }
  }

  dropDB
  runQuery()

}
