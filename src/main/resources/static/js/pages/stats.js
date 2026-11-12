/* 数据统计：每日 / 每周时长、热力图、星期分布 */
(function () {
    var P = window.Pages = window.Pages || {};

    P.stats = {
        render: function (view) {
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">数据统计</h2><div class="d">时长统计来自学习计时与预约签到记录</div></div>' +
                '</div>' +
                '<div class="grid c4" id="statKpis"><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div></div>' +
                '<div class="card mt16"><div class="card-head"><h3>最近 30 天每日学习时长</h3><span class="sub">单位：分钟</span></div><div class="chart-wrap" id="dailyChart"></div></div>' +
                '<div class="grid side mt16">' +
                '  <div class="card"><div class="card-head"><h3>近 30 天热力图</h3><span class="sub">颜色越深表示当天学习越久</span></div><div id="heatBox"></div>' +
                '    <div class="chart-legend"><span>少</span><span class="row tight">' +
                '      <span class="heat-cell" data-l="0" style="width:12px;height:12px"></span>' +
                '      <span class="heat-cell" data-l="1" style="width:12px;height:12px"></span>' +
                '      <span class="heat-cell" data-l="2" style="width:12px;height:12px"></span>' +
                '      <span class="heat-cell" data-l="3" style="width:12px;height:12px"></span>' +
                '      <span class="heat-cell" data-l="4" style="width:12px;height:12px"></span>' +
                '    </span><span>多</span></div>' +
                '  </div>' +
                '  <div class="card"><div class="card-head"><h3>星期分布</h3><span class="sub">近一年按星期聚合</span></div><div class="weekday-bars" id="weekdayBox">' + UI.loading() + '</div></div>' +
                '</div>';

            this.load();
        },

        load: async function () {
            var s;
            try {
                s = await API.get('/checkins/statistics');
            } catch (err) {
                UI.toast(err.message || '统计加载失败', 'err');
                return;
            }

            function kpi(label, value, unit, sub, icon, cls) {
                return '<div class="kpi ' + (cls || '') + '"><div class="k-ico">' + UI.icon(icon) + '</div>' +
                    '<div class="k-label">' + label + '</div><div class="k-value">' + value + (unit ? '<small>' + unit + '</small>' : '') + '</div>' +
                    (sub ? '<div class="k-sub">' + sub + '</div>' : '') + '</div>';
            }

            document.getElementById('statKpis').innerHTML =
                kpi('今日学习', UI.hours(s.todayMinutes || 0), '小时', UI.fmtMinutes(s.todayMinutes || 0), 'clock', 'ok') +
                kpi('本周学习', UI.hours(s.weekMinutes || 0), '小时', '日均 ' + UI.hours((s.weekMinutes || 0) / 7) + ' 小时', 'chart', 'info') +
                kpi('本月学习', UI.hours(s.monthMinutes || 0), '小时', '累计 ' + UI.hours(s.totalMinutes || 0) + ' 小时', 'timer', '') +
                kpi('连续打卡', s.currentStreak || 0, '天', '最长 ' + (s.maxStreak || 0) + ' 天 · 共 ' + (s.totalDays || 0) + ' 天', 'fire', 'warn');

            var daily = s.dailyMinutes || [];
            UI.barChart(document.getElementById('dailyChart'), daily.map(function (d) {
                return { label: String(d.date || '').slice(5), value: d.minutes || 0 };
            }), { height: 260 });

            UI.heatmap(document.getElementById('heatBox'), daily.map(function (d) {
                return { date: d.date, minutes: d.minutes || 0 };
            }));

            var week = s.heatmap || [];
            var names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
            var max = Math.max.apply(null, week.concat([1]));
            var box = document.getElementById('weekdayBox');
            box.innerHTML = names.map(function (n, i) {
                var v = week[i] || 0;
                var pct = Math.round(v * 100 / max);
                return '<div class="weekday-row"><span>' + n + '</span>' +
                    '<span class="bar"><span style="width:' + pct + '%"></span></span>' +
                    '<span class="v">' + (v ? UI.fmtMinutes(v) : '0 分钟') + '</span></div>';
            }).join('');
        }
    };
})();
