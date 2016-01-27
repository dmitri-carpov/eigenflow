package com.mediative.eigenflow.test.it.helper

import java.util.Date

import akka.actor.Props
import com.mediative.eigenflow.StagedProcess
import com.mediative.eigenflow.domain.ProcessContext
import com.mediative.eigenflow.domain.fsm.{ProcessEvent, ProcessStage}
import com.mediative.eigenflow.process.ProcessFSM
import com.mediative.eigenflow.process.ProcessFSM.Continue
import com.mediative.eigenflow.publisher.MessagingSystem

object TracedProcessFSM {

  implicit def messagingSystem = new MessagingSystem {
    override def publish(topic: String, message: String): Unit = () // ignore
  }

  def props(process: StagedProcess, date: Date) = Props(new TracedProcessFSM(process, date))

  case object Start

  class TracedProcessFSM(process: StagedProcess, date: Date) extends ProcessFSM(process, date) {
    private var originalSender = sender
    private var stagesRegistry: Seq[ProcessStage] = Seq.empty

    def testReceive: PartialFunction[Any, Unit] = {
      case Start =>
        originalSender = sender()
        self ! Continue
    }

    override def applyEvent(domainEvent: ProcessEvent, processContext: ProcessContext): ProcessContext = {
      stagesRegistry = stagesRegistry.:+(stateName) // register the last state just before switching it
      super.applyEvent(domainEvent, processContext)
    }

    override def postStop(): Unit = {
      stagesRegistry = stagesRegistry.:+(stateName) // add the last state
      originalSender ! stagesRegistry
      super.postStop()
    }

    override def receive = testReceive orElse super.receive
  }

}
