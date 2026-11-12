/* 应用外壳：路由、导航、会话 */
window.App = (function () {
    var ROUTES = {
        home: { title: '学习概览', crumb: '今天也要专注一点' },
        rooms: { title: '自习室预约', crumb: '按楼栋与楼层选择座位' },
        bookings: { title: '我的预约', crumb: '签到、离座与取消' },
        study: { title: '学习计时', crumb: '记录每一次专注' },
        stats: { title: '数据统计', crumb: '每日 / 每周学习时长与热力图' },
        goals: { title: '学习目标', crumb: '目标进度与连续打卡成就' },
        ranking: { title: '排行榜', crumb: '学习时长排名与好友监督' },
        admin: { title: '管理后台', crumb: '房间座位管理、违规与使用率' }
    };

    var NAV = {
        home: { label: '学习概览', icon: 'home' },
        rooms: { label: '自习室预约', icon: 'rooms' },
        bookings: { label: '我的预约', icon: 'booking' },
        study: { label: '学习计时', icon: 'timer' },
        stats: { label: '数据统计', icon: 'chart' },
        goals: { label: '学习目标', icon: 'target' },
        ranking: { label: '排行榜', icon: 'trophy' },
        admin: { label: '管理后台', icon: 'admin' }
    };

    var current = 'home';

    function initNav() {
        document.querySelectorAll('.nav-item').forEach(function (item) {
            var key = item.dataset.route;
            var meta = NAV[key];
            if (!meta) return;
            item.innerHTML = UI.icon(meta.icon) + '<span>' + meta.label + '</span>';
        });
        var logout = document.getElementById('logoutBtn');
        logout.innerHTML = UI.icon('logout');
        document.getElementById('menuBtn').innerHTML = UI.icon('menu');

        var user = API.user() || {};
        document.getElementById('sideName').textContent = user.nickname || user.username || '未登录';
        document.getElementById('sideAvatar').textContent = UI.avatarText(user.nickname || user.username);
        document.getElementById('sideRole').textContent = user.role === 1 ? '管理员' : '学生';
        if (user.role === 1) {
            document.getElementById('adminNavGroup').style.display = '';
        }
    }

    function setActive(key) {
        document.querySelectorAll('.nav-item').forEach(function (item) {
            item.classList.toggle('active', item.dataset.route === key);
        });
        var meta = ROUTES[key] || ROUTES.home;
        document.getElementById('pageTitle').textContent = meta.title;
        document.getElementById('pageCrumb').textContent = meta.crumb;
    }

    function render() {
        var hash = window.location.hash.replace(/^#\/?/, '');
        var key = hash.split('?')[0] || 'home';
        var page = window.Pages && window.Pages[key];
        if (!page) {
            key = 'home';
            page = window.Pages.home;
        }
        current = key;
        setActive(key);
        document.getElementById('sidebar').classList.remove('open');
        var view = document.getElementById('view');
        view.innerHTML = '';
        try {
            page.render(view, { key: key });
        } catch (e) {
            console.error(e);
            view.innerHTML = UI.empty('页面加载失败', String(e && e.message ? e.message : e));
        }
        window.scrollTo({ top: 0 });
    }

    function go(key) {
        if (('#' + '/' + key) === window.location.hash) render();
        else window.location.hash = '#/' + key;
    }

    function clock() {
        var d = new Date();
        var wk = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
        var p = function (n) { return n < 10 ? '0' + n : n; };
        document.getElementById('clock').textContent =
            d.getMonth() + 1 + '月' + d.getDate() + '日 周' + wk + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (!API.hasLogin()) { API.toLogin(); return; }
        initNav();
        document.getElementById('logoutBtn').addEventListener('click', function () {
            UI.confirm('确定要退出当前账号吗？', { okText: '退出' }).then(function (ok) {
                if (ok) API.toLogin();
            });
        });
        document.getElementById('menuBtn').addEventListener('click', function () {
            document.getElementById('sidebar').classList.toggle('open');
        });
        if (!window.location.hash) window.location.hash = '#/home';
        window.addEventListener('hashchange', render);
        clock();
        setInterval(clock, 20000);
        render();
    });

    return { go: go, reload: render, current: function () { return current; }, user: API.user };
})();
