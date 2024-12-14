import besom.*
import besom.api.gcp
import besom.api.gcp.storage.BucketArgs
import realworld.infra.Redis

@main def main = Pulumi.run {
  val bucket = gcp.storage.Bucket("my-bucket", BucketArgs(location = "US"))

  val redis = Redis("my-redis")

  Stack.exports(
    bucketName = bucket.url // Export the DNS name of the bucket
  )
}
