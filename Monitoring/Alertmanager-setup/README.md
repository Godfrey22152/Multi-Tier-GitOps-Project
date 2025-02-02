
# Setting Up Prometheus Alerts and Alertmanager

This guide provides step-by-step instructions for configuring Prometheus alerts and integrating them with Alertmanager to monitor the BankApp application's custom metrics and the entire cluster metrics. It also covers how to notify stakeholders about significant events effectively.

## Prerequisites: State of our setup So Far
- **Prometheus** and **Alertmanager** have been installed, configured using Helm in the Kubernetes cluster. See [Setup](../README.md).
- **BankApp** application metrics are being scraped by Prometheus.

---
## 1. Configure Prometheus Alerts Rules

In Kubernetes environments, Prometheus alerting rules are managed via ConfigMaps. These rules allow Prometheus to evaluate conditions and notify Alertmanager when specific thresholds are met.

### Step-by-Step Guide to Set Alerting Rules

#### Step 1: Locate the Prometheus ConfigMap

Prometheus configuration is stored in the Kubernetes cluster ConfigMap. To identify the correct ConfigMap:

1. List all ConfigMaps in the `monitoring` namespace:

 ```bash
 kubectl get configmap -n monitoring
 ```

2. Identify the ConfigMap associated with the Prometheus server `prometheus-server`: 

 ```bash
  NAME                      DATA   AGE
  grafana                   1      45m
  kube-root-ca.crt          1      51m
  prometheus-alertmanager   1      50m
  prometheus-server         6      50m
 ```

#### Step 2: Write Prometheus Alerting Rules
1. Create a separate YAML file, e.g `prometheus_server_cm-alert_rules.yaml` and add the alert rules as shown below: 

 ```bash
 data:
   alerting_rules.yml: |
     groups:
       # Detect when MySQL database instance is down
       - name: mysql_alert_rule
         rules:
           - alert: MySQLInstanceDown
             expr: mysql_up{job="mysql-exporter"} == 0
             for: 5m
             labels:
               severity: critical
               team: infra-team
             annotations:
               summary: "MySQL Instance is down. BankApp may also be affected."
               description: |
                 The MySQL database Instance with the label `{{ $labels.instance }}` for job `{{ $labels.job }}` 
                 has been down for more than 5 seconds. 

       - name: Bankapp-alert-rules
         rules:
           # Detect when the BankApp application instance is down
           - alert: BankappInstanceDown
             expr: up{service="bankapp"} == 0
             for: 5s
             labels:
               severity: critical
               team: infra-team
             annotations:
               summary: "The Bankapp Application Instance Down ({{ $labels.instance }})"
               description: "The Bankapp Application Instance {{ $labels.instance }} is down for more than 5 seconds."

           # Tracks the total time taken to load the Bankapp dashboard.
           - alert: HighDashboardLoadTime
             expr: rate(bankapp_dashboard_seconds_sum[5m]) > 10
             for: 2m
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "High latency detected on the dashboard"
               description: "The dashboard is experiencing high latency for the last 5 minutes. Average latency: {{ $value }} seconds."

           # Track Percentage Change in Withdrawal Activity.
           - alert: WithdrawalRateChange
             expr: |
               ((rate(bankapp_withdraw_seconds_sum[5m]) - rate(bankapp_withdraw_seconds_sum[10m]))
               / rate(bankapp_withdraw_seconds_sum[10m]) * 100) > 50
             for: 5m
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "Significant Increase in Withdrawal Activity Detected"
               description: |
                 The withdrawal rate for the bankapp application has increased by {{ $value }}%
                 in the last 5 minutes compared to the previous 10 minutes.
                 This exceeds the defined threshold of 50% and could indicate abnormal activity
                 or system issues.
               action: |
                 - Investigate recent withdrawal trends in the application logs.
                 - Check for potential anomalies such as unexpected spikes in user activity.
                 - Verify the database and backend services for performance issues.

           # Track Average Time Spent on Transfers
           - alert: HighTransferLatency
             expr: rate(bankapp_transferAmount_seconds_sum[5m]) / rate(bankapp_transferAmount_seconds_count[5m]) > 10
             for: 2m
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "High transfer latency detected"
               description: "The average latency for transfers exceeds {{ $value }} seconds."

           # Track Total Time Spent on Withdrawals
           - alert: WithdrawalLoadSpike
             expr: increase(bankapp_withdraw_seconds_sum[5m]) > 50
             for: 2m
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "High withdrawal load detected"
               description: "Total time spent processing withdrawals exceeds 50 seconds in the last 5 minutes."

           # Track the Time Taken to Complete a Deposit
           - alert: HighDepositLatency
             expr: |
               histogram_quantile(0.95, rate(bankapp_deposit_seconds_bucket[5m])) > 5
             for: 2m
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "High Deposit Latency Detected"
               description: |
                 The 95th percentile latency for deposit operations in the bankapp application
                 has exceeded 5 seconds over the past 5 minutes. This could indicate performance
                 degradation in the system.
               action: |
                 - Investigate the bankapp application and database performance.
                 - Check for bottlenecks in the backend service or infrastructure.
                 - Monitor the transaction throughput for any potential correlation.

           # Track Deposit Activity
           - alert: SuddenChangeInDepositRate
             expr: |
               ((rate(bankapp_deposit_seconds_sum[4w]) - rate(bankapp_deposit_seconds_sum[8w]))
               / rate(bankapp_deposit_seconds_sum[8w]) * 100) < -50
             for: 4w
             labels:
               severity: warning
               team: infra-team
             annotations:
               summary: "Significant Change in Deposit Activity Detected"
               description: |
                 The deposit rate for the bankapp application has dropped by {{ $value }}%
                 in the last 4 weeks compared to the previous 8 weeks. This could indicate
                 abnormal activity or system issues.

           # Detect when cluster node instance is down
           - alert: ClusterNodeInstanceDown
             expr: up{job="kubernetes-nodes"} == 0
             for: 2m
             labels:
               severity: critical
               team: devops
             annotations:
               summary: "Cluster Node Instance Down ({{ $labels.instance }})"
               description: "Cluster Node Instance {{ $labels.instance }} is down for more than 2 minutes."

           # Monitor memory usage percentage on Kubernetes nodes
           - alert: NodeMemoryUsageHigh
             expr: 100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 80
             for: 10m
             labels:
               severity: critical
               team: devops
             annotations:
               summary: "High Memory Usage ({{ $labels.instance }})"
               description: "Node {{ $labels.instance }} memory usage is above 80% for more than 10 minutes."

           # Detect when containers are experiencing CPU throttling
           - alert: CPULimitsThrottlingHigh
             expr: rate(container_cpu_cfs_throttled_seconds_total[5m]) / rate(container_cpu_cfs_periods_total[5m]) > 0.2
             for: 5m
             labels:
               severity: warning
               team: devops
             annotations:
               summary: "High CPU Throttling Detected"
               description: "Container {{ $labels.container }} in namespace {{ $labels.namespace }} is experiencing high CPU throttling."

           # Monitor when disk space usage exceeds a threshold
           - alert: DiskSpaceLow
             expr: 100 - (node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 > 90
             for: 10m
             labels:
               severity: critical
               team: devops
             annotations:
               summary: "Low Disk Space ({{ $labels.instance }})"
               description: "Instance {{ $labels.instance }} has less than 10% free disk space remaining."

           # Detect pods in a CrashLoopBackOff state
           - alert: PodCrashLoopBackOff
             expr: kube_pod_container_status_restarts_total > 150
             for: 5m
             labels:
               severity: critical
               team: devops
             annotations:
               summary: "Pod in CrashLoopBackOff ({{ $labels.pod }})"
               description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} is restarting frequently."

           # Monitor for unusually high API server request loads
           - alert: HighAPIServerRequests
             expr: rate(apiserver_request_total[5m]) > 500
             for: 5m
             labels:
               severity: warning
               team: devops
             annotations:
               summary: "High API Server Requests"
               description: "API server is receiving more than 500 requests per second."

 ```

2. **Why Create Rules in a Separate File?**
  - Simplifies maintenance and prevents accidental overwrites in the ConfigMap.
  - Makes debugging and testing of new rules more straightforward.

---
#### Step 3: Add Alerting Rules to the Prometheus ConfigMap
Use `kubectl patch` to inject the alerting rules into the Prometheus server ConfigMap:

1. Run the following command to apply the rules:
```bash
kubectl patch configmap prometheus-server -n monitoring --patch "$(cat prometheus_server_cm-alert_rules.yaml)"
```

2. Confirm the rules were successfully added:
```bash 
kubectl get configmap prometheus-server -n monitoring -o yaml
```

---
#### Step 4: Verify the Alerting Rules

1. **Access the Prometheus UI**  
   Open Prometheus in your browser (e.g., `http://<prometheus-server-ip>:9090`).

2. **Navigate to the Alerts Tab**  
   In the Prometheus UI, go to the **Alerts** tab to ensure your alerting rules are loaded and active.

   - **Prometheus Active Alerts:**  
     ![Prometheus Alerts Tab](../images/prometheus_alerts_tab.png)
     ![Prometheus Alerts Tab](../images/prometheus_alerts_tab_2.png)  
     _These screenshots shows the list of active and pending alerts based on the defined rules. Use this tab to confirm that your rules are correctly recognized by Prometheus._


---

## 2. Configure Alertmanager for Notifications
Alertmanager handles notifications for alerts triggered in Prometheus. It supports routing based on label matches, notification grouping, and multi-channel delivery.
We will define the notification system (the receivers and routing) that will ensure balanced flexibility, efficiency, team-specific targeting, precise and effective alerting through the following steps: 

### Step-by-Step Guide to Set the Alert Notification system:

#### Step 1: Locate the Alertmanager ConfigMap

1. List ConfigMaps in the `monitoring` namespace:

  ```bash
  kubectl get configmap -n monitoring
  ```

2. Identify the `prometheus-alertmanager` ConfigMap.:
  ```bash
  NAME                      DATA   AGE
  grafana                   1      45m
  kube-root-ca.crt          1      51m
  prometheus-alertmanager   1      50m
  prometheus-server         6      50m
  ```

#### Step 2: Write Alertmanager Alerting Rules
Create a YAML file, e.g., `prometheus-alertmanager_cm_notification.yaml`, with the following content:

 ```bash
 data:
   alertmanager.yml: |
     global:
       resolve_timeout: 5s

     inhibit_rules:
       - source_matchers:
           - alertname="MySQLInstanceDown"
         target_matchers:
           - alertname="BankappInstanceDown"
         equal:
           - team

     route:
       group_interval: 5m
       group_wait: 10s
       receiver: default-receiver
       repeat_interval: 3h
       routes:
         - matchers:
             - team=~"devops"
             - severity=~"critical|warning"
           receiver: devops-team
           continue: true
         - matchers:
             - team=~"infra-team"
             - severity=~"critical|warning"
           receiver: on-call-team
           continue: true

     receivers:
       - name: default-receiver
       - name: "devops-team"
         email_configs:
           - to: devops-team@gmail.com
             from: bankapp-project@gmail.com                  # Replace with Email sender
             smarthost: smtp.gmail.com:587                    # SMTP server
             auth_username: bankapp-project@gmail.com         # Replace with SMTP auth username
             auth_identity: bankapp-project@gmail.com         # replace with SMTP auth identity
             auth_password: "YOUR_APP_PASSWORD"               # SMTP auth password
             send_resolved: true
             headers:
               subject: " Prometheus - Alert "
             text: |
               Critical alert triggered: {{ .CommonAnnotations.summary }}
               Description: {{ .CommonAnnotations.description }}

       - name: "on-call-team"
         slack_configs:
           - api_url: "https://hooks.slack.com/services/TXXXX/BXXXX/XXXXXXXXXX"                              # Webhook URL of Slack
             channel: "#devops-projects"                                                                     # Specify the Slack Channel or User
             send_resolved: true
             text: |
               Critical alert triggered: {{ .CommonAnnotations.summary }}
               Description: {{ .CommonAnnotations.description }}
 ```

---
##### **NOTE: Setting Up Gmail `auth_password` and Retrieving Slack `api_url`**

###### Setting Up Gmail `auth_password` for SMTP Authentication
The `auth_password` field is crucial for authenticating your Gmail account to send email alerts through Alertmanager. Follow these steps to set it up securely:

- **Generate an App Password**  
  App passwords are a secure way to allow third-party applications to access your Gmail account without sharing your actual password.

   - Ensure you have **2-Step Verification** enabled and active on your account.
   - Log in to your Google account at [Google Account Settings](https://myaccount.google.com/apppasswords) and generate `App Password`.
   - In the **App Passwords** page:  
     - **App Name**: Add an app name which can be any name (e.g., "Prometheus Alerts").
   - Click **Generate** to get a 16-character App Password. **Copy this password**, as you will need it for the `auth_password` field in Alertmanager.

   Replace the `auth_password` in the YAML file with the generated App Password:
   ```yaml
   auth_password: xxxxxxxxxxxxxxxx  # Replace with your App Password
   ```


###### Retrieving Slack `api_url` for Notifications

Slack notifications in Alertmanager are sent via a **Webhook URL (`api_url`)**. Here’s how to get it:

1. **Create a Slack App for Webhook Integration**
   - Go to [Slack API Apps](https://api.slack.com/apps).
   - Click **Create New App** and select **From Scratch**.
   - Name your app (e.g., "bankapp-alerts") and choose the Slack workspace where you want to send alerts.

2. **Set Up Incoming Webhooks**
   - In the app settings, go to the **Incoming Webhooks** section under `Features`.
   - Click the toggle to enable **Activate Incoming Webhooks**.
   - Click **Add New Webhook to Workspace**.
   - Select the channel where the alerts should be sent (e.g., `#devops-projects`).
   - Click **Allow** to generate the Webhook URL.

3. **Copy the Webhook URL**
   - Once generated, copy the Webhook URL, which looks like this:

     ```
     https://hooks.slack.com/services/TXXXX/BXXXX/XXXXXXXXXX
     ```

4. **Test the Webhook Integration**
   - Use a tool like `curl` to send a test message:

     ```bash
     curl -X POST -H 'Content-type: application/json' --data '{"text":"This is a test alert from Alertmanager!"}' https://hooks.slack.com/services/TXXXX/BXXXX/XXXXXXXXXX
     ```
   - Confirm the test message appears in the designated Slack channel as:

     ```
     This is a test alert from Alertmanager!
     ```

5. **Update the Alertmanager Configuration**
   - Replace the placeholder `api_url` in the YAML file with the Webhook URL:

     ```yaml
     slack_configs:
       - api_url: "https://hooks.slack.com/services/TXXXX/BXXXX/XXXXXXXXXX"  # Replace with your Webhook URL
     ```

---
#### Step 3: Apply the Configuration
Use `kubectl patch` to apply the configuration into the `prometheus-alertmanager` ConfigMap:

1. Patch the prometheus-alertmanager ConfigMap:
```bash
kubectl patch configmap prometheus-alertmanager -n monitoring --patch "$(cat prometheus-alertmanager_cm_notification.yaml)"
```

2. Reload Alertmanager to apply changes:

After updating the `prometheus-alertmanager` configmap configuration file, Use the curl command below to instruct alertmanager server to reload the updated configuration without restarting the server.

```bash
curl -X POST http://<PROMETHEUS-ALERTMANAGER-EXTERNAL-IP>:9093/-/reload
```

3. Access the Alertmanager UI: 
- Verify the configuration in the Alertmanager UI (`http://<ALERTMANAGER_EXTERNAL_IP>:9093`).
- Navigate to the `Status` Tab of the Alertmanager to Verify the updated configuration  
   
   - **Alertmanager updated Configuration:**  
     ![Alertmanager Status Tab](../images/alertmanager_status_tab.png)
     ![Alertmanager Status Tab](../images/alertmanager_status_tab_2.png)
     _This screenshot shows a snippet of the Alertmanager Notification configuration in the alertmanager status tab._


---
### Features of our Alertmanager ConfigMap Configuration

1. **Efficient Alert Management**:
- **Inhibit Rules**: Suppresses `BankappInstanceDown` alerts when `MySQLInstanceDown` is active, reducing alert noise and redundancy.

2. **Flexible Routing**
- **Team and Severity-Based**: Routes alerts to appropriate teams based on labels like `team` and `severity`.
- **Fallback Handling**: Uses `continue: true` to allow unmatched alerts to cascade to other routes.

3. **Diverse Notification Channels**
- **Email Notifications**: Configured for the `DevOps` team with dynamic placeholders to provide detailed alert information.
- **Slack Integration**: Sends alerts to the `#devops-projects` channel for instant team visibility and collaboration.

4. **Optimized Timing**
- **Grouped Alerts**: Alerts are grouped and sent every **5 minutes** (`group_interval`), minimizing notification fatigue.
- **Repeat Alerts**: Resends unresolved critical alerts every **3 hours** for sustained visibility.

5. **Dynamic Alert Resolution**
- **Resolved Alerts**: `send_resolved: true` ensures teams are notified when an issue is resolved, aiding in effective incident tracking.


---
### Testing Notifications in Prometheus and Alertmanager

#### Understand the Notification Flow

1. **Prometheus** detects, evaluates and triggers the alerting conditions based on the defined alerting rules.
2. **Alertmanager** routes the alert to the appropriate receiver (Slack or Email) based on the routing configuration.

#### Testing Steps

##### First Notification Test

1. **Trigger an Alert in Cluster**  
   - Simulate a condition that matches an alerting rule:    
     - Cause an issue in the cluster, such as forcing a pod into a `CrashLoopBackOff` state:  
       - In my case, I obstructed the `openebs` pod in my cluster into `CrashLoopBackOff` 

2. **Verify Active Alerts in Prometheus**  
   - Access the Prometheus UI and navigate to the **Alerts** tab to check if the alert is listed as **Firing**.  

     - **Verify Active Alerts in Prometheus:**  
       ![Prometheus Active Alerts](../images/prometheus_active_alert.png)


3. **Verify Notifications in Alertmanager**    
   - Go to the **Alerts** section to confirm the alert has been routed correctly.

     - **Notifications in Alertmanager:**  
       ![Notifications in Alertmanager](../images/alertmanager_notification.png)

4. **Confirm Notifications at the Receiver**  
   - Check the configured receivers:  
     - **For Email**: Verify the alert email in the inbox.  
     - **For Slack**: Confirm the message in the designated Slack channel.
          
       - **Notification Confirmation:**  
         ![Notification Confirmation](../images/notification_confirmation_slack.png)  
         - *The image shows a notification confirmation in Slack, highlighting both Firing and Resolved alerts for the `openebs` pod in `CrashLoopBackOff` state.* 
         - *The **Firing Notification** alerts the team about the ongoing issue, while the **Resolved Notification** confirms that the issue has been addressed, ensuring 
            effective incident management and tracking.*


---
##### **Notification Test for Inhibition Rule**

##### **Scenario**
In this test, we simulate a situation where a Prometheus alert is triggered for both the `MySQLInstanceDown` and `BankappInstanceDown` alerts. The inhibition rule is configured to suppress the `BankappInstanceDown` alert when the `MySQLInstanceDown` alert is active. This prevents redundant alerts caused by cascading failures, allowing the focus to remain on the root cause. 

###### Steps to Simulate the Scenario

1. **Stop the Dependent MySQL Pod**  
Scale down the `mysql` deployment to zero replicas, which will stop `mysql-d6ff9ccb5-zf9c7` pod, which the `Bankapp` pods depends on:
   ```bash
   kubectl scale deployment mysql --replicas=0 -n webapps
   ```
   **This action will trigger the `MySQLInstanceDown` alert in Prometheus.**

   - **To scale back the deployment once testing is complete, you can scale it back up like this**:

     ```bash
     kubectl scale deployment mysql --replicas=1 -n webapps
     ```

2. **Confirm Alert Triggering**
Verify that the `MySQLInstanceDown` alert has been triggered in the Prometheus Alerting interface:
   - Access the Prometheus web UI: `http://<prometheus-server-ip>:9090/alerts`.
   - Confirm that `MySQLInstanceDown` is **firing**.

     - **MySQLInstanceDown Firing**  
         ![MySQLInstanceDown firing](../images/MySQLInstanceDown_firing.png)
         ![MySQLInstanceDown firing](../images/MySQLInstanceDown_firing_2.png)
         *MySQLInstanceDown firing*  

3. Check for the `BankappInstanceDown` Alert
As **Bankapp** pods depends on the **MySQL** pod, stopping the MySQL pod will also cause the `BankappInstanceDown` alert condition to be met. However, the configured inhibition rule should suppress this alert.

4. **Verify the Inhibition Rule**

- Check the `Alerts` tab in Alertmanager web UI to see the active alerts.
- You should only see the `MySQLInstanceDown` alert. The `BankappInstanceDown` alert will be suppressed due to the inhibition rule.

   - **Inhibition Rule Alerts**  
         ![Inhibition Alerts](../images/inhibition_alert.png)
         *`MySQLInstanceDown` alert*
         ![Inhibition Alerts](../images/inhibition_alert_2.png)
         *`MySQLInstanceDown` and `BankappInstanceDown` alert revealed when you click to see inhibition alerts*

5. **Trigger Notification Test**
Check the notification channels (email or Slack) to ensure:

- You only receive a notification for the `MySQLInstanceDown` alert.
- The `BankappInstanceDown` alert is not sent due to the inhibition rule.

   - **Notification for only `MySQLInstanceDown` alert**  
         ![Notification](../images/MySQLInstanceDown_slack_alert.png)
         *The `BankappInstanceDown` alert is not sent due to the inhibition rule*

---

## 3. kindly visit the [Grafana Setup](../Grafana-setup) folder for a detailed guide on how the `Grafana` was setup and how application metrics was visiualized in dashboards and application monitored through `Grafana Alerts`.

---

## 4. Reference
- [Prometheus Alerting Documentation](https://prometheus.io/docs/alerting/latest/overview/)
- [Alertmanager Configuration Documentation](https://prometheus.io/docs/alerting/latest/configuration/)
- [Grafana Dashboards](https://grafana.com/grafana/dashboards)

---

## 5. Important Notes
- Ensure Prometheus and Alertmanager pods have the necessary RBAC permissions in Kubernetes.
- Regularly test alert configurations to avoid missing critical incidents.

---

Interact with the application to simulate real-world scenarios, ensuring alerts and notifications are functioning as expected.

