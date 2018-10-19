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

package org.loopring.lightcone.lib.solidity

import org.loopring.lightcone.lib.abi.{ RingsDeserializer, RingsGenerator }
import org.scalatest.{ FlatSpec, Matchers }
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.richproto._
import org.loopring.lightcone.lib.time.LocalSystemTimeProvider
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring._

class RingSubmitterSpec extends FlatSpec with Matchers {

  val originInput = "0x00000002000100030008000d00120000002b00300035003a0042004a00000001004b00000000005000000055006e00000000003a00140000000000000000000a00000087003500300042003a004a00020002008c0000000000500000009100aa00000000003a001400000000000000000014020001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000511ebd550bc240c7d167d9ecbf82eb5156dd787f357eb986926808b113c79ba5975dc1bae1fba79000000000000000000000000000000000000000000000000000000000000004300411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b008f6a0679ed3767b2dc27fe28e159fb5ac02b725be90aa62b8239ed12cbb37f976e774e0ca75668f021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000000000000000000000000003635c9adc5dea000005bc44ba9142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b40500000000000000000000000000000000000000000000000000000000000000004300411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe5865970087215d17d25e70aef5a17de3dca92af65534534109dee62034da2846bf4e09f16d008b94e4edf8b8000000000000000000000000000000000000000000000000000000000000004300411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e00000000000000000000000000000000000000000000000000000000000000004300411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d6000"
  val one = BigInt("1000000000000000000")
  val lrcAddress = "0x21e997a7132e79f7721d05cdab78a60df7884d1f"

  // 第一组测试数据
  "submitRings" should "" in {
    info("execute cmd [sbt lib/'testOnly *RingSubmitterSpec -- -z submitRings'] to test single spec of submitRings")

    val order1 = getOrder1()
    val order2 = getOrder2()

    val ringswithouthash = Rings()
      .withOrders(Seq(order1, order2))
      .withFeeRecipient("0x0511ebd550bc240c7d167d9ecbf82eb5156dd787")
      .withMiner("0xf357eb986926808b113c79ba5975dc1bae1fba79")
      .withTransactionOrigin("0x73d8f963642a21663e7617f796c75c99804b9e3b")
      .withSig("0x00411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b")
      .withRings(Seq(Ring(orderIdx = Seq(0, 1))))

    val ringhash = ringswithouthash.getHash()
    val rings = ringswithouthash.copy(hash = ringhash)

    val timeProvider = new LocalSystemTimeProvider
    val start = timeProvider.getTimeMillis
    val result = RingsGenerator(lrcAddress, rings).toSubmitableParam()
    val end = timeProvider.getTimeMillis

    info("generate ring.data spent " + (end - start).toString + "(msec)")

    result should be(originInput)
  }

  "deserializerRing" should "" in {
    info("execute cmd [sbt lib/'testOnly *RingSubmitterSpec -- -z deserializerRing'] to test single spec of deserializerRing")

    val hashseq = Seq(
      "0xc62a755f6530d68e34341e2a399afa65fb13921ca3119695a6710598e191906b",
      "0xcf1213628d4266455a935a64ce6cd3d68fbbc468936cad29dab38eeced987487"
    )
    val ringhash = "0x6cacf9c57af230d0d1d75364196dc144f049b23138200586a7e8d7e467e9355c"
    val result = RingsDeserializer(lrcAddress, originInput).deserialize()

    val orders = result.orders.map(x ⇒ {
      val essential = x.getEssential
      x.copy(essential = Option(essential.copy(hash = x.getHash())))
    })

    orders.map(x ⇒ {
      hashseq should contain(x.getEssential.hash)
    })

    val rings = result.copy(orders = orders)
    rings.getHash() should be(ringhash)
  }

  private def getOrder1(): RawOrder = {
    val essential = RawOrderEssential()
      .withTokenS("0xe90aa62b8239ed12cbb37f976e774e0ca75668f0")
      .withTokenB("0x21e997a7132e79f7721d05cdab78a60df7884d1f")
      .withAmountS(BigInt("1000000000000000000").toHex)
      .withAmountB((BigInt(1000) * one).toHex)
      .withOwner("0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b")
      .withFeeAmount(BigInt("1000000000000000000").toHex)
      .withFeePercentage(20)
      .withDualAuthAddress("0x142b64ef9621ceae082ee365acdaa065bc83eb99")
      .withAllOrNone(false)
      .withValidSince(1539591081)
      .withValidUntil(0)
      .withWallet("0xd5a65accc8b949a34e5ceb6601c95a7ed298039d")
      .withWalletSplitPercentage(10)
      .withTokenRecipient("0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b")
      .withFeeToken("0x21e997a7132e79f7721d05cdab78a60df7884d1f")
      .withBroker("0x0")
      .withOrderInterceptor("0x0")
      .withTokenSFeePercentage(0)
      .withTokenBFeePercentage(0)

    val hash = RawOrder().withEssential(essential).getHash()
    info("orderhash1: " + hash)
    val newesstial = essential.copy(hash = hash)

    RawOrder()
      .withEssential(newesstial)
      .withWaiveFeePercentage(0)
      .withSig("0x00411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b405")
      .withDualAuthSig("0x00411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe586597")

  }

  private def getOrder2(): RawOrder = {
    val essential = RawOrderEssential()
      .withTokenS("0x21e997a7132e79f7721d05cdab78a60df7884d1f")
      .withTokenB("0xe90aa62b8239ed12cbb37f976e774e0ca75668f0")
      .withAmountS((BigInt(1000) * one).toHex)
      .withAmountB(BigInt("1000000000000000000").toHex)
      .withOwner("0x87215d17d25e70aef5a17de3dca92af655345341")
      .withFeeAmount(BigInt("1000000000000000000").toHex)
      .withFeePercentage(20)
      .withDualAuthAddress("0x09dee62034da2846bf4e09f16d008b94e4edf8b8")
      .withAllOrNone(false)
      .withValidSince(1539591081)
      .withValidUntil(0)
      .withWallet("0xd5a65accc8b949a34e5ceb6601c95a7ed298039d")
      .withWalletSplitPercentage(20)
      .withTokenRecipient("0x87215d17d25e70aef5a17de3dca92af655345341")
      .withFeeToken("0x21e997a7132e79f7721d05cdab78a60df7884d1f")
      .withBroker("0x0")
      .withOrderInterceptor("0x0")
      .withTokenSFeePercentage(0)
      .withTokenBFeePercentage(0)

    val hash = RawOrder().withEssential(essential).getHash()
    info("orderhash: " + hash)
    val newesstial = essential.copy(hash = hash)

    RawOrder()
      .withEssential(newesstial)
      .withWaiveFeePercentage(0)
      .withSig("0x00411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e")
      .withDualAuthSig("0x00411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d60")

  }

}
