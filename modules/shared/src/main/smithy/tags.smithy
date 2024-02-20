$version: "2.0"

metadata suppressions = [{
    id: "HttpHeaderTrait"
    namespace: "realworld.spec"
    reason: ""
}]
namespace realworld.spec

use alloy#simpleRestJson
use alloy#uuidFormat
use alloy.common#emailFormat
// SERVICES

@simpleRestJson
@httpBearerAuth
service TagService {
    version: "1.0.0"
    operations: [ListTag]
}

@http(method: "GET", uri: "/api/tags", code: 200)
operation ListTag {
    output := {
        @required
        tags: TagList
    }
}
