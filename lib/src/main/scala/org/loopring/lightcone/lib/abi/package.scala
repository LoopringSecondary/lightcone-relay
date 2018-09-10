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
import org.loopring.lightcone.proto.block_chain_event.TxHeader
import org.loopring.lightcone.proto.block_chain_event.TxStatus._
import org.loopring.lightcone.proto.eth_jsonrpc.{ Log, TraceCall, Transaction, TransactionReceipt }

package object abi {

  implicit class RichBlockNumber(src: BigInt) {

    def afterByzantiumFork(): Boolean = {
      val byzantiumBlock = BigInt(4370000)
      src.compareTo(byzantiumBlock) >= 0
    }

  }

  implicit class RichTransaction(src: Transaction) {

    def isPending(): Boolean = {
      src.blockNumber.isEmpty
    }

    def getTxHeader() = {
      var header = TxHeader(
        txHash = src.hash,
        gasPrice = src.gasPrice,
        gasLimit = src.gas,
        nonce = src.nonce,
        from = src.from,
        to = src.to,
        value = src.value)

      if (src.isPending()) {
        header = header.copy(status = TX_STATUS_PENDING)
      }

      header
    }
  }

  implicit class RichTraceCall(src: TraceCall) {

    def fillTxHeader(header: TxHeader): TxHeader = header.copy(
      from = src.from,
      to = src.to,
      isInternal = true)
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
      case true if src.status.asBigInt.compare(BigInt(1)) == 0 => true
      case false if src.logs.length > 0 => true
      case false if src.root.size > 0 => true
      case _ => false
    }

    def fillTxHeader(header: TxHeader): TxHeader = {
      val status = src.statusSuccess() match {
        case true => TX_STATUS_SUCCESS
        case false => TX_STATUS_FAILED
      }

      header.copy(
        status = status,
        blockHash = src.blockHash,
        blockNumber = src.blockNumber,
        txIndex = src.transactionIndex.asBigInteger.intValue(),
        gasUsed = src.gasUsed)
    }
  }

  implicit class RichLog(src: Log) {

    def fillTxHeader(header: TxHeader): TxHeader = {
      header.copy(isLog = true, logIndex = src.logIndex.asBigInteger.intValue())
    }

  }
}
