FROM openjdk:11-jre-slim
ARG APP_ARTIFACT=userdetails
ARG APP_VERSION=3.0.2
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx1g -Xms256m -XX:+UseConcMarkSweepGC -Dlog4j2.formatMsgNoLookups=true"
ENV LOGGING_CONFIG=/data/userdetails/config/logback.xml
ENV LOG_DIR=/var/log/atlas/userdetails
WORKDIR /opt
ADD https://nexus.ala.org.au/service/local/repositories/releases/content/au/org/ala/${APP_ARTIFACT}/${APP_VERSION}/${APP_ARTIFACT}-${APP_VERSION}-exec.war app.war
RUN mkdir /data
RUN mkdir -p /var/log/atlas/userdetails
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.war"]
