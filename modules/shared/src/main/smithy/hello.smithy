$version: "2.0"

namespace realworld.spec

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Get]
}

@readonly
@http(method: "GET", uri: "/api/hello-world", code: 200)
operation Get{
  output: HelloWorldResponse
}



structure HelloWorldResponse{
  @required
  message: String
}