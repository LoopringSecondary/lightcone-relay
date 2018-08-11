

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

Note: You must start the first node in the cluster first, otherwise singleton instances will not be deployed correctly.

You can see all top-level actors deployed on each of these nodes by visiting:

- [http://192.168.1.152:8081](http://192.168.1.152:8081)
- [http://192.168.1.152:8082](http://192.168.1.152:8082)

### Deploy actors dynamically

post the following as JSON to `http://192.168.1.152:8081/config` to triger actor (re)depolyments.

```
{
    "version": 1,
    "marketConfigs": {},
    "actorDeployments": [
        {
            "actorName": "balance_cacher",
            "roles": ["all"],
            "numInstancesPerNode":10,
            "marketId": ""
        },
         {
            "actorName": "cache_obsoleter",
            "roles": ["all"],
            "numInstancesPerNode": 0,
            "marketId": ""
        }
    ]
}

```

There is a default configuration file at the root of the project, you can load it using `curl`:

```
curl \
-d "@default_cluster_config.json"  \
-H "Content-Type: application/json" \
-X POST http://localhost:8081/config

```

Then visit `http://localhost:8081/stats` for the listed of all actors deployed.

> Note that all `/user/router_*` actors and `/user/management_*` actors are deployed automatically and cannot be removed; and only `/user/service_*` actors can be dynamically deployed.
