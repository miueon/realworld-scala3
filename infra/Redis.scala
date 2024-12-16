package realworld.infra

import Util.*
import besom.*
import besom.aliases.ComponentResourceOptions
import besom.aliases.NonEmptyString
import besom.api.kubernetes
import besom.api.kubernetes.apps.v1.*
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.core
import besom.api.kubernetes.core.v1.*
import besom.api.kubernetes.core.v1.enums.*
import besom.api.kubernetes.core.v1.inputs.ContainerArgs
import besom.api.kubernetes.core.v1.inputs.ContainerPortArgs
import besom.api.kubernetes.core.v1.inputs.EnvVarArgs
import besom.api.kubernetes.core.v1.inputs.ExecActionArgs
import besom.api.kubernetes.core.v1.inputs.PodSpecArgs
import besom.api.kubernetes.core.v1.inputs.PodTemplateSpecArgs
import besom.api.kubernetes.core.v1.inputs.ProbeArgs
import besom.api.kubernetes.core.v1.inputs.ResourceRequirementsArgs
import besom.api.kubernetes.core.v1.inputs.ServicePortArgs
import besom.api.kubernetes.core.v1.inputs.ServiceSpecArgs
import besom.api.kubernetes.core.v1.inputs.VolumeArgs
import besom.api.kubernetes.core.v1.inputs.VolumeMountArgs
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*
import besom.internal.ComponentBase
import besom.internal.ComponentResource
import besom.internal.Context
import besom.internal.Input
import besom.internal.Output
import besom.internal.RegistersOutputs

case class RedisEndpoint(url: Output[String], fqdn: Output[String])
derives Encoder
object RedisEndpoint:
  extension (r: Output[RedisEndpoint])
    def url: Output[String]  = r.flatMap(_.url)
    def fqdn: Output[String] = r.flatMap(_.fqdn)

case class Redis private (
  instanceEdp: Output[RedisEndpoint]
)(using ComponentBase
)
extends ComponentResource
derives RegistersOutputs
object Redis:
  extension (r: Output[Redis])
    def edp: Output[RedisEndpoint] = r.flatMap(_.instanceEdp)

  def apply(
    using Context
  )(
    name: NonEmptyString,
    options: ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Redis] =
    component(name, "besom:example:Redis", options) {
      val namespace                    = Namespace(s"redis-$name")
      val instanceName: NonEmptyString = "redis"
      val labels                       = Map("app" -> instanceName)

      val redisPortName   = "containerPort"
      val redisPortNumber = 6379

      val instance = Deployment(
        instanceName,
        DeploymentArgs(
          metadata = ObjectMetaArgs(
            name = instanceName,
            namespace = namespace.metadata.name,
            labels = labels
          ),
          spec = DeploymentSpecArgs(
            selector = LabelSelectorArgs(
              matchLabels = labels
            ),
            template = PodTemplateSpecArgs(
              metadata = ObjectMetaArgs(
                name = instanceName,
                namespace = namespace.metadata.name,
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

      val service = Service(
        instanceName,
        ServiceArgs(
          metadata = ObjectMetaArgs(
            name = instanceName,
            labels = labels,
            namespace = namespace.metadata.name
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
            selector = instance.spec.template.metadata.labels
          )
        )
      )

      new Redis(
        instanceEdp = Output(
          RedisEndpoint(
            url =
              p"redis://${serviceFqdn(service, namespace)}:$redisPortNumber",
            fqdn = serviceFqdn(service, namespace)
          )
        )
      )
    }
end Redis
