package Util
import besom.*
import besom.api.kubernetes
import besom.api.kubernetes.core
import besom.api.kubernetes.core.v1.*
import besom.api.kubernetes.meta.v1.*
import besom.internal.Context
import besom.internal.Output

def serviceFqdn(service: Output[Service], namespace: Output[Namespace])(using Context): Output[String] =
  val serviceName = service.metadata.name.getOrFail(
    Exception("expected service name to be defined")
  )
  val namespaceName = namespace.metadata.name.getOrFail(
    Exception("expected namespace name to be defined")
  )
  p"${serviceName}.${namespaceName}.svc.cluster.local"
