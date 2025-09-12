module.exports = function (config) {
    // IMPORTANT: Do NOT override frameworks/files added by Kotlin plugin.
    // Only tweak safe options.
    config.set({
        // Prefer ChromeHeadless unless overridden by the plugin
        browsers: ['ChromeHeadless'],

        preprocessors: {
            '**/*.js': ['sourcemap']
        },

        customLaunchers: {
            ChromeHeadlessNoSandbox: {
                base: 'ChromeHeadless',
                flags: [
                    '--no-sandbox',
                    '--disable-setuid-sandbox',
                    '--disable-extensions',
                    '--disable-gpu',
                    '--disable-dev-shm-usage',
                    '--disable-web-security',
                    '--allow-running-insecure-content',
                    '--disable-features=TranslateUI',
                    '--disable-ipc-flooding-protection'
                ]
            }
        },

        captureTimeout: 120000,
        browserDisconnectTimeout: 120000,
        browserDisconnectTolerance: 3,
        browserNoActivityTimeout: 120000,

        // Keep progress reporter; do not remove plugin defaults
        reporters: (config.reporters || []).concat(['progress']),

        logLevel: config.LOG_INFO,
        singleRun: true,
        concurrency: 1,

        client: {
            captureConsole: true,
            clearContext: false,
            runInParent: false,
            useIframe: true,
            mocha: {
                grep: process.env.KARMA_MOCHA_GREP || undefined,
                timeout: 60000
            }
        }
    });
};
