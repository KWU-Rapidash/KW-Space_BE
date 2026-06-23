package com.example.KW_SPACE.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
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
				new Tag().name("Health").description("서비스 상태 확인 API"),
				new Tag().name("Auth").description("KW Space API Spec - Auth")
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
							new StringSchema().name("status").description("서비스 상태"))))))
				.addPathItem("/api/auth/login", post("Auth", "로그인", "학번과 비밀번호로 로그인하고 JWT Cookie를 발급한다.",
					object("LoginRequest", field("klasId", "학번"), field("password", "비밀번호")),
					messageResponse("LoginResponse", "로그인 성공 여부와 JWT Cookie 발급 결과")))
				.addPathItem("/api/auth/signup", post("Auth", "회원가입", "KLAS 재학생 인증 후 서비스 계정을 생성한다.",
					object("SignupRequest",
						field("klasId", "KLAS 학번"),
						field("klasPassword", "KLAS 인증용 비밀번호"),
						field("password", "서비스 로그인 비밀번호"),
						field("phoneNumber", "전화번호")),
					messageResponse("SignupResponse", "회원가입 성공 여부")))
				.addPathItem("/api/auth/password-reset", post("Auth", "비밀번호 재설정", "KLAS 인증 후 서비스 비밀번호를 재설정한다.",
					object("PasswordResetRequest",
						field("klasId", "KLAS 학번"),
						field("klasPassword", "KLAS 인증용 비밀번호"),
						field("newPassword", "새 서비스 비밀번호")),
					messageResponse("PasswordResetResponse", "비밀번호 재설정 성공 여부"))));
	}

	private static PathItem post(String tag, String summary, String description, Schema<?> request, Schema<?> response) {
		return new PathItem().post(new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.requestBody(jsonRequest(request))
			.responses(ok(response.getName(), response)));
	}

	private static RequestBody jsonRequest(Schema<?> schema) {
		return new RequestBody()
			.required(true)
			.content(jsonContent(schema));
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

	private static Schema<?> field(String name, String description) {
		return new StringSchema().name(name).description(description);
	}

	private static Schema<?> messageResponse(String name, String message) {
		return object(name,
			new BooleanSchema().name("success").description("성공 여부"),
			new StringSchema().name("message").description(message));
	}
}
