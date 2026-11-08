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
npm run dev
```

Open the URL printed by Parcel, usually `http://localhost:1234`.

`npm run dev` runs:

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

## Bootstrap a staging server

The Terraform configuration in `terraform/` provisions an Ubuntu server on Hetzner. The GitHub Actions workflow runs Ansible to configure the server by installing Docker, Nginx, Certbot, and Infisical, then builds and deploys the app. You don't need to install or run Ansible on your computer.

You need Terraform, access to the remote Terraform organization and workspace configured in `terraform/main.tf`, a Hetzner API token, and control of the staging domain configured in `ansible/vars.yml`.

### Step 1. Prepare deployment credentials

Authenticate with the remote Terraform service if you haven't already:

```bash
terraform login
```

Generate separate keys for local administration and GitHub Actions. Create the `.ssh` directory first if it doesn't exist:

```bash
mkdir -p ~/.ssh
ssh-keygen -t ed25519 -C "rbstaging@local" -f ~/.ssh/id_reviewboard-staging-local
ssh-keygen -t ed25519 -C "rbstaging@github" -f ~/.ssh/id_reviewboard-staging-github -N ""
```

Keep `HCLOUD_TOKEN` configured as a sensitive environment variable in the remote Terraform workspace named `nopasseu-staging`. The two SSH key inputs are Terraform variables, so provide them in your local shell instead of saving them in the workspace:

```bash
export TF_VAR_staging_ssh_pub_local="$(cat ~/.ssh/id_reviewboard-staging-local.pub)"
export TF_VAR_staging_ssh_pub_github="$(cat ~/.ssh/id_reviewboard-staging-github.pub)"
```

These values are public keys, not private secrets. Terraform reads the `TF_VAR_` variables when you run it and supplies them to the remote run without saving them as workspace variables. The Terraform configuration marks them as sensitive to suppress them in command-line output. Keep the corresponding private keys secure and don't add them to the repository. After provisioning, clear the local variables:

```bash
unset TF_VAR_staging_ssh_pub_local TF_VAR_staging_ssh_pub_github
```

### Step 2. Provision the server

Initialize and apply the configuration from the `terraform/` directory:

```bash
cd terraform
terraform init
terraform apply
terraform output -raw server_ip
cd ..
```

Save the IP address printed by `terraform output`. Use it in the next steps.

### Step 3. Point the staging domain to the server

Create or update the staging domain's A record to point to the server IP. Wait for the Domain Name System (DNS) record to resolve:

```bash
dig +short reviewboard-staging.nopass.eu
```

The returned address should match the server IP. Ansible requests a Transport Layer Security (TLS) certificate. DNS must resolve correctly before GitHub Actions runs the playbook.

### Step 4. Set the Ansible target

The workflow doesn't use a checked-in inventory file. It passes the staging host directly to Ansible using the `SERVER_HOST` environment variable and the inline-host inventory syntax, which uses a trailing comma: `-i "${SERVER_HOST},"`. The playbook targets all hosts in that inline inventory. GitHub Actions loads `SERVER_SSH_KEY` into its SSH agent before running Ansible.

To run the playbook locally, set `SERVER_HOST` to the staging IP address or domain and specify your local private key:

```bash
export SERVER_HOST="your-staging-ip-or-domain"
cd ansible
ansible-playbook -i "$SERVER_HOST," playbook.yml -u holu -e "ansible_port=2222" -e "env=staging" --private-key ~/.ssh/id_reviewboard-staging-local
cd ..
```

When the workflow runs the playbook, Ansible installs Docker, Docker Compose, the Infisical command-line tool, Nginx, and Certbot. It also configures the site, firewall, and fail2ban, then requests a TLS certificate. If Certbot can't validate the domain because DNS hasn't propagated, wait for DNS to resolve and rerun the deployment.

### Step 5. Configure the deployment environment and credentials

In the repository, open **Settings > Environments**, select **New environment**, and create an environment named `staging`. Add the variables and secrets below to it. The deployment job uses this environment.

| Type | Name | Value |
| --- | --- | --- |
| Variable | `SERVER_HOST` | The staging domain or server IP. The workflow uses it as the Ansible target and upload destination. |
| Variable | `INFISICAL_API_URL` | Your Infisical API URL |
| Secret | `SERVER_USER` | `holu` |
| Secret | `SERVER_SSH_KEY` | Contents of `~/.ssh/id_reviewboard-staging-github`, the private key |
| Secret | `INFISICAL_CLIENT_ID` | Universal Auth client ID |
| Secret | `INFISICAL_CLIENT_SECRET` | Universal Auth client secret |
| Secret | `INFISICAL_PROJECT_ID` | Infisical project ID |

In Infisical, make sure the staging environment has all required variables from `docker-compose.yml`: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DATABASE_JDBC_URL`, `DATABASE_USER`, `DATABASE_PASS`, `JWT_SECRET`, `STRIPE_SECRET_KEY`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL`, `STRIPE_INVITES_WEBHOOK_SECRET`, `EMAIL_USER`, `EMAIL_PASSWORD`, `EMAIL_BASE_URL`, and `OPENAI_SECRET_KEY`. `JWT_TTL`, `EMAIL_HOST`, and `EMAIL_PORT` are optional. Configure the Universal Auth identity with access to the project and staging environment.

To move deployments to a different server, update `SERVER_HOST` in the `staging` environment. You don't need to change or commit an inventory file. The new value takes effect the next time the workflow runs. Changing the variable alone doesn't start a deployment.

### Step 6. Run the staging deployment

For the first deployment, open **Actions**, select **Scala CI**, choose **Run workflow**, select the `main` branch, and start the workflow. The workflow runs the tests first, then builds the backend and frontend, runs Ansible to configure the server, uploads the app files, and starts Docker Compose. Check that the run succeeds, then open the staging domain to verify the deployment.

All pushes to `main` run the tests and then deploy to staging if they pass.
