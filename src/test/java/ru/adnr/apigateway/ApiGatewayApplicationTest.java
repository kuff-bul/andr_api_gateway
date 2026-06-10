package ru.adnr.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class ApiGatewayApplicationTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void routesFlowManagerApiThroughServiceDiscovery() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("flow-manager");
                    assertThat(route.getUri().toString()).isEqualTo("lb://flow-manager");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate ->
                                    assertThat(predicate.getArgs())
                                            .containsValues(
                                                    "/api/v1/files",
                                                    "/api/v1/files/**"));
                });
    }
}
