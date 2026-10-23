(function (window, document) {
    'use strict';

    var meta = document.querySelector('meta[name="app-context-path"]');
    var configuredPath = meta ? meta.content : '/';
    var contextPath = String(configuredPath || '/')
        .replace(/\/+$/, '')
        .replace(/^([^/])/, '/$1');

    function url(path) {
        var target = String(path || '');

        if (/^(?:[a-z][a-z\d+.-]*:)?\/\//i.test(target)) {
            return target;
        }

        if (!target || target === '/') {
            return (contextPath || '') + '/';
        }

        if (contextPath && (target === contextPath || target.indexOf(contextPath + '/') === 0)) {
            return target;
        }

        return (contextPath || '') + '/' + target.replace(/^\/+/, '');
    }

    window.SGComplaint = Object.freeze({
        contextPath: contextPath,
        url: url
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-contextual-content]').forEach(function (container) {
            container.querySelectorAll('a[href^="/"], img[src^="/"], source[src^="/"]').forEach(function (element) {
                var attribute = element.hasAttribute('href') ? 'href' : 'src';
                element.setAttribute(attribute, url(element.getAttribute(attribute)));
            });
        });
    });
})(window, document);
