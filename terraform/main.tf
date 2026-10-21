terraform {
  cloud {
    organization = "nopasseu"
    workspaces {
      name = "nopasseu-staging"
    }
  }
  required_providers {
    # 1. Define a `HCLOUD_TOKEN` sensitive env variable in HCP with a Hetzner API Token
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
  }
}

variable "staging_ssh_pub_local" {
  description = "Public Ed25519 key for the user from local"
  type        = string
  sensitive   = true
}

variable "staging_ssh_pub_github" {
  description = "Public Ed25519 key for the user from Github Actions"
  type        = string
  sensitive   = true
}

# 2. Provision the ARM64 CAX11 Server
resource "hcloud_server" "staging_server" {
  name        = "nopasseu-staging-tf"
  server_type = "cax11"
  image       = "ubuntu-26.04"
  location    = "hel1" # Falkenstein. Alternatively: fsn1 or nbg1
  # Hetzner will inject this file into the VM on its first boot
  user_data = templatefile("${path.module}/cloud-init.yml.tftpl", {
    user_ssh_pub_local  = var.staging_ssh_pub_local,
    user_ssh_pub_github = var.staging_ssh_pub_github
  })
}

# 3. Output the IP address so you can give it to Ansible
output "server_ip" {
  value = hcloud_server.staging_server.ipv4_address
}
