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

package org.loopring.lightcone.core.conveter

import java.math.BigInteger

import com.google.protobuf.ByteString
import org.loopring.lightcone.proto.block_chain_event.{ OrderFilled, RingMined }
import org.loopring.lightcone.core.etypes._

class RingMinedConverter() extends ContractConverter[RingMined] with TypeConverter {

  /**
   * demo
   *
   * fills {
   * ring_index: "C"
   * ring_hash: "0fa546abb99e5aedea4429283ead47b914bfbb663123c5a63a75b6a62f9fb0fe"
   * pre_order_hash: "0eea27f4fa8f80e1123ee3a635a0dd63276cc69e7a88dc07519dd6315a15f60c"
   * order_hash: "422940f024d6fc87df8e640b28641a73ace9ffab48c0c935914f5b9060ab6dc3"
   * next_order_hash: "0eea27f4fa8f80e1123ee3a635a0dd63276cc69e7a88dc07519dd6315a15f60c"
   * owner: "000000000000000000000000b1018949b241d76a1ab2094f473e9befeabb5ead"
   * token_s: "000000000000000000000000cd36128815ebe0b44d0374649bad2721b8751bef"
   * token_b: "000000000000000000000000f079e0612e869197c5f4c7d0a95df570b163232b"
   * amount_s: "000000000000000000000000000000000000000000000016ffd8c2a5686bb061"
   * amount_b: "000000000000000000000000000000000000000000000000016345785d8a0000"
   * lrc_reward: "0000000000000000000000000000000000000000000000000000000000000000"
   * lrc_fee: "0000000000000000000000000000000000000000000000001d70719cfcae9e52"
   * split_s: "0000000000000000000000000000000000000000000000000000000000000000"
   * split_b: "\000"
   * fill_index: 1
   * }
   * fills {
   * ring_index: "C"
   * ring_hash: "0fa546abb99e5aedea4429283ead47b914bfbb663123c5a63a75b6a62f9fb0fe"
   * pre_order_hash: "422940f024d6fc87df8e640b28641a73ace9ffab48c0c935914f5b9060ab6dc3"
   * order_hash: "0eea27f4fa8f80e1123ee3a635a0dd63276cc69e7a88dc07519dd6315a15f60c"
   * next_order_hash: "422940f024d6fc87df8e640b28641a73ace9ffab48c0c935914f5b9060ab6dc3"
   * owner: "0000000000000000000000001b978a1d302335a6f2ebe4b8823b5e17c3c84135"
   * token_s: "000000000000000000000000f079e0612e869197c5f4c7d0a95df570b163232b"
   * token_b: "000000000000000000000000cd36128815ebe0b44d0374649bad2721b8751bef"
   * amount_s: "000000000000000000000000000000000000000000000000016345785d8a0000"
   * amount_b: "000000000000000000000000000000000000000000000016ffd8c2a5686bb061"
   * lrc_reward: "0000000000000000000000000000000000000000000000000000000000000000"
   * lrc_fee: "0000000000000000000000000000000000000000000000000000000000000000"
   * split_s: "ffffffffffffffffffffffffffffffffffffffffffffffffba9c6e7dbb0c0000"
   * split_b: "\000"
   * fill_index: 0
   * }
   *
   */

  // todo: safeBig处理负数还是有点问题
  def convert(list: Seq[Any]): Seq[RingMined] = {

    if (list.length != 5) {
      throw new Exception("length of decoded ringmined invalid")
    }

    val ringindex = scalaAny2Bigint(list(0)).asProtoByteString()
    val ringhash = scalaAny2Hex(list(1)).asProtoByteString()
    val miner = scalaAny2Hex(list(2)).asProtoByteString()
    val feeReceipt = scalaAny2Hex(list(3)).asProtoByteString()
    val orderinfoList = list(4) match {
      case arr: Array[Object] => arr.map(javaObj2Hex(_).asProtoByteString())
      case _ => throw new Exception("ringmined orderinfo list type error")
    }

    require(orderinfoList.length.equals(14))

    var fills: Seq[OrderFilled] = Seq()

    val length = orderinfoList.length / 7
    for (i <- 0 to (length - 1)) {
      val start = i * 7

      val orderhashIdx = start
      val preOrderhashIdx = if (i.equals(0)) (length - 1) * 7 else (i - 1) * 7
      val nxtOrderhashIdx = if (i.equals(length - 1)) 0 else (i + 1) * 7

      val lrcFeeOrReward = orderinfoList(start + 5)
      val lrcFee = if (safeBig(lrcFeeOrReward.toByteArray).bigInteger.compareTo(BigInt(0)) > 0) {
        lrcFeeOrReward
      } else {
        BigInt(0).asProtoByteString()
      }

      val split = orderinfoList(start + 6)
      val (splitS, splitB) = if (safeBig(split.toByteArray).bigInteger.compareTo(BigInt(0)) > 0) {
        (split, BigInt(0).asProtoByteString())
      } else {
        (BigInt(0).asProtoByteString(), split)
      }

      fills +:= OrderFilled()
        .withRingHash(ringhash)
        .withRingIndex(ringindex)
        .withFillIndex(i)
        .withOrderHash(orderinfoList(orderhashIdx))
        .withOwner(orderinfoList(start + 1))
        .withPreOrderHash(orderinfoList(preOrderhashIdx))
        .withNextOrderHash(orderinfoList(nxtOrderhashIdx))
        .withTokenS(orderinfoList(start + 2))
        .withTokenB(orderinfoList(nxtOrderhashIdx + 2))
        .withAmountS(orderinfoList(start + 3))
        .withAmountB(orderinfoList(nxtOrderhashIdx + 3))
        .withLrcReward(orderinfoList(start + 4))
        .withLrcFee(lrcFee)
        .withSplitS(splitS)
        .withSplitB(splitB)
    }

    val ring = RingMined().withFills(fills)
    println(ring.toProtoString)

    Seq(ring)
  }

  def safeBig2ByteString(str: String): ByteString = {
    ByteString.copyFrom(safeBig(str.getBytes()).toByteArray)
  }

  // safeAbs 当合约返回负数时做异或取反操作
  def safeBig(bytes: Array[Byte]): BigInt = {
    if (bytes(0) > 128) {
      new BigInteger(bytes).xor(maxUint256).not()
    } else {
      BigInt(bytes)
    }
  }

  val maxUint256: BigInteger = {
    val bytes = (1 to 32).map(_ => Byte.MaxValue).toArray
    new BigInteger(bytes)
  }

}
