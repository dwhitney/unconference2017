package example

import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._
import org.atnos.eff.future._
import org.atnos.eff.interpret._

import scala.concurrent.{Await, duration, Future}, duration._
import scala.concurrent.ExecutionContext.Implicits.global



final case class Username(value: String)
final case class Email(value: String)
final case class User(username: Username, email: Email)

final case class Balance(value: Double)
final case class BankAccount(id: Username, balance: Balance)

final case class RewardPointsAccount(id: Username, balance: Balance)


sealed trait UserDSL[A]
object UserDSL{

  case class Get(username: Username) extends UserDSL[Option[User]]
  case class Save(user: User) extends UserDSL[Unit]

}



object UserService{

  type _user[STACK] = UserDSL |= STACK

  def get[USER_STACK : _user](username: Username): Eff[USER_STACK, Option[User]] = 
    send(UserDSL.Get(username))

  def save[USER_STACK : _user](user: User): Eff[USER_STACK, Unit] = 
    send(UserDSL.Save(user))

}

object UserServiceFutureInterpreter{

  var map =  Map[Username, User]()

  def get(username: Username): Future[Option[User]] =
    Future(map.get(username))

  def save(user: User): Future[Unit] = 
    Future({ map = map + (user.username -> user) })


  def runUser[STACK_WITH_USER_AND_FUTURE, STACK_WITH_FUTURE, A](
    effects         : Eff[STACK_WITH_USER_AND_FUTURE, A])(implicit 
    user            : Member.Aux[UserDSL, STACK_WITH_USER_AND_FUTURE, STACK_WITH_FUTURE]
  , futureProof     : _future[STACK_WITH_FUTURE]
  ): Eff[STACK_WITH_FUTURE, A] = {

    translate(effects)(new Translate[UserDSL, STACK_WITH_FUTURE]{
    
      def apply[X](dsl: UserDSL[X]): Eff[STACK_WITH_FUTURE, X] = dsl match {

        case UserDSL.Get(username) => future.fromFuture(get(username))
        case UserDSL.Save(user) => future.fromFuture(save(user))
  
      }

    })
  }

}


object Main extends App{

  implicit val scheduler = ExecutorServices.schedulerFromGlobalExecutionContext

  import UserService._

  val user = User(Username("dwhitney"), Email("dustin.whitney@gmail.com"))    

  def programWithNastyGet[USER_STACK : _user]: Eff[USER_STACK, Option[User]] = for{
   _       <- save(user)
   u       <- get(Username("dwhitney"))
   _       <- save(u.get.copy(username = Username("dustinwhitney")))
   updated <- get(Username("dustinwhitney"))
  } yield updated


  def program2[USER_STACK : _user]: Eff[USER_STACK, Option[User]] = {

    type USER_WITH_OPTION_STACK = Fx.prepend[Option, USER_STACK]
    
    val progWithOpt = for{
      _       <- save(user).into[USER_WITH_OPTION_STACK]
      u       <- get[USER_WITH_OPTION_STACK](Username("dwhitney")).collapse
      _       <- save(u.copy(username = Username("dustinwhitney"))).into[USER_WITH_OPTION_STACK]
      updated <- get[USER_WITH_OPTION_STACK](Username("dustinwhitney")).collapse
    } yield updated

    progWithOpt.runOption
  }

  type MyStack = Fx2[UserDSL, TimedFuture]

  import org.atnos.eff.syntax.future._

  val updatedUser = 
    Await.result(
      UserServiceFutureInterpreter.runUser(program2[MyStack]).runSequential
    , 1.second)

  println(updatedUser)

}








