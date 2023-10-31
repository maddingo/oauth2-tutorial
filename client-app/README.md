# Client Application
The client app has a Spring boot backend and a Next.js frontend

## Develop the backend

```bash
mvn clean compile spring-boot:run
```

## Develop the frontend

```bash
mvn frontend:install-node-and-npm frontend:npm@npm-run-dev -Pdev
```

or simply

```bash
cd frontend; npm run dev
```

## Run the packaged application

```bash
mvn clean package ; java -jar target/client-app.jar
```
