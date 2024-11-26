import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import injectHtmlVarsPlugin from "./vite-plugins/inject-html-vars.js";
import rollupPluginSourcemaps from "rollup-plugin-sourcemaps";
import globResolverPlugin from "@raquo/vite-plugin-glob-resolver";
import importSideEffectPlugin from "@raquo/vite-plugin-import-side-effect";

// const packageJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, './package.json'), 'utf-8'))
// const dependencies = Object.keys(packageJson.dependencies)
// const alias = dependencies.reduce((aliases, dependency) => {
//   aliases[dependency] = path.resolve(__dirname, `./node_modules/${dependency}`)
//   return aliases;
// }, {});

export default defineConfig({
  // root: path.resolve(__dirname, './../modules/frontend/target/modules-js-3/'),
  // resolve: {
  //   alias
  // },
  base: "/",
  publicDir: "public",
  plugins: [
    scalaJSPlugin({
      cwd: "../..", // path to build.sbt
      projectID: "frontend" // scala.js project name in build.sbt
    }),
    globResolverPlugin({
      // See https://github.com/raquo/vite-plugin-glob-resolver
      cwd: __dirname,
      ignore: [
        'node_modules/**',
        'target/**'
      ]
    }),
    importSideEffectPlugin({
      // See https://github.com/raquo/vite-plugin-import-side-effect
      defNames: ['importStyle'],
      rewriteModuleIds: ['**/*.less', '**/*.css', "**/*.scss"],
      verbose: true
    }),
    injectHtmlVarsPlugin({
      SCRIPT_URL: './index.js'
    })
  ],
  build: {
    outDir: "dist",
    assetsDir: "assets",
    cssCodeSplit: false,
    minify: "terser",
    sourcemap: false
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      }
    }
  },
}
)