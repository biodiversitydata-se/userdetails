FROM livingatlases/java-11-base:1.0.0

# Lint with:
# docker run --rm -i hadolint/hadolint < Dockerfile

# Args
ARG APP_ARTIFACT=userdetails
ARG USER=userdetails
ARG WAR_FILE=./build/libs/userdetails-*-exec.war
ARG DOCKERIZE_ARGS="-wait tcp://auth-mysql:3306 -wait tcp://cas:9000 -timeout 120s"

# Env variables
ENV SERVER_PORT="9001"
ENV WAR_FILE_ENV=${WAR_FILE}
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx1g -Xms256m -XX:+UseConcMarkSweepGC -Dlog4j2.formatMsgNoLookups=true"
ENV LOGGING_CONFIG=/data/userdetails/config/logback.xml
ENV LOG_DIR=/var/log/atlas/userdetails
ENV DOCKERIZE_OPTS=${DOCKERIZE_ARGS}

# Directories and perms
RUN mkdir -p /data/$APP_ARTIFACT $LOG_DIR && \
    groupadd -r $USER -g 1000 && useradd -r -g $USER -u 1000 -m $USER && \
    chown -R $USER:$USER /data/$APP_ARTIFACT $LOG_DIR
WORKDIR /opt/atlas/$APP_ARTIFACT

# war
COPY $WAR_FILE_ENV app.war
RUN chown -R $USER:$USER /opt/atlas/$APP_ARTIFACT

USER $USER
EXPOSE $SERVER_PORT
# hadolint ignore=DL3025
ENTRYPOINT dockerize $DOCKERIZE_OPTS sh -c "java $JAVA_OPTS -jar app.war"
