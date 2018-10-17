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

import org.loopring.lightcone.lib.abi.RingsGenerator
import org.scalatest.FlatSpec
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.richproto._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring._

class RingSubmitterSpec extends FlatSpec {

  info("execute cmd [sbt lib/'testOnly *RingSubmitterSpec -- -z submitRings'] to test single spec of submitRings")

  val originInput = "0x00000002000100030008000d00120000002b00300035003a0042004a00000001004b00000000005000000055006e00000000003a00140000000000000000000a00000087003500300042003a004a00020002008c0000000000500000009100aa00000000003a001400000000000000000014020001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000511ebd550bc240c7d167d9ecbf82eb5156dd787f357eb986926808b113c79ba5975dc1bae1fba79000000000000000000000000000000000000000000000000000000000000004300411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b008f6a0679ed3767b2dc27fe28e159fb5ac02b725be90aa62b8239ed12cbb37f976e774e0ca75668f021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000000000000000000000000003635c9adc5dea000005bc44ba9142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b40500000000000000000000000000000000000000000000000000000000000000004300411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe5865970087215d17d25e70aef5a17de3dca92af65534534109dee62034da2846bf4e09f16d008b94e4edf8b8000000000000000000000000000000000000000000000000000000000000004300411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e00000000000000000000000000000000000000000000000000000000000000004300411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d6000"
  val mriginInput = "0x00000002000100040008000d00120000002b00300035003a0042004a00000001004b00000000005000000055006e00000087008c00140000000000000000000a000000940099009e00a300ab00b30002000300b40000000000b9000000be00d7000000f000f500140000000000000000000a020001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000511ebd550bc240c7d167d9ecbf82eb5156dd787f357eb986926808b113c79ba5975dc1bae1fba79000000000000000000000000000000000000000000000000000000000000004300411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b008f6a0679ed3767b2dc27fe28e159fb5ac02b725be90aa62b8239ed12cbb37f976e774e0ca75668f021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000000000000000000000000003635c9adc5dea000005bc44ba9142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b40500000000000000000000000000000000000000000000000000000000000000004300411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe5865970021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a76400008f6a0679ed3767b2dc27fe28e159fb5ac02b725be90aa62b8239ed12cbb37f976e774e0ca75668f021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000000000000000000000000003635c9adc5dea000005bc44ba9142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e00000000000000000000000000000000000000000000000000000000000000004300411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d600021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a7640000"
  val one = BigInt("1000000000000000000")

  /*
  * ringsInfo: { rings: [ [ 0, 1 ] ],
  orders:
   [ { tokenS: '0xe90aa62b8239ed12cbb37f976e774e0ca75668f0',
       tokenB: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       amountS: 1000000000000000000,
       amountB: 1e+21,
       balanceS: 1e+26,
       balanceFee: 1e+26,
       balanceB: 1e+26,
       owner: '0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b',
       feeAmount: 1000000000000000000,
       feePercentage: 20,
       dualAuthSignAlgorithm: 0,
       dualAuthAddr: '0x142b64ef9621ceae082ee365acdaa065bc83eb99',
       allOrNone: false,
       validSince: 1539591081,
       walletAddr: '0xd5a65accc8b949a34e5ceb6601c95a7ed298039d',
       walletSplitPercentage: 10,
       version: 0,
       validUntil: 0,
       tokenRecipient: '0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b',
       feeToken: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       waiveFeePercentage: 0,
       tokenSFeePercentage: 0,
       tokenBFeePercentage: 0,
       hash: <Buffer c6 2a 75 5f 65 30 d6 8e 34 34 1e 2a 39 9a fa 65 fb 13 92 1c a3 11 96 95 a6 71 05 98 e1 91 90 6b>,
       sig: '0x00411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b405',
       dualAuthSig: '0x00411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe586597' },
     { tokenS: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       tokenB: '0xe90aa62b8239ed12cbb37f976e774e0ca75668f0',
       amountS: 1e+21,
       amountB: 1000000000000000000,
       balanceS: 1e+26,
       balanceFee: 1e+26,
       balanceB: 1e+26,
       owner: '0x87215d17d25e70aef5a17de3dca92af655345341',
       feeAmount: 1000000000000000000,
       feePercentage: 20,
       dualAuthSignAlgorithm: 0,
       dualAuthAddr: '0x09dee62034da2846bf4e09f16d008b94e4edf8b8',
       allOrNone: false,
       validSince: 1539591081,
       walletAddr: '0xd5a65accc8b949a34e5ceb6601c95a7ed298039d',
       walletSplitPercentage: 20,
       version: 0,
       validUntil: 0,
       tokenRecipient: '0x87215d17d25e70aef5a17de3dca92af655345341',
       feeToken: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       waiveFeePercentage: 0,
       tokenSFeePercentage: 0,
       tokenBFeePercentage: 0,
       hash: <Buffer cf 12 13 62 8d 42 66 45 5a 93 5a 64 ce 6c d3 d6 8f bb c4 68 93 6c ad 29 da b3 8e ec ed 98 74 87>,
       sig: '0x00411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e',
       dualAuthSig: '0x00411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d60' } ],
  transactionOrigin: '0x73d8f963642a21663e7617f796c75c99804b9e3b',
  feeRecipient: '0x0511ebd550bc240c7d167d9ecbf82eb5156dd787',
  miner: '0xf357eb986926808b113c79ba5975dc1bae1fba79',
  hash: <Buffer eb 02 69 96 ad ce 9d dd dd 8b 2f 60 a9 c3 06 33 0a 42 a2 47 47 8f 72 d2 4a 0d eb fd e6 85 a3 7a>,
  sig: '0x00411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b' }
data: 0x00000002000100030008000d00120000002b00300035003a0042004a00000001004b00000000005000000055006e00000000003a00140000000000000000000a00000087003500300042003a004a00020002008c0000000000500000009100aa00000000003a001400000000000000000014020001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000511ebd550bc240c7d167d9ecbf82eb5156dd787f357eb986926808b113c79ba5975dc1bae1fba79000000000000000000000000000000000000000000000000000000000000004300411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b008f6a0679ed3767b2dc27fe28e159fb5ac02b725be90aa62b8239ed12cbb37f976e774e0ca75668f021e997a7132e79f7721d05cdab78a60df7884d1f0000000000000000000000000000000000000000000000000de0b6b3a764000000000000000000000000000000000000000000000000003635c9adc5dea000005bc44ba9142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b40500000000000000000000000000000000000000000000000000000000000000004300411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe5865970087215d17d25e70aef5a17de3dca92af65534534109dee62034da2846bf4e09f16d008b94e4edf8b8000000000000000000000000000000000000000000000000000000000000004300411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e00000000000000000000000000000000000000000000000000000000000000004300411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d6000
gas used: 300399
      ✓ the first one, always cost more gas than expected, ignore this one (13630ms)
  * */

  "submitRings" should "" in {
    val essential1 = RawOrderEssential()
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

    val hash1 = RawOrder().withEssential(essential1).getHash()
    info("orderhash1: " + hash1)
    val newesstial1 = essential1.withHash(hash1)

    val order1 = RawOrder()
      .withEssential(essential1)
      .withWaiveFeePercentage(0)
      .withSig("0x00411b9bacc6dc8ebfdd190ebf41d5db9c71d90ebc9bb76ee4bbfff5c98995000b40e94808a768fc07ab6c9b91eff50f0c02de7f04f570274835b05b6039846e71b405")
      .withDualAuthSig("0x00411b490f79938872e7f0604a2b72bb01e746ea7c86e834fe3e4a131d4e6ec2ef58a01d871aad68d0080f4819adbbe437e4e19aa99218b0e58e4928dc79cafe586597")

    /*
    * { tokenS: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       tokenB: '0xe90aa62b8239ed12cbb37f976e774e0ca75668f0',
       amountS: 1e+21,
       amountB: 1000000000000000000,
       balanceS: 1e+26,
       balanceFee: 1e+26,
       balanceB: 1e+26,
       owner: '0x87215d17d25e70aef5a17de3dca92af655345341',
       feeAmount: 1000000000000000000,
       feePercentage: 20,
       dualAuthSignAlgorithm: 0,
       dualAuthAddr: '0x09dee62034da2846bf4e09f16d008b94e4edf8b8',
       allOrNone: false,
       validSince: 1539591081,
       walletAddr: '0xd5a65accc8b949a34e5ceb6601c95a7ed298039d',
       walletSplitPercentage: 20,
       version: 0,
       validUntil: 0,
       tokenRecipient: '0x87215d17d25e70aef5a17de3dca92af655345341',
       feeToken: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       waiveFeePercentage: 0,
       tokenSFeePercentage: 0,
       tokenBFeePercentage: 0,
       hash: <Buffer cf 12 13 62 8d 42 66 45 5a 93 5a 64 ce 6c d3 d6 8f bb c4 68 93 6c ad 29 da b3 8e ec ed 98 74 87>,
       sig: '0x00411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e',
       dualAuthSig: '0x00411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d60'
       },
  transactionOrigin: '0x73d8f963642a21663e7617f796c75c99804b9e3b',
  feeRecipient: '0x0511ebd550bc240c7d167d9ecbf82eb5156dd787',
  miner: '0xf357eb986926808b113c79ba5975dc1bae1fba79',
  hash: <Buffer eb 02 69 96 ad ce 9d dd dd 8b 2f 60 a9 c3 06 33 0a 42 a2 47 47 8f 72 d2 4a 0d eb fd e6 85 a3 7a>,
  sig: '0x00411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b' }
    * */
    val essential2 = RawOrderEssential()
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

    val hash2 = RawOrder().withEssential(essential2).getHash()
    info("orderhash2: " + hash2)
    val newesstial2 = essential1.withHash(hash2)

    val order2 = RawOrder()
      .withEssential(essential1)
      .withWaiveFeePercentage(0)
      .withSig("0x00411cccb1526721ce00f8b57a538ab9465f1d6514e670e961a69d781956003c2b5316033244a5657d4fb69ffb52e1c6ccb64639fbee83f4f03e148f780c2f56567c4e")
      .withDualAuthSig("0x00411bba3a7913e9a1548067741e6c8652ad7d3803fc970ab53314236f63176f205bd06680de1295a94b130075b056f8f08e18b6c26f26e1288e0ce7660410daf54d60")

    val ringswithouthash = Rings()
      .withOrders(Seq(order1, order2))
      .withFeeRecipient("0x0511ebd550bc240c7d167d9ecbf82eb5156dd787")
      .withMiner("0xf357eb986926808b113c79ba5975dc1bae1fba79")
      .withTransactionOrigin("0x73d8f963642a21663e7617f796c75c99804b9e3b")
      .withSig("0x00411ce2bb66d38d0ffd98d73abdfc12563aee1b993e5c9a1d6b87bf9491c0ca2dd8c55aad23ddc053ad4f6ecf0513d857147d9f7445127607e502214a82d9854dc72b")
      .withRings(Seq(Ring(orderIdx = Seq(0, 1))))

    val ringhash = ringswithouthash.getHash()
    info("ringhash: " + ringhash)
    val rings = ringswithouthash.copy(hash = ringhash)

    val result = RingsGenerator(rings).toSubmitableParam()
    info(result)
    info(result.toLowerCase.equals(originInput.toLowerCase).toString)
  }

}
