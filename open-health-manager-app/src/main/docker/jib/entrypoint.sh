#!/bin/sh

echo "The application will start in ${JHIPSTER_SLEEP}s..." && sleep ${JHIPSTER_SLEEP}
exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app:/app/WEB-INF/classes:/app/WEB-INF/lib/* "org.mitre.healthmanager.OpenHealthManagerApp"  "$@"
