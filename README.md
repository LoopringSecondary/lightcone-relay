

## Compile
```
sbt compile
```

### Run
Here is how you can run two nodes to form a cluster (assuming your IP is '192.168.1.152'):

```
sbt "core/run \
-p=19091 \
-m=8081 \
-s 192.168.1.152:19091,192.168.1.152:19092 \
-r all"
```

```
sbt "core/run \
-p=19092 \
-m=8082 \
-s 192.168.1.152:19091,192.168.1.152:19092 \
-r all"
```

You can see all top-level actors deployed on each of these nodes by visiting:

- [http://192.168.1.152:8081](http://192.168.1.152:8081)
- [http://192.168.1.152:8082](http://192.168.1.152:8082)