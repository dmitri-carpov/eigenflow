package com.mediative.eigenflow

import java.io.IOException
import java.util.{Date, UUID}

import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.RecoveryStrategy.{Retry, Complete}
import com.mediative.eigenflow.domain.fsm.{ExecutionPlan, Initial, ProcessStage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.concurrent.duration._

package object test {

  case object Stage1 extends ProcessStage

  case object Stage2 extends ProcessStage

  case object Stage3 extends ProcessStage

  def defaultProcessContext: ProcessContext = {
    ProcessContext(timestamp = System.currentTimeMillis(),
      processingDate = new Date(),
      processId = UUID.randomUUID().toString,
      startTime = System.currentTimeMillis(),
      stage = Initial,
      message = "")
  }

  def randomString: String = UUID.randomUUID().toString

  def stringFunction: Future[String] = {
    Future {
      randomString
    }
  }

  def toFuture[A](a: A): Future[A] = {
    Future {
      a
    }
  }

  val `Stage1 ~> Stage2` = new StagedProcess {
    val a = Stage1 {
      stringFunction
    }
    val b = Stage2 { _: String =>
      stringFunction
    }

    override def executionPlan: ExecutionPlan[_, _] = a ~> b

    override def nextProcessingDate(lastCompleted: Date): Date = new Date()
  }

  val `Stage1(retry) ~> Stage2` = new StagedProcess {
    val a = Stage1 withContext { ctx: ProcessContext =>
      if (ctx.failure.isEmpty) {
        throw new RuntimeException
      }
      stringFunction
    } retry(10.milliseconds, 1)

    val b = Stage2 { _: String =>
      stringFunction
    }

    override def executionPlan: ExecutionPlan[_, _] = a ~> b

    override def nextProcessingDate(lastCompleted: Date): Date = new Date()
  }

  val `Stage1(Complete) ~> Stage2` = new StagedProcess {
    val a = Stage1 withContext { ctx: ProcessContext =>
      if (ctx.failure.isEmpty) {
        throw new RuntimeException
      }
      stringFunction
    } onFailure {
      case _: Throwable => Complete
    }

    val b = Stage2 { _: String =>
      stringFunction
    }

    override def executionPlan: ExecutionPlan[_, _] = a ~> b

    override def nextProcessingDate(lastCompleted: Date): Date = new Date()
  }
}
