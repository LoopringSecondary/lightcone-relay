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

import org.loopring.lightcone.lib.abi.Bitstream
import org.scalatest.FlatSpec
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.core.richproto._
import org.loopring.lightcone.proto.order.RawOrder

class RingSubmitterSpec extends FlatSpec {

  info("execute cmd [sbt lib/'testOnly *RingSubmitterSpec -- -z submitRings'] to test single spec of submitRings")

  // 注意，合约里所有涉及到int,uint类型的都是number,使用hex表示,这里我们只能用string替代,包括validBegin,tokenSFeePercentage等
  // typeScript BitStream addNumber 4byte，addHex 32byte

  val originInput = "0x00000002000100040008000d00120000002b00300035003a0042004a00000001004b00000000005000000055006e00000000008700140000003c00640000000a0000008f003500300094009c00a40002000300a50000000000aa000000af00c8000000000087001400000032001900000014020001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000511ebd550bc240c7d167d9ecbf82eb5156dd787f357eb986926808b113c79ba5975dc1bae1fba79000000000000000000000000000000000000000000000000000000000000004300411c199ce84e4e725b129b944d39ecb803fd89f3223421968277ffa1a645e2719d223e88d0e5e74051aa2c31cc78180e2b4e76db3536df000e67bb9b2e6478a22907008f6a0679ed3767b2dc27fe28e159fb5ac02b725be8abb09646f9d774d8498e844c699a8f6bcd4964e90aa62b8239ed12cbb37f976e774e0ca75668f00000000000000000000000000000000000000000000000056bc75e2d63100000000000000000000000000000000000000000000000000014998f32ac787000005bc44bda142b64ef9621ceae082ee365acdaa065bc83eb99d5a65accc8b949a34e5ceb6601c95a7ed298039d000000000000000000000000000000000000000000000000000000000000004300411c71ed492038fce2dbbbe0c32539a99aa8b4a6954c7b2191582fa11f76bbe6c5dd53404277a95fc956bd80e74c36a19375db5b4e16f28702b4d0a507fbc5735eaa00000000000000000000000000000000000000000000000000000000000000004300411b332b032978e5511eabfdb242b3d8c80e22ce52ae3d554b622d877d773325f9af2cd524f524e88e81a64805603896f511dfad39faa2c0da1a672af28f232964c5000000000000000000000000000000000000000000000000000de0b6b3a764000087215d17d25e70aef5a17de3dca92af655345341000000000000000000000000000000000000000000000015af1d78b58c400000000000000000000000000000000000000000000000000005188315f776b800005bc44be009dee62034da2846bf4e09f16d008b94e4edf8b8372549c2f1a79652a7ed61aeba8a7f0fba010160000000000000000000000000000000000000000000000000000000000000004300411bab4d04add6229b5dc23cb20ea740a181f5705d053599ca5ceaf07681cbfe17041b6b8cb6524a9dfcbfb80c511d28569d3c517738023e93a43e9264c6987493f800000000000000000000000000000000000000000000000000000000000000004300411cd4fade623817d84f35313e3294d01e5fdd03933b7c60a585275acf7d3c7be9df3e0d3d043740ca53a1982b509856ecdc7f652293bf73788ff3ec634c5ea7755300"

  /*
  * { owner: '0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b',
       tokenS: '0xe8abb09646f9d774d8498e844c699a8f6bcd4964',
       tokenB: '0xe90aa62b8239ed12cbb37f976e774e0ca75668f0',
       amountS: 100000000000000000000,
       amountB: 380000000000000000000,
       tokenSFeePercentage: 60,
       tokenBFeePercentage: 100,
       walletAddr: '0xd5a65accc8b949a34e5ceb6601c95a7ed298039d',
       balanceS: 1e+26,
       balanceB: 1e+26,
       feeAmount: 1000000000000000000,
       feePercentage: 20,
       dualAuthSignAlgorithm: 0,
       dualAuthAddr: '0x142b64ef9621ceae082ee365acdaa065bc83eb99',
       allOrNone: false,
       validSince: 1539591130,
       walletSplitPercentage: 10,
       version: 0,
       validUntil: 0,
       tokenRecipient: '0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b',
       feeToken: '0x21e997a7132e79f7721d05cdab78a60df7884d1f',
       waiveFeePercentage: 0,
       hash: <Buffer bf c1 9e 87 db 69 36 72 a0 7a 2f 90 18 e3 09 85 22 30 9e 81 59 9a b7 f7 b6 a2 35 78 24 cc 50 32>,
       sig: '0x00411c71ed492038fce2dbbbe0c32539a99aa8b4a6954c7b2191582fa11f76bbe6c5dd53404277a95fc956bd80e74c36a19375db5b4e16f28702b4d0a507fbc5735eaa',
       dualAuthSig: '0x00411b332b032978e5511eabfdb242b3d8c80e22ce52ae3d554b622d877d773325f9af2cd524f524e88e81a64805603896f511dfad39faa2c0da1a672af28f232964c5' },

  * */

  "submitRings" should "" in {
    val amountB = "380000000000000000000"

    var ord = RawOrder()
      .withOwner("0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b")
      .withTokenS("0xe8abb09646f9d774d8498e844c699a8f6bcd4964")
      .withTokenB("0xe90aa62b8239ed12cbb37f976e774e0ca75668f0")
      .withAmountS(BigInt("100000000000000000000").toHex)
      .withAmountB(BigInt("380000000000000000000").toHex)
      .withFeeAmount(BigInt("1000000000000000000").toHex)
      .withValidSince(1539591130)
      .withValidUntil(0)
      .withDualAuthAddress("0x142b64ef9621ceae082ee365acdaa065bc83eb99")
      .withWallet("0xd5a65accc8b949a34e5ceb6601c95a7ed298039d")
      .withTokenRecipient("0x8f6a0679ed3767b2dc27fe28e159fb5ac02b725b")
      .withFeeToken("0x21e997a7132e79f7721d05cdab78a60df7884d1f")
      .withWalletSplitPercentage(10)
      .withFeePercentage(20)
      .withTokenSFeePercentage(60)
      .withTokenBFeePercentage(100)
      .withAllOrNone(false)

    info(ord.getHash())
  }

}
