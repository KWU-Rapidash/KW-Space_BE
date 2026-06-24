package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.cookie.AuthCookieProperties;
import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI kwSpaceOpenAPI(@Value("${spring.application.version:0.0.1-SNAPSHOT}") String version,
		AuthCookieProperties authCookieProperties) {
		return new OpenAPI()
			.info(new Info()
				.title("KW-Space API")
				.description("새빛관 대여 시스템 Backend API")
				.version(version))
			.tags(List.of(
				new Tag().name("Health").description("서비스 상태 확인 API"),
				new Tag().name("Auth").description("KW Space API Spec - Auth"),
				new Tag().name("Reserve").description("KW Space API Spec - Reserve"),
				new Tag().name("User").description("KW Space API Spec - User")
			))
			.components(new Components()
				.addSecuritySchemes("accessTokenCookie", new SecurityScheme()
					.type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.COOKIE)
					.name(authCookieProperties.accessTokenName())
					.description("로그인 성공 시 발급되는 JWT HttpOnly Cookie")))
			.paths(new Paths()
				.addPathItem("/api/health", new PathItem()
					.get(new Operation()
						.addTagsItem("Health")
						.summary("헬스체크")
						.description("서비스 상태를 확인한다.")
						.responses(ok("HealthResponse", object("HealthResponse",
							new StringSchema().name("status").description("서비스 상태"))))))
				.addPathItem("/api/v1/auth/login", post("Auth", "로그인", "학번과 비밀번호로 로그인하고 JWT Cookie를 발급한다.",
					object("LoginRequest", field("klasId", "학번"), field("password", "비밀번호")),
					messageResponse("LoginResponse", "로그인 성공 여부와 JWT Cookie 발급 결과")))
				.addPathItem("/api/v1/auth/logout", post("Auth", "로그아웃", "accessToken Cookie를 삭제한다.",
					messageResponse("LogoutResponse", "로그아웃 성공 여부와 Cookie 삭제 결과")))
				.addPathItem("/api/v1/auth/signup", postWithResponses("Auth", "회원가입", "KLAS 재학생 인증 후 서비스 계정을 생성한다.",
					object("SignupRequest",
						field("name", "이름"),
						field("klasId", "학번"),
						field("klasPassword", "KLAS 인증용 비밀번호"),
						field("password", "서비스 로그인 비밀번호")),
					signupResponses()))
				.addPathItem("/api/v1/auth/password-reset", postWithResponses("Auth", "비밀번호 재설정", "KLAS 인증 후 서비스 비밀번호를 재설정한다.",
					object("PasswordResetRequest",
						field("klasId", "학번"),
						field("klasPassword", "KLAS 인증용 비밀번호"),
						field("newPassword", "새 서비스 비밀번호")),
					passwordResetResponses()))
				.addPathItem("/api/v1/reservations", securedPostWithResponses("Reserve", "강의실 예약", "선택한 날짜, 강의실, 시간대로 예약을 생성한다.",
					object("ReservationCreateRequest",
						classroomIdSchema().name("classroomId"),
						field("date", "예약 날짜"),
						field("startTime", "예약 시작 시간"),
						field("endTime", "예약 종료 시간")),
					reservationCreateResponses()))
				.addPathItem("/api/v1/classrooms", get("Reserve", "특정 날짜/층의 전체 강의실 조회",
					"특정 날짜와 층의 예약 가능/불가 강의실 목록을 조회한다.",
					List.of(queryParameter("floor", "층"), queryParameter("date", "조회 날짜")),
					arrayResponse("ClassroomAvailabilityList", classroomAvailability())))
				.addPathItem("/api/v1/user/reservations", securedGet("Reserve", "사용자별 예약 정보",
					"내 예약 정보를 예약 날짜 내림차순으로 조회한다.",
					List.of(reservationStatusQueryParameter()),
					arrayResponse("UserReservationList", reservationSchema())))
				.addPathItem("/api/v1/reservations/{reservationId}", securedDelete("Reserve", "예약 취소",
					"예약 식별자로 내 예약을 취소한다.",
					List.of(pathParameter("reservationId", "예약 식별자"))))
				.addPathItem("/api/v1/user", new PathItem()
					.get(securedOperation("User", "내 정보 조회", "내 학번, 이름, 전화번호를 조회한다.")
						.responses(userInfoResponses()))
					.delete(securedOperation("User", "회원 탈퇴", "내 계정을 탈퇴 처리한다.")
						.responses(noContent())))
				.addPathItem("/api/v1/user/password", securedPatch("User", "내 비밀번호 수정", "기존 비밀번호 확인 후 새 비밀번호로 수정한다.",
					object("PasswordUpdateRequest", field("currentPassword", "기존 비밀번호"), field("newPassword", "새 비밀번호"))))
				.addPathItem("/api/v1/user/phone", securedPatchWithResponses("User", "내 전화번호 수정", "내 전화번호를 새 전화번호로 수정한다.",
					object("PhoneUpdateRequest", field("phoneNumber", "새 전화번호")),
					phoneUpdateResponses())));
	}

	private static PathItem post(String tag, String summary, String description, Schema<?> request, Schema<?> response) {
		return new PathItem().post(new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.requestBody(jsonRequest(request))
			.responses(ok(response.getName(), response)));
	}

	private static PathItem postWithResponses(String tag, String summary, String description, Schema<?> request,
		ApiResponses responses) {
		return new PathItem().post(new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.requestBody(jsonRequest(request))
			.responses(responses));
	}

	private static PathItem post(String tag, String summary, String description, Schema<?> response) {
		return new PathItem().post(new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.responses(ok(response.getName(), response)));
	}

	private static PathItem securedPostWithResponses(String tag, String summary, String description, Schema<?> request,
		ApiResponses responses) {
		return new PathItem().post(securedOperation(tag, summary, description)
			.requestBody(jsonRequest(request))
			.responses(responses));
	}

	private static PathItem securedGet(String tag, String summary, String description, List<Parameter> parameters,
		Schema<?> response) {
		Operation operation = securedOperation(tag, summary, description)
			.responses(ok(response.getName(), response));
		parameters.forEach(operation::addParametersItem);
		return new PathItem().get(operation);
	}

	private static PathItem get(String tag, String summary, String description, List<Parameter> parameters,
		Schema<?> response) {
		Operation operation = new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.responses(ok(response.getName(), response));
		parameters.forEach(operation::addParametersItem);
		return new PathItem().get(operation);
	}

	private static PathItem securedDelete(String tag, String summary, String description, List<Parameter> parameters) {
		Operation operation = securedOperation(tag, summary, description)
			.responses(noContent());
		parameters.forEach(operation::addParametersItem);
		return new PathItem().delete(operation);
	}

	private static PathItem securedPatch(String tag, String summary, String description, Schema<?> request) {
		return new PathItem().patch(securedOperation(tag, summary, description)
			.requestBody(jsonRequest(request))
			.responses(ok("MessageResponse", messageResponse("MessageResponse", "수정 성공 여부"))));
	}

	private static PathItem securedPatchWithResponses(String tag, String summary, String description, Schema<?> request,
		ApiResponses responses) {
		return new PathItem().patch(securedOperation(tag, summary, description)
			.requestBody(jsonRequest(request))
			.responses(responses));
	}

	private static Operation securedOperation(String tag, String summary, String description) {
		return new Operation()
			.addTagsItem(tag)
			.summary(summary)
			.description(description)
			.addSecurityItem(new SecurityRequirement().addList("accessTokenCookie"));
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

	private static ApiResponses noContent() {
		return new ApiResponses()
			.addApiResponse("204", new ApiResponse().description("No Content"))
			.addApiResponse("401", new ApiResponse().description("인증 필요"))
			.addApiResponse("404", new ApiResponse().description("대상을 찾을 수 없음"));
	}

	private static ApiResponses signupResponses() {
		return new ApiResponses()
			.addApiResponse("201", new ApiResponse()
				.description("Created")
				.content(jsonContent(messageResponse("SignupResponse", "회원가입 성공 여부"))))
			.addApiResponse("400", new ApiResponse().description("필수 입력값 누락"))
			.addApiResponse("401", new ApiResponse().description("KLAS 인증 실패"))
			.addApiResponse("409", new ApiResponse().description("이미 가입된 사용자"))
			.addApiResponse("422", new ApiResponse().description("입력값 형식 오류"));
	}

	private static ApiResponses passwordResetResponses() {
		return ok("PasswordResetResponse", messageResponse("PasswordResetResponse", "비밀번호 재설정 성공 여부"))
			.addApiResponse("404", new ApiResponse().description("사용자를 찾을 수 없음"));
	}

	private static ApiResponses reservationCreateResponses() {
		return ok("ReservationResponse", reservationResponse())
			.addApiResponse("409", new ApiResponse().description("이미 예약된 시간대"));
	}

	private static ApiResponses userInfoResponses() {
		return new ApiResponses()
			.addApiResponse("200", new ApiResponse()
				.description("OK")
				.content(jsonContent(userInfoResponse())))
			.addApiResponse("401", new ApiResponse().description("인증 필요"))
			.addApiResponse("404", new ApiResponse().description("사용자를 찾을 수 없음"));
	}

	private static ApiResponses phoneUpdateResponses() {
		return ok("PhoneUpdateResponse", phoneUpdateResponse());
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

	private static Parameter pathParameter(String name, String description) {
		return new Parameter().in("path").required(true).name(name).description(description).schema(new StringSchema());
	}

	private static Parameter queryParameter(String name, String description) {
		return new Parameter().in("query").required(true).name(name).description(description).schema(new StringSchema());
	}

	private static Parameter reservationStatusQueryParameter() {
		return new Parameter()
			.in("query")
			.required(false)
			.name("status")
			.description("예약 상태 필터")
			.schema(reservationStatusSchema());
	}

	private static Schema<?> messageResponse(String name, String message) {
		return object(name,
			new BooleanSchema().name("success").description("성공 여부"),
			new StringSchema().name("message").description(message));
	}

	private static Schema<?> reservationResponse() {
		return object("ReservationResponse",
			new StringSchema().name("reservationId").description("예약 식별자"),
			new StringSchema().name("date").description("예약 날짜"),
			new StringSchema().name("classroomNumber").description("강의실 번호"),
			new StringSchema().name("startTime").description("예약 시작 시간"),
			new StringSchema().name("endTime").description("예약 종료 시간"),
			reservationStatusSchema().name("status"));
	}

	private static Schema<?> reservationSchema() {
		return object("UserReservationResponse",
			new StringSchema().name("reservationId").description("예약 식별자"),
			new StringSchema().name("date").description("예약 날짜"),
			new StringSchema().name("classroom").description("강의실 표시명"),
			new StringSchema().name("reserverName").description("예약자 정보"),
			new StringSchema().name("startTime").description("예약 시작 시간"),
			new StringSchema().name("endTime").description("예약 종료 시간"),
			reservationStatusSchema().name("status"));
	}

	private static Schema<?> reservationStatusSchema() {
		StringSchema schema = new StringSchema();
		schema.description("예약 상태");
		schema.setEnum(List.of("RESERVED", "CANCELED"));
		return schema;
	}

	private static Schema<?> classroomTimeAvailability() {
		return object("ClassroomTimeAvailabilityResponse",
			new StringSchema().name("time").description("시간대").example("09:00~10:30"),
			new BooleanSchema().name("available").description("예약 가능 여부"));
	}

	private static Schema<?> classroomAvailability() {
		return object("ClassroomAvailabilityResponse",
			classroomIdSchema().name("classroomId"),
			new IntegerSchema().name("floor").description("층"),
			new StringSchema().name("classroomNumber").description("강의실 번호"),
			new BooleanSchema().name("available").description("강의실 단위 예약 가능 여부(슬롯 1개 이상 가용 시 true)"),
			classroomTimes());
	}

	private static Schema<?> classroomTimes() {
		ArraySchema times = new ArraySchema();
		times.name("times");
		times.description("슬롯별 예약 가능 여부");
		times.items(classroomTimeAvailability());
		return times;
	}

	private static StringSchema classroomIdSchema() {
		return (StringSchema)new StringSchema()
			.description("강의실 식별자. 현재는 saebit-{호실} 형식이며, 추후 {건물명}-{호실} 형식으로 확장한다.")
			.example("saebit-101");
	}

	private static Schema<?> userInfoResponse() {
		return object("UserInfoResponse",
			new StringSchema().name("name").description("이름"),
			new StringSchema().name("klasId").description("학번"),
			new StringSchema().name("phoneNumber").description("전화번호"),
			new StringSchema().name("message").description("내 정보 조회 결과 메시지"));
	}

	private static Schema<?> phoneUpdateResponse() {
		return object("PhoneUpdateResponse",
			new StringSchema().name("phoneNumber").description("수정된 전화번호"),
			new StringSchema().name("message").description("전화번호 수정 결과 메시지"));
	}

	private static Schema<?> arrayResponse(String name, Schema<?> itemSchema) {
		ArraySchema schema = new ArraySchema();
		schema.name(name);
		schema.items(itemSchema);
		return schema;
	}
}
