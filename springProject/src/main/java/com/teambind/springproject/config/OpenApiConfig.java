package com.teambind.springproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String APP_TYPE_HEADER = "X-App-Type";
	private static final String USER_ID_HEADER = "X-User-Id";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Room Time Slot Management Service API")
						.version("1.0.0")
						.description("룸 시간 슬롯 관리 서비스 API"))
				.components(new Components()
						.addSecuritySchemes(APP_TYPE_HEADER,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name(APP_TYPE_HEADER)
										.description("앱 타입 (GENERAL, PLACE_MANAGER)"))
						.addSecuritySchemes(USER_ID_HEADER,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name(USER_ID_HEADER)
										.description("사용자 ID")))
				.addSecurityItem(new SecurityRequirement()
						.addList(APP_TYPE_HEADER)
						.addList(USER_ID_HEADER));
	}

	@Bean
	public OperationCustomizer operationCustomizer() {
		return (operation, handlerMethod) -> {
			operation.addParametersItem(
					new Parameter()
							.in("header")
							.name(APP_TYPE_HEADER)
							.description("앱 타입 (GENERAL, PLACE_MANAGER)")
							.required(false)
							.schema(new io.swagger.v3.oas.models.media.StringSchema()
									._enum(java.util.List.of("GENERAL", "PLACE_MANAGER"))));
			return operation;
		};
	}
}
