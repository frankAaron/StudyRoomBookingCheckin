/* 通用 UI：Toast、Modal、格式化、状态映射、轻量图表 */
window.UI = (function () {
    var ICONS = {
        home: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/>',
        rooms: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
        booking: '<rect x="3" y="4.5" width="18" height="16" rx="2"/><path d="M8 3v3M16 3v3M3 9.5h18"/>',
        timer: '<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2"/><path d="M9 2h6"/>',
        chart: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
        target: '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/><circle cx="12" cy="12" r="1"/>',
        trophy: '<path d="M8 4h8v5a4 4 0 0 1-8 0V4Z"/><path d="M8 5H5v2a3 3 0 0 0 3 3M16 5h3v2a3 3 0 0 1-3 3"/><path d="M12 13v4M9 20h6"/>',
        admin: '<path d="M12 3l8 3v6c0 4.5-3.2 7.8-8 9-4.8-1.2-8-4.5-8-9V6l8-3Z"/><path d="M9.5 12.5l1.8 1.8 3.4-3.6"/>',
        logout: '<path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3"/><path d="M10 8l-4 4 4 4M6 12h10"/>',
        menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
        seat: '<path d="M6 4v9a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4"/><path d="M6 15H4v5M18 15h2v5"/>',
        users: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5"/><path d="M16 5.2a3.2 3.2 0 0 1 0 6.1M18 14.6c2 .7 3 2.4 3 5.4"/>',
        warn: '<path d="M12 3l9.5 17H2.5L12 3Z"/><path d="M12 9.5v4M12 17h.01"/>',
        scan: '<path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"/><path d="M4 12h16"/>',
        clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>',
        fire: '<path d="M12 3c3 4 5 6 5 9a5 5 0 0 1-10 0c0-2 1-3 2-4 .4 2 1.5 2.6 2 1.5.6-1.3-.5-3.5 1-6.5Z"/>'
    };

    function icon(name, cls) {
        var p = ICONS[name] || '';
        return '<svg class="' + (cls || '') + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">' + p + '</svg>';
    }

    function esc(s) {
        if (s === null || s === undefined) return '';
        return String(s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    function pad(n) { return n < 10 ? '0' + n : '' + n; }

    function parseDate(v) {
        if (!v) return null;
        if (v instanceof Date) return v;
        var s = String(v).replace(' ', 'T');
        var d = new Date(s);
        return isNaN(d.getTime()) ? null : d;
    }

    function fmtDateTime(v) {
        var d = parseDate(v);
        if (!d) return '--';
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function fmtDate(v) {
        var d = parseDate(v);
        if (!d) return '--';
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    }

    function fmtTime(v) {
        var d = parseDate(v);
        if (!d) return '--';
        return pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function fmtMinutes(m) {
        m = Number(m || 0);
        if (m < 60) return m + ' 分钟';
        var h = Math.floor(m / 60), r = m % 60;
        return r ? h + ' 小时 ' + r + ' 分' : h + ' 小时';
    }

    function hours(m) { return (Number(m || 0) / 60).toFixed(1); }

    var SEAT_STATUS = {
        0: { label: '空闲', cls: 'ok' },
        1: { label: '使用中', cls: 'danger' },
        2: { label: '已预约', cls: 'warn' },
        3: { label: '维护中', cls: '' }
    };

    var BOOKING_STATUS = {
        0: { label: '待签到', cls: 'warn' },
        1: { label: '学习中', cls: 'ok' },
        2: { label: '已完成', cls: 'info' },
        3: { label: '已取消', cls: '' },
        4: { label: '已过期', cls: 'danger' }
    };

    var CHECKIN_STATUS = {
        0: { label: '进行中', cls: 'ok' },
        1: { label: '已结束', cls: 'info' },
        2: { label: '暂离', cls: 'warn' }
    };

    var GOAL_STATUS = {
        0: { label: '进行中', cls: 'warn' },
        1: { label: '已达成', cls: 'ok' },
        2: { label: '已放弃', cls: '' }
    };

    function badge(map, key) {
        var s = map[key] || { label: '未知', cls: '' };
        return '<span class="badge dot ' + s.cls + '">' + s.label + '</span>';
    }

    function toast(msg, type) {
        var wrap = document.querySelector('.toast-wrap');
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.className = 'toast-wrap';
            document.body.appendChild(wrap);
        }
        var el = document.createElement('div');
        el.className = 'toast ' + (type || '');
        el.textContent = msg;
        wrap.appendChild(el);
        setTimeout(function () {
            el.style.transition = 'opacity .2s, transform .2s';
            el.style.opacity = '0';
            el.style.transform = 'translateX(12px)';
            setTimeout(function () { el.remove(); }, 220);
        }, type === 'err' ? 3600 : 2400);
    }

    function modal(opt) {
        opt = opt || {};
        var mask = document.createElement('div');
        mask.className = 'modal-mask';
        var box = document.createElement('div');
        box.className = 'modal' + (opt.wide ? ' wide' : '');

        var head = document.createElement('div');
        head.className = 'modal-head';
        head.innerHTML = '<h3>' + esc(opt.title || '') + '</h3><button class="x" type="button">&times;</button>';

        var body = document.createElement('div');
        body.className = 'modal-body';
        if (typeof opt.body === 'string') body.innerHTML = opt.body;
        else if (opt.body) body.appendChild(opt.body);

        box.appendChild(head);
        box.appendChild(body);

        var api = { el: box, body: body, close: close };

        if (opt.footer && opt.footer.length) {
            var foot = document.createElement('div');
            foot.className = 'modal-foot';
            opt.footer.forEach(function (b) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn ' + (b.cls || 'ghost');
                btn.textContent = b.text;
                if (b.disabled) btn.disabled = true;
                btn.addEventListener('click', function () {
                    if (b.onClick) b.onClick(api, btn);
                });
                foot.appendChild(btn);
            });
            box.appendChild(foot);
        }

        function close() {
            mask.style.opacity = '0';
            setTimeout(function () { mask.remove(); }, 150);
        }

        head.querySelector('.x').addEventListener('click', close);
        mask.addEventListener('click', function (e) { if (e.target === mask) close(); });
        mask.appendChild(box);
        document.body.appendChild(mask);
        return api;
    }

    function confirm(text, opt) {
        opt = opt || {};
        return new Promise(function (resolve) {
            var m = modal({
                title: opt.title || '请确认',
                body: '<div class="small" style="line-height:1.8">' + esc(text) + '</div>',
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); resolve(false); } },
                    { text: opt.okText || '确定', cls: opt.danger ? 'danger' : '', onClick: function (x) { x.close(); resolve(true); } }
                ]
            });
            m.el.parentNode.addEventListener('click', function (e) {
                if (e.target === m.el.parentNode) resolve(false);
            });
        });
    }

    function empty(text, sub) {
        return '<div class="empty"><div class="big">' + icon('seat') + '</div>' +
            '<div class="t">' + esc(text || '暂无数据') + '</div>' +
            (sub ? '<div class="small">' + esc(sub) + '</div>' : '') + '</div>';
    }

    function loading(text) {
        return '<div class="empty"><div class="t">' + esc(text || '加载中...') + '</div></div>';
    }

    /* 折线/柱状图（纯 SVG） */
    function barChart(container, data, opt) {
        opt = opt || {};
        var w = 720, h = opt.height || 220;
        var padL = 42, padR = 12, padT = 16, padB = 28;
        var innerW = w - padL - padR, innerH = h - padT - padB;
        var max = Math.max.apply(null, data.map(function (d) { return d.value; }).concat([1]));
        var step = innerW / Math.max(data.length, 1);
        var bw = Math.max(3, Math.min(26, step * 0.58));

        var svg = '<svg class="chart-svg" viewBox="0 0 ' + w + ' ' + h + '" preserveAspectRatio="none">';
        for (var g = 0; g <= 3; g++) {
            var y = padT + innerH - innerH * g / 3;
            svg += '<line x1="' + padL + '" y1="' + y + '" x2="' + (w - padR) + '" y2="' + y + '" stroke="#eef1f7" stroke-width="1"/>';
            svg += '<text x="' + (padL - 8) + '" y="' + (y + 4) + '" text-anchor="end" font-size="10" fill="#98a2b3">' + Math.round(max * g / 3) + '</text>';
        }
        data.forEach(function (d, i) {
            var bh = innerH * (d.value / max);
            var x = padL + step * i + (step - bw) / 2;
            var y = padT + innerH - bh;
            svg += '<rect x="' + x + '" y="' + y + '" width="' + bw + '" height="' + Math.max(bh, d.value > 0 ? 2 : 0) + '" rx="' + Math.min(4, bw / 2) + '">' +
                '<title>' + esc(d.label) + '：' + d.value + ' 分钟</title></rect>';
            if (d.value > 0) {
                svg += '<rect x="' + x + '" y="' + y + '" width="' + bw + '" height="' + Math.max(bh, 2) + '" rx="' + Math.min(4, bw / 2) + '" fill="url(#barGrad)"/>';
            }
            if (i % Math.ceil(data.length / 10) === 0) {
                svg += '<text x="' + (x + bw / 2) + '" y="' + (h - 10) + '" text-anchor="middle" font-size="10" fill="#98a2b3">' + esc(d.label) + '</text>';
            }
        });
        svg += '<defs><linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#6366f1"/><stop offset="100%" stop-color="#a5b4fc"/></linearGradient></defs>';
        svg += '</svg>';
        container.innerHTML = svg;
    }

    /* 30 天热力图 */
    function heatmap(container, data) {
        var max = Math.max.apply(null, data.map(function (d) { return d.minutes; }).concat([1]));
        var html = '<div class="heat-grid">';
        data.forEach(function (d) {
            var lv = 0;
            if (d.minutes > 0) {
                var r = d.minutes / max;
                lv = r > 0.75 ? 4 : r > 0.5 ? 3 : r > 0.25 ? 2 : 1;
            }
            html += '<div class="heat-cell" data-l="' + lv + '" title="' + esc(d.date) + '：' + d.minutes + ' 分钟"></div>';
        });
        html += '</div>';
        container.innerHTML = html;
    }

    function avatarText(name) {
        var s = String(name || '?').trim();
        return s ? s.substring(0, 1).toUpperCase() : '?';
    }

    function todayStr() {
        var d = new Date();
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    }

    function nowLocalInput(plusMinutes) {
        var d = new Date(Date.now() + (plusMinutes || 0) * 60000);
        d.setSeconds(0, 0);
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    return {
        icon: icon,
        esc: esc,
        fmtDateTime: fmtDateTime,
        fmtDate: fmtDate,
        fmtTime: fmtTime,
        fmtMinutes: fmtMinutes,
        hours: hours,
        toast: toast,
        modal: modal,
        confirm: confirm,
        empty: empty,
        loading: loading,
        barChart: barChart,
        heatmap: heatmap,
        badge: badge,
        avatarText: avatarText,
        todayStr: todayStr,
        nowLocalInput: nowLocalInput,
        SEAT_STATUS: SEAT_STATUS,
        BOOKING_STATUS: BOOKING_STATUS,
        CHECKIN_STATUS: CHECKIN_STATUS,
        GOAL_STATUS: GOAL_STATUS
    };
})();
