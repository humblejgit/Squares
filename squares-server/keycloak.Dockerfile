# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-jammy

ENV KC_DB=dev-file

ARG KEYCLOAK_VERSION=26.7.0
ARG KEYCLOAK_SHA256=f771df0aa1e4820f57d56f7d6d015beb6415487b43f8de7e5a6d48f8a7fe118a

ADD --checksum=sha256:${KEYCLOAK_SHA256} \
    https://github.com/keycloak/keycloak/releases/download/${KEYCLOAK_VERSION}/keycloak-${KEYCLOAK_VERSION}.tar.gz \
    /tmp/keycloak.tar.gz

RUN groupadd --system keycloak \
    && useradd --system --gid keycloak --home-dir /opt/keycloak --shell /usr/sbin/nologin keycloak \
    && mkdir -p /opt/keycloak \
    && tar -xzf /tmp/keycloak.tar.gz --strip-components=1 -C /opt/keycloak \
    && /opt/keycloak/bin/kc.sh build \
    && mkdir -p /opt/keycloak/data/import \
    && rm /tmp/keycloak.tar.gz \
    && chown -R keycloak:keycloak /opt/keycloak

WORKDIR /opt/keycloak
USER keycloak

EXPOSE 8080
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
