package br.com.voting_system_api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class VotingSystemApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(VotingSystemApiGatewayApplication.class, args);
	}

}