FROM java:8
EXPOSE 8080
ADD /target/mathpipeline.jar mathpipeline.jar
ENTRYPOINT ["java","-jar","mathpipeline.jar"]
