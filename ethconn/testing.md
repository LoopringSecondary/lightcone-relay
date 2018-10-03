# Testing

## web3_clientVersion

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"web3_clientVersion","params":[],"id":67}' 'http://localhost:8080'
```

## web3_sha3

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"web3_sha3","params":["0x68656c6c6f20776f726c64"],"id":67}' 'http://localhost:8080'
```

## net_version

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"net_version","params":[],"id":67}' 'http://localhost:8080'
```

## net_listening

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"net_listening","params":[],"id":67}' 'http://localhost:8080'
```

## net_peerCount

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"net_peerCount","params":[],"id":67}' 'http://localhost:8080'
```

## eth_protocolVersion

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_protocolVersion","params":[],"id":67}' 'http://localhost:8080'
```

## eth_syncing

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":67}' 'http://localhost:8080'
```

## eth_coinbase

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_coinbase","params":[],"id":67}' 'http://localhost:8080'
```

## eth_mining

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_mining","params":[],"id":67}' 'http://localhost:8080'
```

## eth_hashrate

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_hashrate","params":[],"id":67}' 'http://localhost:8080'
```

## eth_gasPrice

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_gasPrice","params":[],"id":67}' 'http://localhost:8080'
```

## eth_accounts

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_accounts","params":[],"id":67}' 'http://localhost:8080'
```

## eth_blockNumber

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":67}' 'http://localhost:8080'
```

## eth_getBalance

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getBalance","params":["0xe9d52364f5a3d57d24bd6a5ec58dd9b8932b4b6c", "latest"],"id":67}' 'http://localhost:8080'
```

## eth_getStorageAt

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getStorageAt","params":["0xe9d52364f5a3d57d24bd6a5ec58dd9b8932b4b6c", "0x0", "latest"],"id":67}' 'http://localhost:8080'
```

## eth_getTransactionCount

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getTransactionCount","params":["0xe9d52364f5a3d57d24bd6a5ec58dd9b8932b4b6c", "latest"],"id":67}' 'http://localhost:8080'
```

## eth_getBlockTransactionCountByHash

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getBlockTransactionCountByHash","params":["0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"],"id":67}' 'http://localhost:8080'
```

## eth_getBlockTransactionCountByNumber

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getBlockTransactionCountByNumber","params":["latest"],"id":67}' 'http://localhost:8080'
```

## eth_getUncleCountByBlockHash

https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getunclecountbyblockhash

# Errors

## eth_getUncleCountByBlockNumber

https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getunclecountbyblocknumber

## eth_getCode

eth_getCode
eth_sign
eth_sendTransaction
eth_sendRawTransaction
eth_call
eth_estimateGas
eth_getBlockByHash

## eth_getBlockByNumber

```
curl -H "Content-Type: application/json" -X POST --data '{"jsonrpc":"2.0","method":"eth_getTransactionByHash","params":["0x88df016429689c079f3b2f6ad39fa052532c56795b733da78a91ebe6a713944b"],"id":1}' 'http://localhost:8080'
```

eth_getTransactionByBlockHashAndIndex
eth_getTransactionByBlockNumberAndIndex
eth_getTransactionReceipt
eth_getUncleByBlockHashAndIndex
eth_getUncleByBlockNumberAndIndex
eth_getCompilers
eth_compileLLL
eth_compileSolidity
eth_compileSerpent
eth_newFilter
eth_newBlockFilter
eth_newPendingTransactionFilter
eth_uninstallFilter
eth_getFilterChanges
eth_getFilterLogs
eth_getLogs
eth_getWork
eth_submitWork
eth_submitHashrate
db_putString
db_getString
db_putHex
db_getHex
shh_post
shh_version
shh_newIdentity
shh_hasIdentity
shh_newGroup
shh_addToGroup
shh_newFilter
shh_uninstallFilter
shh_getFilterChanges
shh_getMessages
































