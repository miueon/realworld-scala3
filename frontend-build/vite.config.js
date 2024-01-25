/**
 * @type {import('vite').UserConfig}
 */

import { defineConfig } from "vite";
import path from 'path';
// import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import fs from 'fs'

const packageJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, './package.json'), 'utf-8'))
const dependencies = Object.keys(packageJson.dependencies)
const alias = dependencies.reduce((aliases, dependency) => {
  aliases[dependency] = path.resolve(__dirname, `./node_modules/${dependency}`)
  return aliases;
}, {});

export default defineConfig({
  root: path.resolve(__dirname, './../modules/frontend/target/modules-js-3/'),
  resolve: {
    alias
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
   
  },
  // plugins: [scalaJSPlugin({
  //   cwd: "..",
  //   uriPrefix: "modules-js-3"
  // })]
}
)