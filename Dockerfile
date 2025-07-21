FROM docker-stmj-tnt-services-virtual.rt.artifactory.tio.systems/certs:latest AS certs                                                                   
FROM maven:3.9-eclipse-temurin-21 AS builder                                                                                                             

COPY --from=certs /certs/ /certs                                                                                                                         
COPY --from=certs /certs/ZscalerRootCertificate-2048-SHA256.crt "$JAVA_HOME/lib/security/ZscalerRootCertificate-2048-SHA256.crt"                         
COPY --from=certs /certs/ZscalerRootCertificate-2048-SHA256.crt /usr/local/share/ca-certificates/ZscalerRootCertificate-2048-SHA256.crt                  
RUN ls /certs && \                                                                                                                                       
    cd "$JAVA_HOME/lib/security" && \                                                                                                                    
    keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias ldapcert -file ZscalerRootCertificate-2048-SHA256.crt && \  
    apt-get update && apt-get install -y --no-install-recommends ca-certificates && \                                                                    
    update-ca-certificates && \                                                                                                                          
    rm -rf /var/lib/apt/lists/* /certs                                                                                                                   

WORKDIR /app                                                                                                                                             

RUN --mount=type=bind,source=pom.xml,target=/app/pom.xml,readonly mvn dependency:go-offline -B                                                           

COPY src ./src   
COPY pom.xml .                                                                                                                                        
RUN mvn clean package -DskipTests -e                                                                                                                    

FROM eclipse-temurin:21-jre                                                                                                                              

COPY --from=certs /certs/ /certs                                                                                                                         
COPY --from=certs /certs/ZscalerRootCertificate-2048-SHA256.crt "$JAVA_HOME/lib/security/ZscalerRootCertificate-2048-SHA256.crt"                         
COPY --from=certs /certs/ZscalerRootCertificate-2048-SHA256.crt /usr/local/share/ca-certificates/ZscalerRootCertificate-2048-SHA256.crt                  
RUN ls /certs && \                                                                                                                                       
    cd "$JAVA_HOME/lib/security" && \                                                                                                                    
    keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias ldapcert -file ZscalerRootCertificate-2048-SHA256.crt && \  
    apt-get update && apt-get install -y --no-install-recommends ca-certificates && \                                                                    
    update-ca-certificates && \                                                                                                                          
    rm -rf /var/lib/apt/lists/* /certs                                                                                                                   

RUN apt-get update && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core && \                                                       
    rm -rf /var/lib/apt/lists/*                                                                                                                          

RUN groupadd -r appuser && useradd -r -g appuser appuser                                                                                                 
WORKDIR /app                                                                                                                                             

COPY --from=builder /app/target/powerpoint-parser-*-jar-with-dependencies.jar /app/powerpoint-parser.jar                                                 

RUN mkdir -p /app/input /app/output && \                                                                                                                 
    chown -R appuser:appuser /app                                                                                                                        

USER appuser                                                                                                                                             

ENV INPUT_FILE=""                                                                                                                                        
ENV OUTPUT_DIR="/app/output"                                                                                                                             
ENV JAVA_OPTS=""                                                                                                                                         

COPY --chown=appuser:appuser ./entrypoint.sh /app/entrypoint.sh                                                                                          

VOLUME ["/app/input", "/app/output"]                                                                                                                     

ENTRYPOINT ["/app/entrypoint.sh"]                                                                                                                        
CMD []             