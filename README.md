# CeR API

The Centre for eResearch REST API.

To deploy the CeR API follow the instructions on the [Research Hub Deploy project](https://github.com/UoA-eResearch/research-hub-deploy) and Nuclino.

## Developing CeR API

To work on the Centre for eResearch REST API locally, follow the instructions on the [Research Hub Deploy project](https://github.com/UoA-eResearch/research-hub-deploy) on running the project locally.

The instructions will run a Spring Boot Devtools instance inside a Docker container that [hotswaps project classfiles](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-hotswapping.html) (stored under the ՝/target՝ directory). See the ՝Dockerfile՝ for more detail.

You can then modify source code of the project, compile (either by running the command ՝mvn compile՝ or by using an IDE), and watch the changes get loaded by Spring Boot Devtools.

The API is served on localhost:8081/cer-api/.
