dev:
  process-compose up

dev-js:
  cd modules/frontend; pnpm dev

dev-scala-js:
  sbt ~frontend/fastLinkJS

dev-scala:
  sbt ~app/reStart

build-frontend:
  sbt buildFrontend

install-frontend:
  cd modules/frontend; pnpm i --frozen-lockfile --no-verify-store-integrity

test:
  sbt backend/test

publish-docker:
  sbt publishDocker
