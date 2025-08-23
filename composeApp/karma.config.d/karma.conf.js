config.set({
    // Use Chrome headless for CI/CD compatibility
    browsers: ['ChromeHeadless'],

    // Enable source maps for better debugging
    preprocessors: {
        '**/*.js': ['sourcemap']
    },

    // Configure Chrome flags for better testing
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

    // Increase timeout for complex UI tests
    captureTimeout: 120000,
    browserDisconnectTimeout: 120000,
    browserDisconnectTolerance: 3,
    browserNoActivityTimeout: 120000,

    // Configure reporters
    reporters: ['progress'],

    // Log level for debugging
    logLevel: config.LOG_INFO,

    // Single run for CI/CD
    singleRun: true,

    // Concurrency level
    concurrency: 1,

    // Client configuration for better error reporting and test filtering
    client: {
        captureConsole: true,
        clearContext: false,
        runInParent: false,
        useIframe: true,
        mocha: {
            // Use KARMA_MOCHA_GREP env to filter tests, e.g. 'CounterScreenTest'
            grep: process.env.KARMA_MOCHA_GREP || undefined,
            timeout: 60000
        }
    }
});
