FROM openjdk:17-jdk

# Arguments de build pour la version
ARG VERSION=5.0.1-SNAPSHOT
ARG JAR_FILE=tp-foyer-${VERSION}.jar

# Variables d'environnement
ENV VERSION=${VERSION}
ENV JAR_FILE=${JAR_FILE}

EXPOSE 8089

# Copier le JAR avec le nom dynamique
ADD target/${JAR_FILE} ${JAR_FILE}

# Point d'entrée avec le nom dynamique
ENTRYPOINT ["sh", "-c", "java -jar /${JAR_FILE}"]
