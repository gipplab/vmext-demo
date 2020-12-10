FROM openjdk:11
EXPOSE 8080
COPY /target/mathpipeline.jar mathpipeline.jar
COPY application.yaml application.yaml
COPY lacast.config.yaml lacast.config.yaml
COPY sampleHarvest.xml basex/sampleHarvest.xml
ENTRYPOINT ["java","-jar","mathpipeline.jar"]
