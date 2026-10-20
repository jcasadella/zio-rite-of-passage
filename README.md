# ZIO Rite of Passage

[![Scala CI](https://github.com/jcasadella/zio-rite-of-passage/actions/workflows/scala-ci.yaml/badge.svg?branch=main)](https://github.com/jcasadella/zio-rite-of-passage/actions/workflows/scala-ci.yaml)

Review Board app built with Scala 3, ZIO, Scala.js, Laminar, Parcel, and PostgreSQL.

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
docker-compose up
```

The database listens on `localhost:5432` and uses the credentials configured in `docker-compose.yml`:

```text
user: docker
password: docker
```

To stop the database:

```bash
docker-compose down
```

## Forward Stripe webhooks in local environment

After installing and authenticating the Stripe command-line tool, run this from a separate
terminal and leave it running while developing:

```sh
stripe listen --forward-to localhost:4041/invites/webhook
```

The tool prints a webhook signing secret beginning with `whsec_`. Set that value
as `STRIPE_INVITES_WEBHOOK_SECRET` in the environment used to start the backend.

## Run the backend server

Start PostgreSQL first, then from the repository root set the required Stripe
and OpenAI credentials in your terminal session and run the server:

```bash
export STRIPE_SECRET_KEY="sk_test_..."
export STRIPE_INVITES_WEBHOOK_SECRET="whsec_..."
export OPENAI_SECRET_KEY="..."
sbt "server / runMain com.rockthejvm.reviewboard.Application"
```

Replace the example values with your Stripe secret key and the signing secret
for the invites webhook. Keep the values in your local environment rather than
committing them to the repository. The backend listens on `http://localhost:4041`.

## Test the project

Run all Scala tests from the repository root:

```bash
sbt test
```

GitHub Actions runs the same command. Start the database container before running tests that need PostgreSQL. Tests also require the environment variables listed in the backend setup section.

## Build the Scala.js frontend

Run the development linker from the repository root:

```bash
sbt "app / Compile / fastLinkJS"
```

For an optimized production build, use:

```bash
sbt "app / Compile / fullLinkJS"
```

The build writes production files to `modules/app/target/scala-3.5.0/`, including `app-opt.js`.

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
```

```bash
cd modules/app
npm run build-pro
```

These scripts expect the corresponding `build/index.html` entry point and write to `dist-staging` or `dist-pro`.
