# Schema Architecture

## Section 1: Architecture Summary

This Spring Boot application follows a layered architecture using both MVC and REST controllers. Thymeleaf templates are used for the Admin and Doctor dashboards, while REST APIs handle requests for the other modules. The application uses MySQL for storing patient, doctor, appointment, and admin-related data, and MongoDB for storing prescription information.

Requests from the controllers are passed to a common service layer, which contains the application's business logic. The service layer communicates with the appropriate repositories to access or modify data. MySQL data is managed using JPA entities and repositories, while MongoDB data is handled using document models and MongoDB repositories.

## Section 2: Numbered Flow of Data and Control

1. The user accesses an Admin, Doctor, or other application feature through the web interface or API.
2. The request is routed to the appropriate Thymeleaf MVC controller or REST controller.
3. The controller receives the request and passes the required operation to the service layer.
4. The service layer applies the required business logic and determines which data source needs to be accessed.
5. The service layer calls the appropriate repository to read or update the required data.
6. The repository communicates with MySQL using JPA entities or with MongoDB using document models, depending on the type of data.
7. The result is returned through the service and controller back to the user, either as a Thymeleaf page for dashboard requests or as a REST API response for API requests.
