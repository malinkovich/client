FROM tomcat:9.0.72
#FROM tomcat:9.0.87
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/client.war /usr/local/tomcat/webapps/ROOT.war
## Указываем использовать существующую сеть "my_network" при запуске контейнера
#ENV DOCKER_network my_network