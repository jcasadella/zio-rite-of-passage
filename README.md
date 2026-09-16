# ZIO Rite of Passage

Review Board application built with Scala 3, ZIO, Scala.js, Laminar, Parcel, and PostgreSQL.

Project created from the [Rock the JVM](https://rockthejvm.com/) course of the same name.

## Prerequisites

- JDK 21
- sbt 1.12.13 or newer
- Node.js and npm
- Docker Desktop, for the PostgreSQL database

## Install frontend dependencies

From the repository root:

```bash
cd modules/app
npm install
cd ../..
```

## Start the database

From the repository root:

```bash
docker compose up -d db
```

The database listens on `localhost:5432` and uses the credentials configured in `docker-compose.yml`:

```text
user: docker
password: docker
```

To stop the database:

```bash
docker compose down
```

## Test the project

Run all Scala tests from the repository root:

```bash
sbt test
```

The same command is used by the GitHub Actions workflow. Tests that require PostgreSQL should be run with the database container started.

## Build the Scala.js frontend

Run the development linker from the repository root:

```bash
sbt "app / Compile / fastLinkJS"
```

`fastLinkJS` is the current Scala.js name for the fast development build. In this project, the equivalent legacy command is:

```bash
sbt "app / Compile / fastOptJS"
```

Both commands produce the frontend JavaScript under `modules/app/target/scala-3.5.0/`, including `app-fastopt.js`, which is referenced by `modules/app/index.html`.

For an optimized production build, use:

```bash
sbt "app / Compile / fullLinkJS"
```

The equivalent legacy command is:

```bash
sbt "app / Compile / fullOptJS"
```

The production output is generated under `modules/app/target/scala-3.5.0/`, including `app-opt.js`.

## Run the frontend in development

First generate the fast Scala.js output, then start Parcel from the app directory:

```bash
sbt "app / Compile / fastLinkJS"
cd modules/app
npm run start
```

Open the URL printed by Parcel, usually `http://localhost:1234`.

`npm run start` runs:

```bash
parcel index.html --no-cache --dist-dir dist --log-level info
```

Parcel watches the HTML, CSS, and JavaScript files and rebuilds while you develop. Stop it with `Ctrl+C`.

## Build frontend assets with Parcel

The available npm production/staging scripts are:

```bash
cd modules/app
npm run build-staging
npm run build-prod
```

These scripts expect the corresponding `build/index.html` entry point and write to `dist-staging` or `dist-prod`.
