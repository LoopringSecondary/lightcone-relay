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

import com.typesafe.config.ConfigFactory
import org.loopring.lightcone.lib.abi.LoopringAbi
import org.loopring.lightcone.proto.block_chain_event.{ SubmitRing, TxHeader }
import org.scalatest.FlatSpec
import org.spongycastle.util.encoders.Hex

class SubmitRingSpec extends FlatSpec {

  info("execute cmd [sbt lib/'testOnly *SubmitRingSpec'] to test single spec of submitRing")

  val config = ConfigFactory.parseString(
    """
      |abi {
      |  basedir = "/Users/fukun/projects/javahome/github.com/Loopring/lightcone-relay/core/src/main/resources/abi/"
      |  loopring = "loopring.json"
      |}
      |
    """.stripMargin)

  val loopringAbi = new LoopringAbi(config)
  val originInput = "e78aadb20000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000044000000000000000000000000000000000000000000000000000000000000004a0000000000000000000000000000000000000000000000000000000000000054000000000000000000000000000000000000000000000000000000000000005e00000000000000000000000005552dcfba48c94544beaaf26470df9898e050ac2000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000004175015b30ff8d116989cdf61f4c7dba15d422ff000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2000000000000000000000000b94065482ad64d4c2b9252358d746b39e820a58200000000000000000000000090a16f7264897b325009e4040c374e6f13cee30e000000000000000000000000897b12870a0490adc56c8cc57ff0a404be35b6280000000000000000000000001b793e49237758dbd8b752afc9eb4b329d5da016000000000000000000000000b94065482ad64d4c2b9252358d746b39e820a5820000000000000000000000001e702595814d05358067a41f8716d553b8492ca500000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000032dbac9a2e6f08000000000000000000000000000000000000000000000003e15283235ae89440000000000000000000000000000000000000000000000000000000000005b2f6c15000000000000000000000000000000000000000000000000000000005b56f91500000000000000000000000000000000000000000000000918b15af51e8c00000000000000000000000000000000000000000000000000032dbac9a2e6f080000000000000000000000000000000000000000000000012a27d53bc0487000000000000000000000000000000000000000000000000000000f43fc2c04ee00000000000000000000000000000000000000000000000000000000000005b557953000000000000000000000000000000000000000000000000000000005b56cad30000000000000000000000000000000000000000000000032c4454e50f9d00000000000000000000000000000000000000000000000012a27d53bc04870000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000003200000000000000000000000000000000000000000000000000000000000000320000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b0000000000000000000000000000000000000000000000000000000000000004a58f7592f40145b2f08aae392eb2f3977dd62135d2e5e5ed85fa54ac7e18700154f815446924000545a89081f23f401091e1ba846bad04e8f1ca9ddae96c4d1a5a62d331e93de11b1494a6e3d4c7b251005617d026ac2929ef72f3259d94fa5532328de3016aeb0baafcc65d09fdeda093ca9d5b51721f1eb78f3799f4e2184600000000000000000000000000000000000000000000000000000000000000046f6bed699b5fc3c386d04bc1fbbbbc289c6316517b3b8506415a0668dc0063133b408b904a1df09f3642af3130060e2d678989617e3e56dcc44dd69652e8e27f144bd8ffee74d52872863b6a72f6d6757ffc35ae900b95c59642111c7c77b7860e26b6d82ab8f007c8c910df86cfd3eba70ecdbae59505a400febaa7e9f609d6"
  val input = Hex.decode("000000000000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000044000000000000000000000000000000000000000000000000000000000000004a0000000000000000000000000000000000000000000000000000000000000054000000000000000000000000000000000000000000000000000000000000005e00000000000000000000000005552dcfba48c94544beaaf26470df9898e050ac2000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000004175015b30ff8d116989cdf61f4c7dba15d422ff000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2000000000000000000000000b94065482ad64d4c2b9252358d746b39e820a58200000000000000000000000090a16f7264897b325009e4040c374e6f13cee30e000000000000000000000000897b12870a0490adc56c8cc57ff0a404be35b6280000000000000000000000001b793e49237758dbd8b752afc9eb4b329d5da016000000000000000000000000b94065482ad64d4c2b9252358d746b39e820a5820000000000000000000000001e702595814d05358067a41f8716d553b8492ca500000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000032dbac9a2e6f08000000000000000000000000000000000000000000000003e15283235ae89440000000000000000000000000000000000000000000000000000000000005b2f6c15000000000000000000000000000000000000000000000000000000005b56f91500000000000000000000000000000000000000000000000918b15af51e8c00000000000000000000000000000000000000000000000000032dbac9a2e6f080000000000000000000000000000000000000000000000012a27d53bc0487000000000000000000000000000000000000000000000000000000f43fc2c04ee00000000000000000000000000000000000000000000000000000000000005b557953000000000000000000000000000000000000000000000000000000005b56cad30000000000000000000000000000000000000000000000032c4454e50f9d00000000000000000000000000000000000000000000000012a27d53bc04870000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000003200000000000000000000000000000000000000000000000000000000000000320000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b0000000000000000000000000000000000000000000000000000000000000004a58f7592f40145b2f08aae392eb2f3977dd62135d2e5e5ed85fa54ac7e18700154f815446924000545a89081f23f401091e1ba846bad04e8f1ca9ddae96c4d1a5a62d331e93de11b1494a6e3d4c7b251005617d026ac2929ef72f3259d94fa5532328de3016aeb0baafcc65d09fdeda093ca9d5b51721f1eb78f3799f4e2184600000000000000000000000000000000000000000000000000000000000000046f6bed699b5fc3c386d04bc1fbbbbc289c6316517b3b8506415a0668dc0063133b408b904a1df09f3642af3130060e2d678989617e3e56dcc44dd69652e8e27f144bd8ffee74d52872863b6a72f6d6757ffc35ae900b95c59642111c7c77b7860e26b6d82ab8f007c8c910df86cfd3eba70ecdbae59505a400febaa7e9f609d6")

  "submitRing" should "convert to two orders" in {
    val method = loopringAbi.findFunctionByName("submitRing")
    val list = method.decode(input).toArray().toSeq
    val header = TxHeader()

    loopringAbi.assembleSubmitRingFunction(list, header) match {
      case s: SubmitRing => info(s.toProtoString)
      case _ => info("unpack failed")
    }

  }

}
