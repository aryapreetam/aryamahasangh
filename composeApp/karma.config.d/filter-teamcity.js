// Inject a runtime script to filter out TeamCity service messages from console
// to avoid confusing Gradle's TC reporter during wasmJsBrowserTest
module.exports = function (config) {
    config.files = config.files || [];
    config.files.push({
        pattern: __dirname + '/patch-console-runtime.js',
        included: true,
        served: true,
        watched: false,
    });
};
