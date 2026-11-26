# syntax=docker/dockerfile:1

# Imagen de ejecución (el JAR se construye antes con Maven o en GitHub Actions).
# Se usa Jammy para que los nombres de paquetes coincidan con el CI del proyecto.
FROM eclipse-temurin:25-jre-jammy

ENV TZ=America/Argentina/Buenos_Aires \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Djava.awt.headless=true --enable-native-access=ALL-UNNAMED"

# Librerías que necesitan OpenCV/OpenBLAS, fuentes para PDF y curl para el healthcheck.
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      fontconfig \
      fonts-dejavu-core \
      libfreetype6 \
      libgl1 \
      libglib2.0-0 \
      libgomp1 \
      libgtk2.0-0 \
      libsm6 \
      libxext6 \
      libxrender1 \
      tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 modalink \
 && useradd --system --uid 10001 --gid modalink --home-dir /app --shell /usr/sbin/nologin modalink \
 && mkdir -p /app/uploads \
 && chown -R modalink:modalink /app

WORKDIR /app

COPY --chown=modalink:modalink target/*.jar /app/app.jar

USER modalink

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=150s --retries=6 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
