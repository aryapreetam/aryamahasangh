(function () {
    function isTeamCityMessage(arg) {
        try {
            return typeof arg === 'string' && arg.startsWith('##teamcity[');
        } catch (e) {
            return false;
        }
    }

    function wrap(orig) {
        return function () {
            if (arguments.length > 0 && isTeamCityMessage(arguments[0])) {
                return; // suppress TC service messages to avoid Gradle parser conflicts
            }
            return orig.apply(console, arguments);
        };
    }

    console.log = wrap(console.log);
    console.info = wrap(console.info);
    console.warn = wrap(console.warn);
    console.error = wrap(console.error);
})();
