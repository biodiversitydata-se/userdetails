FROM openjdk:11-jre-slim
ENV DOCKERIZE_VERSION v0.7.0
RUN apt-get update \
    && apt-get install -y wget \
    && wget -O - https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz | tar xzf - -C /usr/local/bin \
    && apt-get autoremove -yqq --purge wget && rm -rf /var/lib/apt/lists/*
ARG APP_ARTIFACT=userdetails
ARG APP_VERSION=3.0.2
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx1g -Xms256m -XX:+UseConcMarkSweepGC -Dlog4j2.formatMsgNoLookups=true"
ENV LOGGING_CONFIG=/data/userdetails/config/logback.xml
ENV LOG_DIR=/var/log/atlas/userdetails
WORKDIR /opt
ADD https://nexus.ala.org.au/service/local/repositories/releases/content/au/org/ala/${APP_ARTIFACT}/${APP_VERSION}/${APP_ARTIFACT}-${APP_VERSION}-exec.war app.war
RUN mkdir /data
RUN mkdir -p /var/log/atlas/userdetails
EXPOSE 9001
# Wait for mysql and cas (that inits the emmet db currently)
ENTRYPOINT dockerize -wait tcp://mysql:3306 -wait tcp://cas:9000 -timeout 120s sh -c "java $JAVA_OPTS -jar app.war"
