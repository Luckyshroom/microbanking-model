package io.mimoza;

import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class MimozaStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimozaStarter.class);

    public static void main(String[] args){
        final ClusterManager mgr = new HazelcastClusterManager();
        final VertxOptions options = new VertxOptions().setClusterManager(mgr);
        Vertx.clusteredVertx(options, cluster -> {
            if (cluster.succeeded()) {
                cluster.result().deployVerticle(new MimozaVerticle(), res -> {
                    if(res.succeeded()){
                        LOGGER.info("Deployment id is: " + res.result());
                    } else {
                        LOGGER.error("Deployment failed!");
                    }
                });
            } else {
                LOGGER.error("Cluster up failed: " + cluster.cause());
            }
        });
    }
}
