/* 后端接口封装：统一 /api 前缀、JWT 头、Result 拆包、401 跳登录 */
window.API = (function () {
    var BASE = (function () {
        var p = window.location.pathname;
        var i = p.lastIndexOf('/');
        var base = i >= 0 ? p.substring(0, i) : '';
        return base.replace(/\/$/, '');
    })();

    var TOKEN_KEY = 'srb_token';
    var USER_KEY = 'srb_user';

    function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }
    function getUser() {
        try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch (e) { return null; }
    }
    function saveSession(data) {
        localStorage.setItem(TOKEN_KEY, data.token || '');
        localStorage.setItem(USER_KEY, JSON.stringify({
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            role: data.role,
            avatar: data.avatar
        }));
    }
    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }
    function toLogin() {
        clearSession();
        window.location.href = 'index.html';
    }

    function buildUrl(path, params) {
        var url = BASE + path;
        if (params) {
            var qs = [];
            Object.keys(params).forEach(function (k) {
                var v = params[k];
                if (v === null || v === undefined || v === '') return;
                qs.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
            });
            if (qs.length) url += (url.indexOf('?') >= 0 ? '&' : '?') + qs.join('&');
        }
        return url;
    }

    async function request(path, opt) {
        opt = opt || {};
        var headers = { 'Accept': 'application/json' };
        var token = getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;
        var init = { method: opt.method || 'GET', headers: headers };
        if (opt.body !== undefined && opt.body !== null) {
            headers['Content-Type'] = 'application/json';
            init.body = typeof opt.body === 'string' ? opt.body : JSON.stringify(opt.body);
        }

        var resp;
        try {
            resp = await fetch(buildUrl(path, opt.params), init);
        } catch (e) {
            throw { code: -1, message: '网络异常，请确认服务是否已启动' };
        }

        if (resp.status === 401) {
            toLogin();
            throw { code: 401, message: '登录已失效，请重新登录' };
        }

        var data = null;
        var text = await resp.text();
        if (text) {
            try { data = JSON.parse(text); } catch (e) { data = null; }
        }

        if (!data) {
            if (resp.ok) return null;
            throw { code: resp.status, message: 'HTTP ' + resp.status };
        }
        if (data.code !== 200) {
            throw { code: data.code, message: data.message || '请求失败' };
        }
        return data.data;
    }

    return {
        BASE: BASE,
        get: function (p, params) { return request(p, { params: params }); },
        post: function (p, body, params) { return request(p, { method: 'POST', body: body, params: params }); },
        put: function (p, body, params) { return request(p, { method: 'PUT', body: body, params: params }); },
        del: function (p, params) { return request(p, { method: 'DELETE', params: params }); },
        token: getToken,
        user: getUser,
        saveSession: saveSession,
        clearSession: clearSession,
        toLogin: toLogin,
        hasLogin: function () { return !!getToken(); }
    };
})();
