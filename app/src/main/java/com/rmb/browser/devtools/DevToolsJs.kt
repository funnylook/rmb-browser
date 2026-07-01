package com.rmb.browser.devtools

/**
 * JavaScript code injected into WebViews to intercept console, network, and DOM.
 * All JS is injected via WebView.addJavascriptInterface + evaluateJavascript.
 */
object DevToolsJs {

    /**
     * Install console interceptor.
     * Calls DevToolsBridge.onConsole(level, args) for every console method.
     */
    const val CONSOLE_INTERCEPTOR = """
    (function() {
        if (window.__devtools_console_installed) return;
        window.__devtools_console_installed = true;
        
        var methods = ['log', 'info', 'warn', 'error', 'debug', 'dir', 'table', 'trace'];
        methods.forEach(function(method) {
            var orig = console[method];
            console[method] = function() {
                try {
                    var args = Array.prototype.slice.call(arguments);
                    var msg = args.map(function(a) {
                        if (a === null) return 'null';
                        if (a === undefined) return 'undefined';
                        if (typeof a === 'object') {
                            try { return JSON.stringify(a, null, 2); }
                            catch(e) { return String(a); }
                        }
                        return String(a);
                    }).join(' ');
                    DevToolsBridge.onConsole(method.toUpperCase(), msg);
                } catch(e) {}
                if (orig && orig.apply) orig.apply(console, arguments);
            };
        });
        
        // Intercept uncaught errors
        window.addEventListener('error', function(e) {
            DevToolsBridge.onConsole('ERROR', e.message + ' (' + e.filename + ':' + e.lineno + ')');
        });
        window.addEventListener('unhandledrejection', function(e) {
            DevToolsBridge.onConsole('ERROR', 'Unhandled Promise Rejection: ' + (e.reason || ''));
        });
    })();
    """

    /**
     * Install network interceptor.
     * Intercepts fetch() and XMLHttpRequest.
     * Calls DevToolsBridge.onNetworkStart/End/Response.
     */
    const val NETWORK_INTERCEPTOR = """
    (function() {
        if (window.__devtools_network_installed) return;
        window.__devtools_network_installed = true;
        
        // --- Fetch interceptor ---
        var origFetch = window.fetch;
        window.fetch = function(input, init) {
            var url = typeof input === 'string' ? input : (input.url || '');
            var method = (init && init.method) || 'GET';
            var body = (init && init.body) || '';
            if (typeof body !== 'string') {
                try { body = JSON.stringify(body); } catch(e) { body = String(body); }
            }
            var id = 'f_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);
            
            DevToolsBridge.onNetworkStart(id, method, url, body, 'FETCH');
            
            return origFetch.apply(this, arguments).then(function(response) {
                var status = response.status;
                var statusText = response.statusText;
                var headers = {};
                response.headers.forEach(function(v, k) { headers[k] = v; });
                
                // Clone to read body without consuming
                var clone = response.clone();
                clone.text().then(function(text) {
                    DevToolsBridge.onNetworkEnd(id, status, statusText, JSON.stringify(headers), text.substring(0, 50000));
                }).catch(function() {
                    DevToolsBridge.onNetworkEnd(id, status, statusText, JSON.stringify(headers), '');
                });
                
                return response;
            }).catch(function(err) {
                DevToolsBridge.onNetworkEnd(id, 0, 'Failed', '{}', err.message || 'Network error');
                throw err;
            });
        };
        
        // --- XHR interceptor ---
        var OrigXHR = window.XMLHttpRequest;
        window.XMLHttpRequest = function() {
            var xhr = new OrigXHR();
            var id = 'x_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);
            var method, url, body;
            
            var origOpen = xhr.open;
            xhr.open = function(m, u) {
                method = m; url = u;
                return origOpen.apply(xhr, arguments);
            };
            
            var origSend = xhr.send;
            xhr.send = function(b) {
                body = b || '';
                DevToolsBridge.onNetworkStart(id, method || 'GET', url || '', typeof body === 'string' ? body : '', 'XHR');
                return origSend.apply(xhr, arguments);
            };
            
            xhr.addEventListener('load', function() {
                var headers = {};
                var raw = xhr.getAllResponseHeaders();
                raw.split('\r\n').forEach(function(line) {
                    var parts = line.split(': ');
                    if (parts.length === 2) headers[parts[0]] = parts[1];
                });
                DevToolsBridge.onNetworkEnd(id, xhr.status, xhr.statusText, JSON.stringify(headers), (xhr.responseText || '').substring(0, 50000));
            });
            
            xhr.addEventListener('error', function() {
                DevToolsBridge.onNetworkEnd(id, 0, 'Error', '{}', 'Network error');
            });
            
            return xhr;
        };
    })();
    """

    /**
     * Install element inspector.
     * Tap any element → sends info back via DevToolsBridge.onElementInspect.
     */
    const val ELEMENT_INSPECTOR = """
    (function() {
        if (window.__devtools_element_installed) return;
        window.__devtools_element_installed = true;
        
        window.__devtools_inspect_mode = false;
        
        function getElementInfo(el) {
            if (!el) return '{}';
            var computed = window.getComputedStyle(el);
            var styles = {};
            var important = ['display','position','width','height','margin','padding',
                'background','color','font-size','font-family','border','overflow',
                'z-index','opacity','transform','flex','grid'];
            important.forEach(function(p) {
                styles[p] = computed.getPropertyValue(p);
            });
            
            var attrs = {};
            for (var i = 0; i < el.attributes.length; i++) {
                attrs[el.attributes[i].name] = el.attributes[i].value;
            }
            
            // Generate CSS selector
            function getSelector(el) {
                if (el.id) return '#' + el.id;
                var path = [];
                while (el && el.nodeType === 1) {
                    var name = el.tagName.toLowerCase();
                    if (el.id) { path.unshift('#' + el.id); break; }
                    var sibling = el, nth = 1;
                    while (sibling = sibling.previousElementSibling) {
                        if (sibling.tagName === el.tagName) nth++;
                    }
                    path.unshift(name + ':nth-of-type(' + nth + ')');
                    el = el.parentElement;
                }
                return path.join(' > ');
            }
            
            return JSON.stringify({
                tag: el.tagName.toLowerCase(),
                id: el.id || '',
                class: el.className || '',
                text: (el.textContent || '').substring(0, 200),
                attrs: attrs,
                styles: styles,
                selector: getSelector(el),
                html: el.outerHTML.substring(0, 5000)
            });
        }
        
        function getElementTree(el, depth) {
            if (!el || depth > 5) return null;
            var info = {
                tag: el.tagName ? el.tagName.toLowerCase() : '#text',
                id: el.id || '',
                class: el.className || '',
                text: el.nodeType === 3 ? (el.textContent || '').substring(0, 50) : '',
                children: []
            };
            for (var i = 0; i < el.children.length && i < 30; i++) {
                var child = getElementTree(el.children[i], depth + 1);
                if (child) info.children.push(child);
            }
            return info;
        }
        
        // Highlight overlay
        var overlay = document.createElement('div');
        overlay.id = '__devtools_overlay';
        overlay.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483646;border:2px solid #2196F3;background:rgba(33,150,243,0.1);transition:all 0.1s;';
        document.body.appendChild(overlay);
        
        function showOverlay(el) {
            var rect = el.getBoundingClientRect();
            overlay.style.display = 'block';
            overlay.style.left = rect.left + 'px';
            overlay.style.top = rect.top + 'px';
            overlay.style.width = rect.width + 'px';
            overlay.style.height = rect.height + 'px';
        }
        
        window.__devtools_enableInspect = function() {
            window.__devtools_inspect_mode = true;
            overlay.style.display = 'block';
            document.addEventListener('mouseover', onHover, true);
            document.addEventListener('click', onClick, true);
            document.addEventListener('touchstart', onTouch, true);
        };
        
        window.__devtools_disableInspect = function() {
            window.__devtools_inspect_mode = false;
            overlay.style.display = 'none';
            document.removeEventListener('mouseover', onHover, true);
            document.removeEventListener('click', onClick, true);
            document.removeEventListener('touchstart', onTouch, true);
        };
        
        function onHover(e) {
            if (!window.__devtools_inspect_mode) return;
            e.preventDefault();
            e.stopPropagation();
            showOverlay(e.target);
        }
        
        function onClick(e) {
            if (!window.__devtools_inspect_mode) return;
            e.preventDefault();
            e.stopPropagation();
            var info = getElementInfo(e.target);
            var tree = getElementTree(e.target, 0);
            DevToolsBridge.onElementInspect(info, JSON.stringify(tree));
            window.__devtools_disableInspect();
        }
        
        function onTouch(e) {
            if (!window.__devtools_inspect_mode) return;
            e.preventDefault();
            e.stopPropagation();
            var touch = e.touches[0];
            var el = document.elementFromPoint(touch.clientX, touch.clientY);
            if (el) {
                showOverlay(el);
                var info = getElementInfo(el);
                var tree = getElementTree(el, 0);
                DevToolsBridge.onElementInspect(info, JSON.stringify(tree));
            }
            window.__devtools_disableInspect();
        }
        
        // Evaluate JS in page context
        window.__devtools_eval = function(code) {
            try {
                var result = eval(code);
                return typeof result === 'object' ? JSON.stringify(result, null, 2) : String(result);
            } catch(e) {
                return 'Error: ' + e.message;
            }
        };
    })();
    """
}
