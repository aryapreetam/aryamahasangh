config.watchOptions = config.watchOptions || {
  ignored: ["**/*.kt", "**/node_modules"]
}

if (config.devServer) {
  config.devServer.static = config.devServer.static.map(file => {
    if (typeof file === "string") {
      return {
        directory: file,
        watch: false,
      }
    } else {
      return file
    }
  })
}
