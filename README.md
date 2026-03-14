# Spring Boot and React Project Setup

This document outlines the steps to create a sample Java Spring Boot project with a ReactJS front end.

## Quick Start (Windows)

Use the provided helper scripts to start/stop the app and the required Docker services:

* `start.bat` – start the Spring Boot app (Redis cache enabled by default)
* `stop.bat` – stop the Spring Boot app (kills the process listening on port 8080)
* `start_docker.bat` – start Redis + Prometheus + Zipkin
* `stop_docker.bat` – stop the above services

## Phase 1: Backend (Spring Boot)

1.  **Initialize a Spring Boot application:**
    *   Go to [https://start.spring.io/](https://start.spring.io/).
    *   Select `Maven` or `Gradle` as the project type.
    *   Choose `Java` as the language.
    *   Select a Spring Boot version (e.g., 3.2.3).
    *   Fill in the project metadata (Group, Artifact, Name, Description, Package name).
    *   Choose `Jar` as the packaging.
    *   Select a Java version (e.g., 17).
    *   Add the `Spring Web` dependency.
    *   Click `GENERATE` to download the project.
    *   Unzip the downloaded file.

2.  **Create a simple REST controller:**
    *   In your Spring Boot project, create a new Java class (e.g., `HelloController`) in the same package as your main application class.
    *   Add the `@RestController` annotation to the class.
    *   Create a method that returns a simple message and annotate it with `@GetMapping("/api/hello")`.

    ```java
    package com.example.gemini_demo;

    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class HelloController {

        @GetMapping("/api/hello")
        public String hello() {
            return "Hello from Spring Boot!";
        }
    }
    ```

## Phase 2: Frontend (React)

1.  **Initialize a React application:**
    *   Open a terminal or command prompt.
    *   Navigate to the root directory of your Spring Boot project.
    *   Run the following command to create a new React app (e.g., named `frontend`):

    ```bash
    npx create-react-app frontend
    ```

2.  **Create a component to fetch data:**
    *   In the `frontend/src` directory, open `App.js`.
    *   Use the `useEffect` and `useState` hooks to fetch data from the Spring Boot backend when the component mounts.

    ```jsx
    import React, { useState, useEffect } from 'react';
    import './App.css';

    function App() {
      const [message, setMessage] = useState('');

      useEffect(() => {
        fetch('/api/hello')
          .then(response => response.text())
          .then(message => {
            setMessage(message);
          });
      }, []);

      return (
        <div className="App">
          <header className="App-header">
            <p>
              {message}
            </p>
          </header>
        </div>
      );
    }

    export default App;
    ```

## Phase 3: Integration

1.  **Configure proxy for API requests:**
    *   In the `frontend` directory, open the `package.json` file.
    *   Add the following line to the `package.json` file to proxy API requests to the Spring Boot backend (which runs on port 8080 by default):

    ```json
    "proxy": "http://localhost:8080"
    ```

2.  **Build the React app and serve it from Spring Boot:**
    *   In the `frontend` directory, run the following command to build the React app for production:

    ```bash
    npm run build
    ```
    *   **Automation**: To automate running `npm` and copying the files during the Maven build, add the `frontend-maven-plugin` and `maven-resources-plugin` to your `pom.xml`.

    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>1.15.0</version>
                <configuration>
                    <workingDirectory>frontend</workingDirectory>
                    <installDirectory>target</installDirectory>
                </configuration>
                <executions>
                    <execution>
                        <id>install node and npm</id>
                        <goals>
                            <goal>install-node-and-npm</goal>
                        </goals>
                        <configuration>
                            <nodeVersion>v20.11.0</nodeVersion>
                            <npmVersion>10.2.4</npmVersion>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm install</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>install</arguments>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm run build</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>run build</arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>copy-react</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>frontend/build</directory>
                                    <filtering>false</filtering>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ```

## Phase 4: Dockerization (Optional)

1.  **Create a `Dockerfile`:**
    *   Create a file named `Dockerfile` in the root of your project.
    *   This `Dockerfile` will use a multi-stage build to first build the React app, then build the Spring Boot app, and finally create a small image with the compiled application.

    ```dockerfile
    # Stage 1: Build React app
    FROM node:18 AS build-react
    WORKDIR /app
    COPY frontend/package.json frontend/package-lock.json ./
    RUN npm install
    COPY frontend/ ./
    RUN npm run build

    # Stage 2: Build Spring Boot app
    FROM maven:3.8.5-openjdk-17 AS build-spring
    WORKDIR /app
    COPY pom.xml .
    COPY src ./src
    COPY --from=build-react /app/build ./src/main/resources/static
    RUN mvn clean package -DskipTests

    # Stage 3: Create final image
    FROM openjdk:17-jdk-slim
    WORKDIR /app
    COPY --from=build-spring /app/target/*.jar app.jar
    ENTRYPOINT ["java","-jar","app.jar"]
    ```

## Running the Application

### Development (Separated)

In this mode, you run the backend and frontend separately. This enables hot-reloading for React.

1.  **Start Backend**:
    *   Run Spring Boot. You can skip the frontend build steps to speed up startup:
        ```bash
        mvn spring-boot:run -Dskip.frontend
        ```
    *   On Windows, you can use the provided helper script which also enables Redis caching:
        ```bat
        start.bat
        ```
    *   The backend runs on `http://localhost:8080`.
2.  **Stop Backend (Windows)**:
    *   Run:
        ```bat
        stop.bat
        ```
3.  **Start Frontend**:
    *   Open a new terminal in the `frontend` directory.
    *   Run `npm start`.
    *   Access the application at `http://localhost:3000`. (API requests are proxied to port 8080).

### Production (Integrated / Single Server)

1.  Package the application (this will automatically install Node, build React, and create the JAR):
    ```bash
    mvn clean package
    ```
3.  Run the application:
    ```bash
    java -jar target/*.jar
    ```
    Access the application at `http://localhost:8080`.

## How `mvn spring-boot:run` works with the Frontend

By default, `mvn spring-boot:run` **does not** run the React development server (which supports hot reloading). Instead, it serves the frontend as **static assets**.

*   **Single Server**: Only the Spring Boot application runs (on port 8080). It acts as both the API server and the web server for the React static files.
1.  **Mechanism**: Spring Boot serves files found in `classpath:/static` (mapped to `target/classes/static` during the build).
2.  **Automation**: The `frontend-maven-plugin` runs `npm install` and `npm run build` during the `generate-resources` phase.
3.  **Copying**: The `maven-resources-plugin` copies the resulting `frontend/build` directory to `target/classes/static` during the `process-resources` phase.

**Result**: Running `mvn spring-boot:run` will now perform a full build of the frontend before starting the server.

## Step 7: Caching Strategies

We added Spring Cache with Caffeine to speed up repeated reads from the database.

### New backend endpoints
* `/api/cached-db-hello` - returns a cached greeting.
* `/api/cached-users` - returns cached user list.
* `/api/cache-clear` - clears all application caches.

### Frontend updates
In `frontend/src/App.js`, we added explicit comparison UI:
* `Fetch from DB` (no cache)
* `Fetch Cached Greeting` (local cache / Caffeine)
* `Fetch Hybrid Cached Greeting` (L1 Caffeine + L2 Redis, when enabled)
* `Clear Cache`
This allows direct side-by-side comparison of no cache, local cache, and hybrid cache behavior.

---

## Continuing the Journey with Gemini Code Assist

To continue working on the "Senior Dev Journey" in a new chat session, use the following prompt as your first message to provide the necessary context. Remember to update the step number.

```markdown
We are continuing the 'Senior Dev Journey' for this project.

**Current Status:** We have completed Step X. The next step is **Step Y: [Topic Name]**.

Please be aware of these two files:
1.  `SENIOR_DEV_JOURNEY.md` (The roadmap)
2.  `SENIOR_DEV_JOURNEY_DETAIL.md` (The implementation guide)

When we complete the next step, please update both files accordingly.

Additionally , DON'T break the previous implementation steps.

Let's begin implementing Step Y.
```


## Commit Message Conventions

To keep the project history clean, we follow the Conventional Commits standard.

### Common Prefixes

*   **`feat`**: New feature.
*   **`fix`**: Bug fix.
*   **`docs`**: Documentation.
*   **`test`**: Tests.
*   **`refactor`**: Code change (no feature/fix).
*   **`chore`**: Build/Auxiliary tools.

### "Senior Dev Journey" Scope

Use a scope to indicate the step number from `SENIOR_DEV_JOURNEY.md`.

**Format**: `type(scope): message`

**Example**:
```bash
git commit -m "feat(step-4): Add Testcontainers"
