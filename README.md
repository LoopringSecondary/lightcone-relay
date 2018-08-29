## lightcone-relay

[![Build Status](https://travis-ci.com/Loopring/lightcone-relay.svg?token=LFU5xhzys581aWFBPai3&branch=master)](https://travis-ci.com/Loopring/lightcone-relay)


## Compile
```
sbt compile
```

### Run
Here is how you can run two nodes to form a cluster:

```
sbt "core/run \
-p=19091 \
-m=8081 \
-s localhost:19091,localhost:19092 \
-r foo"
```

```
sbt "core/run \
-p=19092 \
-m=8082 \
-s localhost:19091,localhost:19092 \
-r bar"
```

Note: You must start the first node in the cluster first, otherwise singleton instances will not be deployed correctly.

You can see all top-level actors deployed on each of these nodes by visiting:

- [http://localhost:8081/stats](http://localhost:8081/stats)
- [http://localhost:8082/stats](http://localhost:8082/stats)

### Deploy actors dynamically

post the following as JSON to `http://localhost:8081/settings` to triger actor (re)depolyments.

```
{
    "balanceCacherSettings":
    {
        "roles": [],
        "instances": 2
    },
    "balanceManagerSettings":
    {
        "roles": ["foo"],
        "instances": 2
    },
    "orderAccessorSettingsSeq": [
    {
        "id": "abc",
        "roles": ["bar"],
        "instances": 2
    },
    {
        "id": "xyz",
        "roles": ["foo", "bar"],
        "instances": 2
    }]
}

```

There is a default configuration file at the root of the project, you can load it using `curl`:

```
curl \
-d "@default_dynamic_settings.json"  \
-H "Content-Type: application/json" \
-X POST http://localhost:8081/settings

```

Tp see the  global dynamic settings, visit:

- [http://localhost:8081/settings](http://localhost:8081/settings)
- [http://localhost:8082/settings](http://localhost:8082/settings)


Then visit `http://localhost:8081/stats` for the listed of all actors deployed.

