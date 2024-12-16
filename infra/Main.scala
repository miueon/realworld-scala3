import besom.*
import besom.api.gcp
import besom.api.gcp.sql.DatabaseInstanceArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsArgs
import besom.api.gcp.sql.inputs.DatabaseInstanceSettingsLocationPreferenceArgs
import besom.api.gcp.storage.BucketArgs
import realworld.infra.Redis
import besom.api.gcp.sql.UserArgs
import besom.api.kubernetes
import besom.api.kubernetes.{ProviderArgs => kProviderArgs}

@main def main = Pulumi.run {
  val bucket = gcp.storage.Bucket("my-bucket", BucketArgs(location = "US"))

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

  val redis = Redis("redis", options = ComponentResourceOptions(providers = gkeProvider))

  Stack(k8sCluster, redis, databaseInstance, dbUser).exports(
    bucketName = bucket.url // Export the DNS name of the bucket
  )
}
