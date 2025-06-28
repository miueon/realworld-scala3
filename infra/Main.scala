import besom.*
import besom.aliases.NonEmptyString
import besom.api.gcp
import besom.api.gcp.compute.*
import besom.api.gcp.compute.inputs.*
import besom.api.gcp.container.*
import besom.api.gcp.container.inputs.*
import besom.api.gcp.redis.*
import besom.api.gcp.redis.inputs.*
import besom.api.gcp.secretmanager.{Secret as GcpSecret, SecretArgs as GcpSecretArgs, SecretVersion, SecretVersionArgs}
import besom.api.gcp.secretmanager.inputs.*
import besom.api.gcp.sql.DatabaseInstanceArgs
import besom.api.gcp.sql.UserArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsBackupConfigurationArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsIpConfigurationArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsLocationPreferenceArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsMaintenanceWindowArgs
import besom.api.kubernetes
import besom.api.kubernetes.ProviderArgs as kProviderArgs
import besom.api.kubernetes.apps.v1.*
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.core
import besom.api.kubernetes.core.v1.{ConfigMap, ConfigMapArgs, Secret as K8sSecret, SecretArgs, Service, ServiceArgs}
import besom.api.kubernetes.core.v1.enums.*
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*
import besom.internal.Context
import besom.internal.Input
import besom.internal.Output

@main def main = Pulumi.run:
  // Enable required GCP services
  val requiredServices = List(
    "container.googleapis.com",
    "compute.googleapis.com",
    "sqladmin.googleapis.com",
    "redis.googleapis.com",
    "secretmanager.googleapis.com",
    "monitoring.googleapis.com",
    "logging.googleapis.com"
  )

  val enabledServices = requiredServices.map { service =>
    gcp.projects.Service(
      name = s"enable-${service.replace(".", "-")}",
      gcp.projects.ServiceArgs(
        service = service,
        disableDependentServices = true,
        disableOnDestroy = false
      )
    )
  }

  // Create VPC network
  val vpc = Network(
    name = "realworld-vpc",
    NetworkArgs(
      autoCreateSubnetworks = false,
      description = "VPC network for Real World Scala application"
    )
  )

  // Create subnet for GKE cluster
  val subnet = Subnetwork(
    name = "realworld-subnet",
    SubnetworkArgs(
      network = vpc.id,
      region = "us-central1",
      ipCidrRange = "10.0.0.0/24",
      secondaryIpRanges = List(
        SubnetworkSecondaryIpRangeArgs(
          ipCidrRange = "10.1.0.0/16",
          rangeName = "pods"
        ),
        SubnetworkSecondaryIpRangeArgs(
          ipCidrRange = "10.2.0.0/16",
          rangeName = "services"
        )
      )
    ),
    opts = opts(dependsOn = vpc)
  )

  // Create production-ready GKE cluster
  val k8sCluster = gcp.container.Cluster(
    name = "realworld-cluster",
    gcp.container.ClusterArgs(
      location = "us-central1",
      network = vpc.id,
      subnetwork = subnet.id,
      removeDefaultNodePool = true,
      initialNodeCount = 1,
      minMasterVersion = "1.30.4-gke.1348000",
      deletionProtection = false,
      ipAllocationPolicy = ClusterIpAllocationPolicyArgs(
        clusterSecondaryRangeName = "pods",
        servicesSecondaryRangeName = "services"
      ),
      privateClusterConfig = ClusterPrivateClusterConfigArgs(
        enablePrivateNodes = true,
        enablePrivateEndpoint = false,
        masterIpv4CidrBlock = "10.3.0.0/28"
      ),
      masterAuth = ClusterMasterAuthArgs(
        clientCertificateConfig = ClusterMasterAuthClientCertificateConfigArgs(
          issueClientCertificate = false
        )
      ),
      workloadIdentityConfig = ClusterWorkloadIdentityConfigArgs(
        workloadPool = p"${config.require[String]("gcp:project")}.svc.id.goog"
      ),
      addonsConfig = ClusterAddonsConfigArgs(
        httpLoadBalancing = ClusterAddonsConfigHttpLoadBalancingArgs(
          disabled = false
        ),
        networkPolicyConfig = ClusterAddonsConfigNetworkPolicyConfigArgs(
          disabled = false
        )
      ),
      networkPolicy = ClusterNetworkPolicyArgs(
        enabled = true,
        provider = "CALICO"
      )
    ),
    opts = opts(dependsOn = enabledServices)
  )

  // Create production node pool
  val nodePool = gcp.container.NodePool(
    name = "realworld-nodes",
    gcp.container.NodePoolArgs(
      cluster = k8sCluster.name,
      location = k8sCluster.location,
      nodeCount = 2,
      autoscaling = NodePoolAutoscalingArgs(
        minNodeCount = 1,
        maxNodeCount = 5
      ),
      nodeConfig = NodePoolNodeConfigArgs(
        machineType = "e2-standard-2",
        diskSizeGb = 50,
        diskType = "pd-ssd",
        imageType = "COS_CONTAINERD",
        oauthScopes = List(
          "https://www.googleapis.com/auth/cloud-platform"
        ),
        workloadMetadataConfig = NodePoolNodeConfigWorkloadMetadataConfigArgs(
          mode = "GKE_METADATA"
        ),
        shieldedInstanceConfig = NodePoolNodeConfigShieldedInstanceConfigArgs(
          enableSecureBoot = true,
          enableIntegrityMonitoring = true
        ),
        metadata = Map(
          "disable-legacy-endpoints" -> "true"
        )
      ),
      management = NodePoolManagementArgs(
        autoRepair = true,
        autoUpgrade = true
      )
    ),
    opts = opts(dependsOn = k8sCluster)
  )

  val context =
    p"${k8sCluster.project}_${k8sCluster.location}_${k8sCluster.name}"
  val kubeconfig =
    p"""apiVersion: v1
      |clusters:
      |- cluster:
      |    certificate-authority-data: ${k8sCluster.masterAuth.clusterCaCertificate
        .map(_.get)
        .asPlaintext}
      |    server: https://${k8sCluster.endpoint}
      |  name: $context
      |contexts:
      |- context:
      |    cluster: $context
      |    user: $context
      |  name: $context
      |current-context: $context
      |kind: Config
      |preferences: {}
      |users:
      |- name: $context
      |  user:
      |    exec:
      |      apiVersion: client.authentication.k8s.io/v1beta1
      |      command: gke-gcloud-auth-plugin
      |      installHint: Install gke-gcloud-auth-plugin for use with kubectl by following
      |        https://cloud.google.com/blog/products/containers-kubernetes/kubectl-auth-changes-in-gke
      |      provideClusterInfo: true
      |""".stripMargin

  val gkeProvider = kubernetes.Provider("gke", kProviderArgs(kubeconfig = kubeconfig))

  // Create Cloud SQL instance with production settings
  val sqlInstance = gcp.sql.DatabaseInstance(
    name = "realworld-postgres",
    args = DatabaseInstanceArgs(
      databaseVersion = "POSTGRES_16",
      region = "us-central1",
      settings = DatabaseInstanceSettingsArgs(
        tier = "db-custom-1-3840",
        availabilityType = "REGIONAL",
        diskSize = 20,
        diskType = "PD_SSD",
        diskAutoresize = true,
        diskAutoresizeLimit = 100,
        activationPolicy = "ALWAYS",
        deletionProtectionEnabled = true,
        backupConfiguration = DatabaseInstanceSettingsBackupConfigurationArgs(
          enabled = true,
          startTime = "03:00",
          pointInTimeRecoveryEnabled = true,
          transactionLogRetentionDays = 7
        ),
        maintenanceWindow = DatabaseInstanceSettingsMaintenanceWindowArgs(
          day = 7,
          hour = 4,
          updateTrack = "stable"
        ),
        ipConfiguration = DatabaseInstanceSettingsIpConfigurationArgs(
          ipv4Enabled = true,
          requireSsl = true,
          authorizedNetworks = List(
            gcp.sql.inputs.DatabaseInstanceSettingsIpConfigurationAuthorizedNetworkArgs(
              value = "0.0.0.0/0",
              name = "all"
            )
          )
        ),
        locationPreference = DatabaseInstanceSettingsLocationPreferenceArgs(
          zone = "us-central1-a"
        ),
        databaseFlags = List(
          gcp.sql.inputs.DatabaseInstanceSettingsDatabaseFlagArgs(
            name = "log_statement",
            value = "all"
          ),
          gcp.sql.inputs.DatabaseInstanceSettingsDatabaseFlagArgs(
            name = "log_min_duration_statement",
            value = "1000"
          )
        )
      )
    ),
    opts = opts(dependsOn = enabledServices)
  )

  val databaseInstance = gcp.sql.Database(
    name = "realworld",
    args = gcp.sql.DatabaseArgs(
      instance = sqlInstance.name
    ),
    opts = opts(dependsOn = sqlInstance)
  )

  // Create Secret Manager secrets for database credentials
  val dbPasswordSecret = GcpSecret(
    name = "db-password",
    GcpSecretArgs(
      secretId = "db-password",
      replication = SecretReplicationArgs(
        auto = SecretReplicationAutoArgs()
      )
    )
  )

  val dbPasswordVersion = SecretVersion(
    name = "db-password-version",
    SecretVersionArgs(
      secret = dbPasswordSecret.id,
      secretData = config.require[String]("SC_POSTGRES_PASSWORD")
    ),
    opts = opts(dependsOn = dbPasswordSecret)
  )

  val dbUser = gcp.sql.User(
    name = "postgres",
    args = UserArgs(
      instance = sqlInstance.name,
      password = config.require[String]("SC_POSTGRES_PASSWORD"),
      `type` = "BUILT_IN"
    ),
    opts = opts(dependsOn = sqlInstance)
  )

  // Create GCP Memorystore Redis instance
  val redisInstance = gcp.redis.Instance(
    name = "realworld-redis",
    gcp.redis.InstanceArgs(
      name = "realworld-redis",
      memorySizeGb = 1,
      region = "us-central1",
      locationId = "us-central1-a",
      tier = "BASIC",
      redisVersion = "REDIS_7_0",
      displayName = "Real World Redis Cache",
      reservedIpRange = "10.4.0.0/29",
      authorizedNetwork = vpc.id,
      maintenancePolicy = gcp.redis.inputs.InstanceMaintenancePolicyArgs(
        weeklyMaintenanceWindows = List(
          gcp.redis.inputs.InstanceMaintenancePolicyWeeklyMaintenanceWindowArgs(
            day = "SUNDAY",
            startTime = gcp.redis.inputs.InstanceMaintenancePolicyWeeklyMaintenanceWindowStartTimeArgs(
              hours = 4,
              minutes = 0
            )
          )
        )
      )
    ),
    opts = opts(dependsOn = List(vpc) ++ enabledServices)
  )

  // Create static IP for load balancer
  val staticIp = Address(
    name = "realworld-lb-ip",
    AddressArgs(
      region = "us-central1",
      addressType = "EXTERNAL"
    )
  )

  // Create firewall rules
  val firewallRule = Firewall(
    name = "realworld-allow-https",
    FirewallArgs(
      network = vpc.id,
      allows = List(
        FirewallAllowArgs(
          protocol = "tcp",
          ports = List("80", "443")
        )
      ),
      sourceRanges = List("0.0.0.0/0"),
      targetTags = List("realworld-lb")
    ),
    opts = opts(dependsOn = vpc)
  )

  // Deploy the application
  val realworldApp = gkeProvider.flatMap { provider =>
    realworldService(
      instanceName = "realworld-app",
      cloudSqlInstance = sqlInstance.connectionName,
      appImageTag = "latest",
      redisHost = redisInstance.host,
      redisPort = redisInstance.port,
      options = CustomResourceOptions(provider = provider)
    )
  }

  // Create application secrets
  val appSecrets = gkeProvider.flatMap(provider => appSecretsConfigMap(provider))
  val appConfig  = gkeProvider.flatMap(provider => appConfigMap(provider))

  Stack(
    k8sCluster,
    nodePool,
    redisInstance,
    databaseInstance,
    dbUser,
    dbPasswordSecret,
    dbPasswordVersion,
    staticIp,
    firewallRule,
    realworldApp,
    appSecrets,
    appConfig
  ).exports(
    clusterName = k8sCluster.name,
    clusterEndpoint = k8sCluster.endpoint,
    redisHost = redisInstance.host,
    redisPort = redisInstance.port,
    dbConnectionName = sqlInstance.connectionName,
    staticIp = staticIp.address
  )

def realworldService(
  using Context
)(
  instanceName: NonEmptyString,
  cloudSqlInstance: Output[String],
  appImageTag: NonEmptyString,
  redisHost: Output[String],
  redisPort: Output[Int],
  options: CustomResourceOptions = CustomResourceOptions()
): Output[Service] =
  val labels = Map("app" -> instanceName)
  val deployment = Deployment(
    instanceName,
    DeploymentArgs(
      metadata = ObjectMetaArgs(
        name = instanceName
      ),
      spec = DeploymentSpecArgs(
        replicas = 1,
        selector = LabelSelectorArgs(
          matchLabels = labels
        ),
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            name = instanceName,
            labels = labels,
          ),
          spec = PodSpecArgs(
            containers = List(
              ContainerArgs(
                name = instanceName,
                image = s"ghcr.io/miueon/realworld-smithy4s:$appImageTag",
                ports = List(
                  ContainerPortArgs(
                    containerPort = 8088
                  )
                ),
                env = List(
                  EnvVarArgs(
                    name = "SC_POSTGRES_HOST",
                    value = "127.0.0.1"
                  ),
                  EnvVarArgs(
                    name = "SC_POSTGRES_PORT",
                    value = "5432"
                  ),
                  EnvVarArgs(
                    name = "SC_POSTGRES_DB",
                    value = "realworld"
                  ),
                  EnvVarArgs(
                    name = "SC_POSTGRES_USER",
                    value = "postgres"
                  ),
                  EnvVarArgs(
                    name = "SC_REDIS_HOST",
                    value = redisHost
                  ),
                  EnvVarArgs(
                    name = "SC_REDIS_PORT",
                    value = redisPort.map(_.toString)
                  )
                ),
                envFrom = List(
                  EnvFromSourceArgs(
                    configMapRef = ConfigMapEnvSourceArgs(
                      name = "app-config"
                    ),
                    secretRef = SecretEnvSourceArgs(
                      name = "app-secrets"
                    )
                  )
                ),
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu"    -> "200m",
                    "memory" -> "256Mi"
                  ),
                  limits = Map(
                    "cpu"    -> "500m",
                    "memory" -> "512Mi"
                  )
                ),
                livenessProbe = ProbeArgs(
                  httpGet = HttpGetActionArgs(
                    path = "/health",
                    port = 8088
                  ),
                  initialDelaySeconds = 30,
                  periodSeconds = 10
                ),
                readinessProbe = ProbeArgs(
                  httpGet = HttpGetActionArgs(
                    path = "/health",
                    port = 8088
                  ),
                  initialDelaySeconds = 5,
                  periodSeconds = 5
                )
              ),
              ContainerArgs(
                args = cloudSqlInstance.map(instance => List(s"--instances=$instance=tcp:5432")),
                name = "cloud-sql-proxy",
                image = "gcr.io/cloud-sql-proxy/cloud-sql-proxy:2.8.0",
                securityContext = SecurityContextArgs(
                  runAsNonRoot = true,
                  runAsUser = 65532
                ),
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu"    -> "100m",
                    "memory" -> "128Mi"
                  ),
                  limits = Map(
                    "cpu"    -> "200m",
                    "memory" -> "256Mi"
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  Service(
    instanceName,
    ServiceArgs(
      metadata = ObjectMetaArgs(
        name = instanceName,
        labels = labels
      ),
      spec = ServiceSpecArgs(
        ports = List(
          ServicePortArgs(
            name = "http",
            port = 80,
            targetPort = 8088
          )
        ),
        selector = deployment.spec.template.metadata.labels
      )
    ),
    opts = options
  )
end realworldService

def appConfigMap(
  using Context
)(
  provider: kubernetes.Provider
): Output[ConfigMap] =
  ConfigMap(
    "app-config",
    ConfigMapArgs(
      metadata = ObjectMetaArgs(
        name = "app-config"
      ),
      data = Map(
        "SC_APP_ENV" -> "Prod",
        "DEBUG"      -> "false"
      )
    ),
    opts = CustomResourceOptions(provider = provider)
  )

def appSecretsConfigMap(
  using Context
)(
  provider: kubernetes.Provider
): Output[K8sSecret] =
  K8sSecret(
    "app-secrets",
    SecretArgs(
      metadata = ObjectMetaArgs(
        name = "app-secrets"
      ),
      `type` = "Opaque",
      stringData = Map(
        "SC_POSTGRES_PASSWORD" -> config.require[String]("SC_POSTGRES_PASSWORD"),
        "SC_ACCESS_TOKEN_KEY"  -> config.require[String]("SC_ACCESS_TOKEN_KEY"),
        "SC_PASSWORD_SALT"     -> config.require[String]("SC_PASSWORD_SALT")
      )
    ),
    opts = CustomResourceOptions(provider = provider)
  )
