package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> serviceRoutes() {
		return GatewayRouterFunctions.route("user-service")
				.route(path("/users/**").or(path("/adduser")), HandlerFunctions.http())
				.before(uri("http://localhost"))
				.filter(lb("user-service"))
				.build()
				.and(GatewayRouterFunctions.route("hotel-service")
						.route(path("/hotels/**"), HandlerFunctions.http())
						.before(uri("http://localhost"))
						.filter(lb("hotel-service"))
						.build())
				.and(GatewayRouterFunctions.route("rating-service")
						.route(path("/ratings/**"), HandlerFunctions.http())
						.before(uri("http://localhost"))
						.filter(lb("rating-service"))
						.build());
	}
}