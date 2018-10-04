# Ethcube

[![Build Status](https://travis-ci.com/Loopring/ethcube.svg?token=LFU5xhzys581aWFBPai3&branch=master)](https://travis-ci.com/Loopring/ethcube)

对以太坊API进行包装

# 设计

使用Akka的路由的功能对不同客户端采用单独的actor控制, 使用环形路由对节点做环形访问请求, 再使用广播路由对没给客户端actor检查区块高度, 便于环形路由控制客户端actor的访问控制。

<img src="./docs/ethcube.png"/>

## 环形路由

对外提供接口可以访问到环形路由, 有路由内部自动取下一个操作。

## 广播路由

程序定时向路由中的每个节点发送消息, 验证区块高度, 不合适的节点不会加入到环路由中。


# 运行

以下命令都是在工程路径下面执行, 运行项目可以使用环境变量或是命令行参数


## 编译

```
sbt clean compile
```

## eclipse 编译

```
sbt clean compile eclipse
```

## 运行

```
sbt run
```

## 指定配置文件

```

# 下面的命令会自动读取 $APP_HOME/src/main/resources/test.conf

sbt -Denv=test run

```

## docker 命令

### 打包

1. 这里只配置了发布到本地的参数, 没有配置远端repository
2. 必须使用参数 -Denv=docker 程序内部已经配置好了关于 "env=docker" 相关属性 

```
sbt -Denv=docker docker:publishLocal
```

### 运行

1. 程序对外端口是 9000, docker 需要映射一下
2. logs 是程序日志文件夹, 自动写入 console.log
3. conf 是配置文件夹, 程序自动检测 docker.conf, 没有会自动创建一个, 也可以自动一个docker.conf

```
docker run -d --name ethcube -v ~/logs:/opt/docker/logs -v ~/conf:/opt/docker/conf -p 9000:9000 CONTAINER_ID
```



# 功能

1. 提供以太坊jsonrpc API 查询功能
2. 扩充了jsonrpc的多请求功能, 使用数组jsonrpc post 到 /loor 会自动转发所有请求, 并收集全部结果再返回
3. 使用广播功能获取每个客户端同步情况, 再可控制环形路由的routees, 如果无路由可用返回 JsonRpcError(600, "has no routee")


