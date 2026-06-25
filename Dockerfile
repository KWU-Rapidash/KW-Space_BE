FROM eclipse-temurin:17-jre
RUN useradd --system --create-home --uid 10001 appuser

WORKDIR /app

COPY --chown=appuser:appuser build/libs/KW-SPACE-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
