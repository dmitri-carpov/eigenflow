package com.mediative.eigenflow.domain

import scala.concurrent.duration.FiniteDuration

trait RecoveryStrategy

object RecoveryStrategy {
  case class Retry(interval: FiniteDuration, attempts: Int) extends RecoveryStrategy

  case object Fail extends RecoveryStrategy

  case object Complete extends RecoveryStrategy

  def default: PartialFunction[Throwable, RecoveryStrategy] = {
    case _ => Fail
  }
}