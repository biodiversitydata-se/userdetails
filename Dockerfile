FROM livingatlases/java-11-base:latest
# Args and Envs
ARG APP_ARTIFACT=userdetails
ARG USER=userdetails
ARG APP_VERSION=3.0.2
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx1g -Xms256m -XX:+UseConcMarkSweepGC -Dlog4j2.formatMsgNoLookups=true"
ENV LOGGING_CONFIG=/data/userdetails/config/logback.xml
ENV LOG_DIR=/var/log/atlas/userdetails
# Directories and perms
RUN mkdir -p /data/$APP_ARTIFACT $LOG_DIR && \
    groupadd -r $USER -g 1000 && useradd -r -g $USER -u 1000 -m $USER && \
    chown -R $USER:$USER /data/$APP_ARTIFACT $LOG_DIR
WORKDIR /opt/atlas/$APP_ARTIFACT
# war
ADD https://nexus.ala.org.au/service/local/repositories/releases/content/au/org/ala/${APP_ARTIFACT}/${APP_VERSION}/${APP_ARTIFACT}-${APP_VERSION}-exec.war app.war
RUN chown -R $USER:$USER /opt/atlas/$APP_ARTIFACT
USER $USER
EXPOSE 9001
# Lint with:
# docker run --rm -i hadolint/hadolint < Dockerfile
# Wait for mysql and cas (that inits the emmet db currently)
# hadolint ignore=DL3025
ENTRYPOINT dockerize -wait tcp://mysql:3306 -wait tcp://cas:9000 -timeout 120s sh -c "java $JAVA_OPTS -jar app.war"
