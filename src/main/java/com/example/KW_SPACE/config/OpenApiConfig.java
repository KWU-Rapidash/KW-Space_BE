package com.example.KW_SPACE.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI kwSpaceOpenAPI(@Value("${spring.application.version:0.0.1-SNAPSHOT}") String version) {
		return new OpenAPI()
			.info(new Info()
				.title("KW-Space API")
				.description("새빛관 대여 시스템 Backend API")
				.version(version))
			.tags(List.of(
				new Tag().name("Health").description("서비스 상태 확인 API")
			))
			.components(new Components()
				.addSecuritySchemes("accessTokenCookie", new SecurityScheme()
					.type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.COOKIE)
					.name("accessToken")
					.description("로그인 성공 시 발급되는 JWT HttpOnly Cookie")))
			.paths(new Paths()
				.addPathItem("/api/health", new PathItem()
					.get(new Operation()
						.addTagsItem("Health")
						.summary("헬스체크")
						.description("서비스 상태를 확인한다.")
						.responses(ok("HealthResponse", object("HealthResponse",
							new StringSchema().name("status").description("서비스 상태")))))));
	}

	private static ApiResponses ok(String name, Schema<?> schema) {
		return new ApiResponses()
			.addApiResponse("200", new ApiResponse()
				.description("OK")
				.content(jsonContent(schema)))
			.addApiResponse("400", new ApiResponse().description("잘못된 요청"))
			.addApiResponse("401", new ApiResponse().description("인증 실패 또는 인증 필요"));
	}

	private static Content jsonContent(Schema<?> schema) {
		return new Content().addMediaType("application/json", new MediaType().schema(schema));
	}

	private static Schema<?> object(String name, Schema<?>... fields) {
		ObjectSchema schema = new ObjectSchema();
		schema.name(name);
		for (Schema<?> field : fields) {
			schema.addProperty(field.getName(), field);
		}
		return schema;
	}
}
