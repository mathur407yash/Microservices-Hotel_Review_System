package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewayRoutesProperties.class)
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> serviceRoutes(GatewayRoutesProperties properties) {
		RouterFunction<ServerResponse> routes = null;
		for (GatewayRoutesProperties.Route route : properties.getRoutes()) {
			if (route.getId() == null || route.getServiceId() == null || route.getPaths().isEmpty()) {
				throw new IllegalStateException("Each gateway route must define an id, serviceId, and at least one path");
			}

			RequestPredicate predicate = null;
			for (String routePath : route.getPaths()) {
				RequestPredicate pathPredicate = path(routePath);
				predicate = predicate == null ? pathPredicate : predicate.or(pathPredicate);
			}

			RouterFunction<ServerResponse> serviceRoute = GatewayRouterFunctions.route(route.getId())
					.route(predicate, HandlerFunctions.http())
					.before(uri("http://localhost"))
					.filter(lb(route.getServiceId()))
					.build();
			routes = routes == null ? serviceRoute : routes.and(serviceRoute);
		}
		if (routes == null) {
			throw new IllegalStateException("At least one gateway route must be configured");
		}
		return routes;
	}
}