FROM openjdk:11-jre-slim
ENV APP_ARTIFACT=userdetails
ENV APP_VERSION=3.0.2
WORKDIR /opt
ADD https://nexus.ala.org.au/service/local/repositories/releases/content/au/org/ala/${APP_ARTIFACT}/${APP_VERSION}/${APP_ARTIFACT}-${APP_VERSION}-exec.war app.war
RUN mkdir /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
