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

class RingMinedConverter() extends ContractConverter[RingMined] with TypeConverter {

  def convert(list: Seq[Any]): RingMined = {

    if (list.length != 5) {
      throw new Exception("length of decoded ringmined invalid")
    }

    val ringindex = ByteString.copyFrom(scalaAny2Bigint(list(0)).toByteArray)
    val ringhash = ByteString.copyFrom(scalaAny2Hex(list(1)).getBytes())
    val miner = ByteString.copyFrom(scalaAny2Hex(list(2)).getBytes())
    val feeReceipt = ByteString.copyFrom(scalaAny2Hex(list(3)).getBytes())

    val orderinfoList = list(4) match {
      case arr: Array[Object] => arr.map(javaObj2Hex(_))
      case _ => throw new Exception("ringmined orderinfo list type error")
    }

    require(orderinfoList.length.equals(14))

    val firstFillOrderhash = ByteString.copyFrom(orderinfoList(0).getBytes())
    val firstFillOwner = ByteString.copyFrom(orderinfoList(1).getBytes())
    val firstFillTokenS = ByteString.copyFrom(orderinfoList(2).getBytes())
    val firstFillAmountS = orderinfoList(3)
    val firstFillLrcReward = orderinfoList(4)
    val firstFillLrcFeeOrReward = orderinfoList(5)
    val firstFillSplitS = orderinfoList(6)

    val lastFillOrderhash = ByteString.copyFrom(orderinfoList(7).getBytes())
    val lastFillOwner = ByteString.copyFrom(orderinfoList(8).getBytes())
    val lastFillTokenS = ByteString.copyFrom(orderinfoList(9).getBytes())
    val lastFillAmountS = orderinfoList(10)
    val lastFillLrcReward = orderinfoList(11)
    val lastFillLrcFeeOrReward = orderinfoList(12)
    val lastFillSplitS = orderinfoList(13)

    val fill1 = OrderFilled()
      .withRingHash(ringhash)
      .withRingIndex(ringindex)
      .withFillIndex(0)
      .withOwner(firstFillOwner)
      .withOrderHash(firstFillOrderhash)
      .withPreOrderHash(lastFillOrderhash)
      .withNextOrderHash(lastFillOrderhash)
      .withOwner(ByteString.copyFrom(orderinfoList(1).getBytes()))
      .withTokenS(firstFillTokenS)
      .withTokenB(lastFillTokenS)
      .withAmountS(safeBig2ByteString(firstFillAmountS))
      .withAmountB(safeBig2ByteString(lastFillAmountS))

    // todo
    //        // lrcFee or lrcReward, if >= 0 lrcFee, else lrcReward
    //        if lrcFeeOrReward := safeBig(e.OrderInfoList[start+5]); lrcFeeOrReward.Cmp(big.NewInt(0)) > 0 {
    //          fill.LrcFee = lrcFeeOrReward
    //        } else {
    //          fill.LrcFee = big.NewInt(0)
    //        }
    //
    //        // splitS or splitB: if > 0 splitS, else splitB
    //        if split := safeBig(e.OrderInfoList[start+6]); split.Cmp(big.NewInt(0)) > 0 {
    //          fill.SplitS = split
    //          fill.SplitB = big.NewInt(0)
    //        } else {
    //          fill.SplitS = big.NewInt(0)
    //          fill.SplitB = new(big.Int).Mul(split, big.NewInt(-1))
    //        }

    RingMined()
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
