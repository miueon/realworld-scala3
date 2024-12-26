import besom.*
import besom.aliases.NonEmptyString
import besom.api.gcp
import besom.api.gcp.sql.DatabaseInstanceArgs
import besom.api.gcp.sql.UserArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsLocationPreferenceArgs
import besom.api.kubernetes
import besom.api.kubernetes.ProviderArgs as kProviderArgs
import besom.api.kubernetes.apps.v1.*
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.core
import besom.api.kubernetes.core.v1.*
import besom.api.kubernetes.core.v1.enums.*
import besom.api.kubernetes.core.v1.inputs.ConfigMapEnvSourceArgs
import besom.api.kubernetes.core.v1.inputs.ContainerArgs
import besom.api.kubernetes.core.v1.inputs.ContainerPortArgs
import besom.api.kubernetes.core.v1.inputs.EnvFromSourceArgs
import besom.api.kubernetes.core.v1.inputs.EnvVarArgs
import besom.api.kubernetes.core.v1.inputs.ExecActionArgs
import besom.api.kubernetes.core.v1.inputs.PodSpecArgs
import besom.api.kubernetes.core.v1.inputs.PodTemplateSpecArgs
import besom.api.kubernetes.core.v1.inputs.ProbeArgs
import besom.api.kubernetes.core.v1.inputs.ResourceRequirementsArgs
import besom.api.kubernetes.core.v1.inputs.SecretEnvSourceArgs
import besom.api.kubernetes.core.v1.inputs.SecurityContextArgs
import besom.api.kubernetes.core.v1.inputs.ServicePortArgs
import besom.api.kubernetes.core.v1.inputs.ServiceSpecArgs
import besom.api.kubernetes.core.v1.inputs.VolumeArgs
import besom.api.kubernetes.core.v1.inputs.VolumeMountArgs
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*
import besom.internal.Context
import besom.internal.Input
import besom.internal.Output

@main def main = Pulumi.run {
  val kubernetesEngine = gcp.projects.Service(
    name = "enable-kubernetes-engine",
    gcp.projects.ServiceArgs(
      service = "container.googleapis.com",
      disableDependentServices = true,
      disableOnDestroy = true
    )
  )

  val k8sCluster = gcp.container.Cluster(
    name = "cluster",
    gcp.container.ClusterArgs(
      deletionProtection = false,
      initialNodeCount = 1,
      location = "us-central1-c",
      minMasterVersion = "1.30.4-gke.1348000",
      nodeVersion = "1.30.4-gke.1348000",
      nodeConfig = gcp.container.inputs.ClusterNodeConfigArgs(
        diskSizeGb = Some(20),
        imageType = "ubuntu_containerd",
        machineType = "e2-medium",
        oauthScopes = List(
          "https://www.googleapis.com/auth/compute",
          "https://www.googleapis.com/auth/devstorage.read_only",
          "https://www.googleapis.com/auth/logging.write",
          "https://www.googleapis.com/auth/monitoring"
        ),
        spot = true
      )
    ),
    opts = opts(dependsOn = kubernetesEngine)
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

  val sqlInstance = gcp.sql.DatabaseInstance(
    name = "test-instance-1919810",
    args = DatabaseInstanceArgs(
      databaseVersion = "POSTGRES_16",
      instanceType = "CLOUD_SQL_INSTANCE",
      maintenanceVersion = "POSTGRES_16_4.R20240910.01_02",
      region = "us-central1",
      rootPassword = "123456",
      settings = DatabaseInstanceSettingsArgs(
        activationPolicy = "ALWAYS",
        availabilityType = "ZONAL",
        connectorEnforcement = "NOT_REQUIRED",
        diskSize = 10,
        diskType = "PD_SSD",
        deletionProtectionEnabled = true,
        edition = "ENTERPRISE",
        locationPreference = DatabaseInstanceSettingsLocationPreferenceArgs(
          zone = "us-central1-c"
        ),
        tier = "db-f1-micro"
      )
    )
  )

  val databaseInstance = gcp.sql.Database(
    name = "realworld",
    args = gcp.sql.DatabaseArgs(
      instance = sqlInstance.name
    ),
    opts = opts(dependsOn = sqlInstance)
  )

  val dbUser = gcp.sql.User(
    name = "root",
    args = UserArgs(
      instance = sqlInstance.name,
      password = config.require("SC_POSTGRES_PASSWORD")
    ),
    opts = opts(dependsOn = sqlInstance)
  )

  val redis = Redis("redis", options = CustomResourceOptions(provider = gkeProvider))

  Stack(k8sCluster, redis, databaseInstance, dbUser).exports(
  )
}

def realworldService(
  using Context
)(
  instanceName: NonEmptyString,
  cloudSqlInstance: NonEmptyString,
  appImageTag: NonEmptyString,
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
                  )
                )
              ),
              ContainerArgs(
                args = List(s"--instances=$cloudSqlInstance=tcp:5432"),
                name = "cloud-sql-proxy",
                image = "gcr.io/cloudsql-docker/gce-proxy:1.33.0",
                securityContext = SecurityContextArgs(
                  runAsNonRoot = true
                ),
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu"    -> "100m",
                    "memory" -> "128"
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

def Redis(
  using Context
)(
  instanceName: NonEmptyString,
  options: CustomResourceOptions = CustomResourceOptions()
): Output[Service] =
  val labels = Map("app" -> instanceName)

  val redisPortName   = "containerPort"
  val redisPortNumber = 6379

  val deployment = Deployment(
    instanceName,
    DeploymentArgs(
      metadata = ObjectMetaArgs(
        name = instanceName,
        labels = labels
      ),
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(
          matchLabels = labels
        ),
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            name = instanceName,
            labels = labels
          ),
          spec = PodSpecArgs(
            containers = List(
              ContainerArgs(
                name = "redis",
                image = "redis:latest",
                ports = List(
                  ContainerPortArgs(
                    name = redisPortName,
                    containerPort = redisPortNumber
                  )
                ),
                readinessProbe = ProbeArgs(
                  exec = ExecActionArgs(
                    List("redis-cli", "ping")
                  ),
                  initialDelaySeconds = 1,
                  periodSeconds = 1,
                  timeoutSeconds = 3,
                  failureThreshold = 30
                ),
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu"    -> "100m",
                    "memory" -> "100Mi"
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
            appProtocol = "TCP",
            name = redisPortName,
            port = redisPortNumber,
            targetPort = redisPortNumber
          )
        ),
        selector = deployment.spec.template.metadata.labels
      )
    ),
    opts = options
  )
end Redis
