/*
 * Copyright 2018 Loopring Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.loopring.lightcone.lib

import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.proto.block_chain_event.{ FullTransaction, TxHeader }
import org.loopring.lightcone.proto.block_chain_event.TxHeaderSource._
import org.loopring.lightcone.proto.block_chain_event.TxStatus._
import org.loopring.lightcone.proto.eth_jsonrpc._

package object abi {

  def safeEquals(s1: String, s2: String): Boolean = {
    s1.toLowerCase.equals(s2.toLowerCase)
  }

  case class RingsSubmitParam(
      data: Bitstream,
      tables: Bitstream
  )

  case class OrderSpendableIdx(
      orderHash: String,
      tokenSpendableFeeIdx: Int,
      tokenSpendableSIdx: Int
  )

  implicit class RichBlockNumber(src: BigInt) {

    def afterByzantiumFork(): Boolean = {
      val byzantiumBlock = BigInt(4370000)
      src.compareTo(byzantiumBlock) >= 0
    }

  }

  implicit class RichTransaction(src: Transaction) {

    def isPending: Boolean = {
      src.blockNumber.isEmpty
    }
  }

  implicit class RichReceipt(src: TransactionReceipt) {

    // 拜占庭分叉前(私链测试)/获取receipt时判断
    def statusInvalid(): Boolean = {
      if (src.blockNumber.asBigInt.afterByzantiumFork().equals(false)) {
        false
      } else {
        src.status.isEmpty
      }
    }

    def statusSuccess(): Boolean = src.blockNumber.asBigInt.afterByzantiumFork() match {
      case true if src.status.asBigInt.compare(BigInt(1)) == 0 ⇒ true
      case false if src.logs.nonEmpty ⇒ true
      case false if src.root.nonEmpty ⇒ true
      case _ ⇒ false
    }
  }

  implicit class RichFullTransaction(src: FullTransaction) {

    def getTxHeader: TxHeader = {
      TxHeader().copyFrom(src.getTx).fillWith(src.getReceipt)
    }
  }

  implicit class RichTxHeader(tx: TxHeader) {

    def copyFrom(src: Transaction): TxHeader = {
      var header = tx.copy(
        txHash = src.hash,
        from = src.from,
        to = src.to,
        value = src.value.asBigInteger.toString,
        gasPrice = src.gasPrice.asBigInteger.toString,
        gasLimit = src.gas.asBigInteger.toString,
        nonce = src.nonce.asBigInteger.toString,
        source = TX_FROM_TX
      )

      if (src.isPending) {
        header = header.copy(status = TX_STATUS_PENDING)
      } else {
        header = header.copy(txIndex = src.transactionIndex.asBigInteger.intValue())
      }

      header
    }

    def fillWith(src: TraceTransaction): TxHeader = tx.copy(
      traceFrom = src.from,
      traceTo = src.to,
      traceGas = src.gas.asBigInteger.toString,
      traceGasUsed = src.gasUsed.asBigInteger.toString,
      traceValue = src.value.asBigInteger.toString,
      source = TX_FROM_TRACE_TX
    )

    def fillWith(src: TraceCall): TxHeader = tx.copy(
      traceFrom = src.from,
      traceTo = src.to,
      traceGas = src.gas.asBigInteger.toString,
      traceGasUsed = src.gasUsed.asBigInteger.toString,
      traceValue = src.value.asBigInteger.toString,
      source = TX_FROM_TRACE_CALL
    )

    def fillWith(src: TransactionReceipt): TxHeader = {
      val status = if (src.statusSuccess().equals(true)) {
        TX_STATUS_SUCCESS
      } else {
        TX_STATUS_FAILED
      }

      tx.copy(
        status = status,
        blockHash = src.blockHash,
        blockNumber = src.blockNumber.asBigInteger.toString,
        txIndex = src.transactionIndex.asBigInteger.intValue(),
        gasUsed = src.gasUsed.asBigInteger.toString
      )
    }

    def fillWith(src: Log): TxHeader = tx.copy(
      logIndex = src.logIndex.asBigInteger.intValue(),
      source = TX_FROM_RECEIPT_LOG
    )

    def safeFrom: String = tx.source match {
      case TX_FROM_TRACE_CALL ⇒ tx.traceFrom
      case TX_FROM_TRACE_TX   ⇒ tx.traceFrom
      case _                  ⇒ tx.from
    }

    def safeTo: String = tx.source match {
      case TX_FROM_TRACE_CALL ⇒ tx.traceTo
      case TX_FROM_TRACE_TX   ⇒ tx.traceTo
      case _                  ⇒ tx.to
    }

    def safeGas: String = tx.source match {
      case TX_FROM_TRACE_CALL ⇒ tx.traceGas
      case TX_FROM_TRACE_TX   ⇒ tx.traceGas
      case _                  ⇒ tx.to
    }

    def safeGasUsed: String = tx.source match {
      case TX_FROM_TRACE_CALL ⇒ tx.traceGasUsed
      case TX_FROM_TRACE_TX   ⇒ tx.traceGasUsed
      case _                  ⇒ tx.gasUsed
    }

  }

}
