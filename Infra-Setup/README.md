# Infra-Setup

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Prerequisites](#prerequisites)
4. [Tools Technologies Required](#tools-and-technologies-required)
5. [Infrastructure Setup](#infrastructure-setup)
   - [AWS EC2 Instances for Jenkins, SonarQube, and Nexus](#aws-ec2-instances-for-jenkins-sonarqube-and-nexus)
   - [EKS Cluster Setup](#eks-cluster-setup)
6. [Pipeline Setup](#pipeline-setup)

## Overview

The BankApp Infrastructure Setup `Infra-Setup` folder contains the deployment pipeline and infrastructure configurations to establish a CI/CD pipeline for the BankApp project. This setup includes Continuous Integration (CI) using Jenkins and Continuous Deployment (CD) through ArgoCD. With this setup, each code change is tested, analyzed, and deployed to ensure a robust application lifecycle.

## Features

- **[Jenkins-CI Folder in Main Branch](./Jenkins-CI)**: CI pipeline setup with Jenkins.
- **[Manifest Branch](https://github.com/Godfrey22152/Multi-Tier-GitOps-Project/tree/manifest)**: CD setup using ArgoCD.
- **[Monitoring Folder in Main Branch](./Monitoring)**: Monitoring setup with Prometheus and Grafana.


---
## Prerequisites
Before setting up the **Multi-Tier-GitOps-Project Infrastructure**, ensure you have a good knowledge on:
- AWS account setup with permissions to create EC2 instances and an EKS cluster. 
- Setting up a Kubernetes cluster either locally or on any cloud platform. (Minikube, k3s, or a managed service like EKS/GKE/AKS).
- [AWS CLI](https://aws.amazon.com/cli/) configured with IAM access for deploying to AWS.
- [Kubectl](https://kubernetes.io/docs/tasks/tools/) for interacting with the EKS cluster.
- [Helm](https://helm.sh/) for deploying Prometheus and Grafana.
- [Jenkins](https://www.jenkins.io/) for managing the CI pipeline.
- [ArgoCD](https://argo-cd.readthedocs.io/en/stable/) for CD management.
- [Terraform](https://www.terraform.io/) installed for EKS automation.
- A Docker Hub account or private Docker registry for image storage.
- [SonarQube](https://www.sonarqube.org/) and [Nexus](https://www.sonatype.com/products/repository-oss) instances.

## Tools and Technologies Required

- **Cloud Provider**: AWS (EC2 for Jenkins, SonarQube, Nexus, and EKS for Kubernetes cluster)
- **Containerization**: Docker
- **Terraform**: Infrastructure as code for automating EKS and EC2 setup.

### CI Tools: 
- **Jenkins**: Continuous integration and deployment.
- **SonarQube**: Static code analysis for quality and security checks.
- **Nexus**: Artifact repository for storing build artifacts.
- **Docker**: For containerizing the application.
- **Trivy**: Security scanner for file systems and container images.


### CD Tools: 
- **Kubernetes**: Orchestrates the containerized deployment of the Bankapp application.
- **ArgoCD**: Automates continuous delivery with declarative GitOps for Kubernetes.
- **Helm**: Kubernetes package manager

### Monitoring Tools: 
- **Prometheus**: Collects and manages metrics for the infrastructure and application.
- **Grafana**: Visualizes metrics and system health from Prometheus.


---
## Infrastructure Setup

### CI: AWS EC2 Instance for Jenkins, SonarQube, and Nexus

#### Manual Procedure for AWS EC2 Instance setup for Jenkins, SonarQube, and Nexus

##### First Option: Using AWS Management Console

Set up three AWS EC2 instances for Jenkins, SonarQube, and Nexus.
Launch 3 EC2 instances (Ubuntu) in AWS but ensure you add these user-data for each instance during creation to install necessary dependencies.

- **User Data for EC2 Instance dependency installation**:

To add the **User Data** script during the EC2 instance creation, follow these steps:

   - **Log in to AWS Management Console** and navigate to **EC2**.
   - Click on **Launch Instance**.
   - Choose the **Amazon Machine Image (AMI)** and **Instance Type**, then proceed to **Configure Instance Details**.
   - Scroll down to the **Advanced Details** section.
   - In the **User data** field, add your script or commands:
   
   - **For Jenkins:**
   ```bash  
    #!/bin/bash

    # Update the package list
    sudo apt-get update -y

    # Install Java
    sudo apt-get install -y openjdk-17-jre-headless

    # Jenkins installation process
    echo "Installing Jenkins package..."
    sudo wget -O /usr/share/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
    echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y jenkins

    # Docker installation
    echo "Installing Docker..."
    sudo apt-get install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    sudo chmod 666 /var/run/docker.sock

    sudo usermod -aG docker jenkins

    # Trivy installation
    echo "Installing Trivy..."
    wget https://github.com/aquasecurity/trivy/releases/download/v0.27.1/trivy_0.27.1_Linux-64bit.deb
    sudo dpkg -i trivy_0.27.1_Linux-64bit.deb
   ```

   - **For SonarQube:**
   ```bash
    #!/bin/bash
    sudo apt-get update

    ## Install Docker
    yes | sudo apt-get install docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    sudo chmod 666 /var/run/docker.sock
    echo "Waiting for 30 seconds before runing Sonarqube Docker container..."
    sleep 30

    ## Runing Sonarqube in a docker container
    docker run -d -p 9000:9000 --name sonarqube-container sonarqube:lts-community
   ```
   - **For Nexus:**
   ```bash
    #!/bin/bash
    sudo apt-get update

    ## Install Docker
    yes | sudo apt-get install docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    sudo chmod 666 /var/run/docker.sock
    echo "Waiting for 30 seconds before running Nexus Docker container..."
    sleep 30

    ## Runing Nexus in a docker container
    docker run -d -p 8081:8081 --name nexus-container sonatype/nexus3:latest
   ```
 - Continue with the remaining steps to configure **Storage**, **Security Groups**, and **Tags**, then launch the instance.


##### Second Option: Using AWS CLI to setup the EC2 Instances
You can also use the `AWS CLI` to setup each of the EC2 Instance:

   ```bash
    aws ec2 run-instances --image-id ami-12345678 --instance-type t2.medium --key-name your-key-pair \
    --security-group-ids sg-12345678 --subnet-id subnet-12345678 --user-data file://userdata.sh
   ```

- **NOTE**:
 - `--image-id ami-12345678`: Specifies the Amazon Machine Image (AMI) ID to use for the instance. This ID represents the OS and software configuration of the instance (e.g., Ubuntu, Amazon Linux).

 - `--instance-type t2.medium`: Specifies the type of instance to create, which determines its CPU, memory, and network performance. `t2.medium` provides moderate resources suitable for small to medium workloads. (You should increase the size for Jenkins instance.)

 - `--key-name your-key-pair`: Specifies the name of the key pair to use for SSH access to the instance. You should have created this key pair in advance within AWS EC2.

 - `--security-group-ids sg-12345678`: Assigns the instance to a security group that controls inbound and outbound traffic rules. Security groups are configured with rules to allow or restrict network access.

 - `--subnet-id subnet-12345678`: Specifies the subnet where the instance will be launched. The subnet should be part of a Virtual Private Cloud (VPC) in which the instance can access resources and network configurations.

 - `--user-data file://userdata.sh`: Supplies the user data script (`userdata.sh` in this case) that will run automatically when the instance starts. If the userdata.sh is in the same directory where you’re running the command, you can reference it as `file://userdata.sh`. Otherwise, provide the full path, e.g., `file:///home/user/scripts/userdata.sh`.


##### Automated setup for the AWS EC2 Instance for Jenkins, SonarQube, and Nexus
 For a fully automated setup of the EC2 Instances using Terraform: 
 Refer to the [Terraform EC2 Setup](https://github.com/Godfrey22152/automation-of-aws-infra-using-terraform-via-Gitlab) for automated setup scripts.


#### Configure Installed Tools: `Jenkins`, `SonarQube`, and `Nexus`

##### Jenkins
 
Access running `Jenkins` and complete the setup over the browser using the EC2 Instance server `Public IP` at `http://<public-ip>:8080`.
   
- **Access Jenkins Initial Admin Password**:

   ```bash
   sudo cat /var/jenkins_home/secrets/initialAdminPassword
   ```

- **create an account to sign in and Install recommended Plugins**

##### SonarQube

- **Access running SonarQube server at `http://<public-ip>:9000`**
- **SonarQube Initial Admin Password Credentials**:
  - Username: `admin`
  - Password: `admin`


##### Nexus 
- **Access running Nexus server at `http://<public-ip>:8081`**  
- **Access Nexus Credentials**:
  The default admin password is stored in a file inside the container. Retrieve it by accessing container shell:

     ```bash
     docker exec nexus-container cat /nexus-data/admin.password
     ```

---
### CD: EKS Cluster Setup
Refer to the repository and guide [here](https://github.com/Godfrey22152/Automated-EKS-Cluster-Deployment-Pipeline/tree/main/aws_eks_terraform_files) for setting up your EKS cluster using terraform. 
 
- **Before you run the terraform scripts**: 
Ensure you have `terraform`, `AWS CLI`, and `kubectl` installed on the server which you will use to create the cluster:

```bash
# Terraform Installation
echo "Installing Terraform..."
wget https://releases.hashicorp.com/terraform/1.6.5/terraform_1.6.5_linux_386.zip
sudo apt-get install -y unzip
unzip terraform_1.6.5_linux_386.zip
sudo mv terraform /usr/local/bin/
```
```bash
# Install Kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl 
```
```bash
# Install AWS CLI 
sudo apt install unzip 
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

- **After Installation, Run**: 
```bash
aws configure
# Follow the prompts and provide your "Access key ID" and "Secret Access Key" 
```


**You can also refer to this repository and guide [here](https://github.com/Godfrey22152/Automated-EKS-Cluster-Deployment-Pipeline.git) for automated cluster creation.**


**After setting up, connect to the cluster with**:
```bash
aws eks --region <region> update-kubeconfig --name <cluster-name>

# In our case  
aws eks --region eu-west-1 update-kubeconfig --name odo-eks-cluster
```

---
### Install and run ARGOCD in the Cluster:

#### Installation Steps

1. **Create a Namespace for ArgoCD**:
To organize ArgoCD resources, first create a dedicated namespace:

```bash
 kubectl create namespace argocd
```

2. **Install ArgoCD**:
Apply the ArgoCD installation manifests to install it within the argocd namespace:

```bash
 kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
This command will deploy all necessary components for ArgoCD, including the API server, repository server, controller, and application controller.

3. **Verify Installation**:
Check the status of the ArgoCD pods and services to ensure they are running as expected:

```bash
 kubectl get pods -n argocd
 kubectl get services -n argocd
```

4. **Expose the ArgoCD Server**:
To access the ArgoCD web interface externally, change the argocd-server service type from ClusterIP to LoadBalancer. This will provision a cloud-assigned public IP:

```bash 
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'
```
>**NOTE**:
For local clusters (e.g., Minikube), use port-forwarding instead:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

#### Accessing the ArgoCD UI
Once the `argocd-server` service is set to `LoadBalancer`, retrieve the external IP:

```bash 
kubectl get svc -n argocd argocd-server
```
- Access the ArgoCD UI using this IP (or port-forwarded address) at:

```bash
https://<external-ip>
```
#### Logging into ArgoCD
The initial login requires the admin credentials. Retrieve the ArgoCD admin password as follows:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d
```
- Username: `admin`
- Password: (output from the command above)
>**Security Tip**: 
For security, change the `admin` password immediately after the first login or disable the admin account in favor of more secure authentication methods.

#### Post-Installation Configuration
See **[Manifest Branch](https://github.com/Godfrey22152/Multi-Tier-GitOps-Project/tree/manifest)** for a detailed procedure to run the application CD in ArgoCD. 

ArgoCD offers several configurations to optimize and secure your CD pipeline:

- **Integrate Git Repositories**: In the ArgoCD UI, configure repository access to link your GitOps repositories.
- **Create and Manage Projects**: Define ArgoCD projects to manage application access controls, roles, and sync configurations.
- **Enable SSO**: For production, enable Single Sign-On (SSO) for secure access using an identity provider.
- **Automate Sync Policies**: Configure sync policies to auto-sync or manually approve updates for enhanced control.


---
### Helm Configuration for Amazon EKS Cluster with Prometheus Deployment
This guide walks through configuring Helm on your EKS cluster and deploying Prometheus to monitor the application and cluster resources. Helm is a package manager for Kubernetes, making it easier to install, configure, and manage Kubernetes applications.

#### Prerequisites
- **Amazon EKS Cluster**: Ensure your EKS cluster is running and configured. The `kubectl` CLI should be connected to the cluster.
- **IAM Permissions**: Ensure your IAM role or user has the necessary permissions to manage resources within EKS.
- **kubectl**: Ensure `kubectl` is installed and configured for your EKS cluster.
- **AWS CLI**: The AWS CLI should be installed and configured with the appropriate IAM permissions.
>**NOTE**:
See [CD: EKS Cluster Setup](#cd-eks-cluster-setup) for guidance.

#### Installing Helm
1. Download Helm (Linux example below; for macOS and Windows, see Helm’s[installation documentation](https://helm.sh/docs/intro/install)):

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

2. Verify Helm Installation:

```bash
helm version
```
You should see Helm’s version details if installed correctly.

#### Configuring Helm for Amazon EKS
Helm requires access to the Kubernetes API server to manage resources within your EKS cluster.

1. Set up RBAC for Helm (Helm 3):

In Amazon EKS, you may need to set up permissions if your user doesn’t have full cluster access by default. Run the following commands to create a ServiceAccount and RoleBinding for Helm:

```bash
kubectl create serviceaccount helm -n kube-system
kubectl create clusterrolebinding helm --clusterrole cluster-admin --serviceaccount=kube-system:helm
```

2. Add Helm Repository for Prometheus:

Add the stable repository (includes Prometheus) to Helm:
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

#### Deploying Prometheus to the EKS Cluster
After you configure Helm for your Amazon EKS cluster, you can use it to deploy Prometheus with the following steps.

1. Create a Namespace for Prometheus:

```bash
kubectl create namespace monitoring
```
2. Deploy Prometheus.
Install the prometheus into the monitoring namespace:

```bash
helm upgrade -i prometheus prometheus-community/prometheus \
    --namespace monitoring \
    --set alertmanager.persistence.storageClass="gp2" \
    --set server.persistentVolume.storageClass="gp2"
```
> This command will deploy Prometheus, Alertmanager, and Grafana along with Kubernetes service monitors and alerting rules.

3. Verify that all of the Pods in the `monitoring` namespace are in the READY state.
```bash
kubectl get pods -n monitoring
```

An example output is as follows.

```bash 
NAME                                             READY   STATUS    RESTARTS   AGE
prometheus-alertmanager-59b4c8c744-r7bgp         1/2     Running   0          48s
prometheus-kube-state-metrics-7cfd87cf99-jkz2f   1/1     Running   0          48s
prometheus-node-exporter-jcjqz                   1/1     Running   0          48s
prometheus-node-exporter-jxv2h                   1/1     Running   0          48s
prometheus-node-exporter-vbdks                   1/1     Running   0          48s
prometheus-pushgateway-76c444b68c-82tnw          1/1     Running   0          48s
prometheus-server-775957f748-mmht9               1/2     Running   0          48s
```
> **NOTE**:
If running your cluster on a local environment without a dynamic volume provisioning, you may wish to install `OpenEBS` through `helm` before installing `Prometheus`:

```bash 
helm repo add openebs https://openebs.github.io/openebs
helm repo update
helm install openebs openebs/openebs -n openebs --set engines.replicated.mayastor.enabled=false --create-namespace
```
- Then Deploy Prometheus: 
```bash 
helm upgrade -i prometheus prometheus-community/prometheus \
   --namespace monitoring \
   --set alertmanager.persistentVolume.storageClass="openebs-hostpath" \
   --set server.persistentVolume.storageClass="openebs-hostpath"
```

#### Accessing the Prometheus UI
To access Prometheus, expose the prometheus-kube-prometheus-prometheus service as a LoadBalancer.

1. Patch the Service:

```bash
kubectl patch svc prometheus-server -n monitoring -p '{"spec": {"type": "LoadBalancer"}}'
```

2. Get the External IP:

After a few moments, retrieve the external IP assigned to Prometheus by running:
```bash
kubectl get svc -n monitoring prometheus-server
```

Once you have the external IP, you can access Prometheus in your browser at:

```bash
http://<external-ip>:9090
```

>**NOTE**:
For local clusters (e.g., Minikube), use port-forwarding instead:

```bash
kubectl --namespace monitoring port-forward svc/prometheus-server 9090:80
```
SEE **[Monitoring Folder in Main Branch](./Monitoring)**: Monitoring setup with Prometheus and Grafana for further guidance on completing the setup.

---
## Pipeline Setup
The Jenkins pipeline includes stages for compiling, file system scans, testing, static code analysis, building, image scanning, updating docker-tag, and Email notification of the builds. To set up the pipeline, follow these basic steps:

   - **Ensure the following plugins are installed and configured:**
   In Jenkins navigate to `Dashboard` > `Manage Jenkins` > `Plugins` > `available plugins` search and install: `Docker`, `Eclipse Temurin installer`, `SonarQube Scanner`, `Config File Provider`, `Maven Integration`, `Pipeline Maven Integration`, `Kubernetes`, `Kubernetes Credentials`, `Kubernetes CLI`, `Kubernetes Client API`, `Pipeline: Stage View`

   - **Clone the repository and navigate to the `Jenkins-CI` folder.**
- For details on setting up the Jenkins pipeline, refer to the `README.md` file in the **[Jenkins-CI](./Jenkins-CI)** folder. 


