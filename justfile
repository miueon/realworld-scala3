dev:
  process-compose up

dev-js:
  cd modules/frontend; pnpm dev

dev-scala-js:
  sbt --client '~frontend/fastLinkJS'

dev-scala:
  sbt --client '~app/reStart'

build-and-copy-frontend:
  sbt --client 'buildAndCopyFrontend'

install-frontend:
  cd modules/frontend; pnpm i --frozen-lockfile --no-verify-store-integrity

test:
  sbt --client 'backend/test'

publish-docker:
  sbt --client 'publishDocker'
