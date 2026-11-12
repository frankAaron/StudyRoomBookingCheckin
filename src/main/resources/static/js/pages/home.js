/* 学习概览 */
(function () {
    var P = window.Pages = window.Pages || {};

    function kpi(label, value, unit, sub, icon, cls) {
        return '<div class="kpi ' + (cls || '') + '">' +
            '<div class="k-icon k-ico">' + UI.icon(icon) + '</div>' +
            '<div class="k-label">' + label + '</div>' +
            '<div class="k-value">' + value + (unit ? '<small>' + unit + '</small>' : '') + '</div>' +
            (sub ? '<div class="k-sub">' + sub + '</div>' : '') +
            '</div>';
    }

    P.home = {
        render: function (view) {
            var user = API.user() || {};
            var hour = new Date().getHours();
            var greet = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好';
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">' + greet + '，' + UI.esc(user.nickname || user.username || '同学') + '</h2>' +
                '  <div class="d">' + UI.todayStr() + ' · 保持专注，记录每一次进步</div></div>' +
                '  <div class="actions">' +
                '    <button class="btn ghost" id="goRoom">去预约座位</button>' +
                '    <button class="btn" id="goStudy">' + UI.icon('timer') + ' 开始学习</button>' +
                '  </div>' +
                '</div>' +
                '<div id="activeBanner"></div>' +
                '<div class="grid c4" id="kpis">' + [1, 2, 3, 4].map(function () { return '<div class="card" style="height:106px"></div>'; }).join('') + '</div>' +
                '<div class="grid side mt16">' +
                '  <div class="card"><div class="card-head"><h3>最近 30 天学习时长</h3><span class="sub">单位：分钟</span></div><div id="chartHome" class="chart-wrap"></div></div>' +
                '  <div class="card"><div class="card-head"><h3>近期预约</h3><a class="small" href="#/bookings">全部</a></div><div id="homeBookings">' + UI.loading() + '</div></div>' +
                '</div>' +
                '<div class="grid side mt16">' +
                '  <div class="card"><div class="card-head"><h3>今日学习榜</h3><a class="small" href="#/ranking">完整榜单</a></div><div id="homeRank">' + UI.loading() + '</div></div>' +
                '  <div class="card"><div class="card-head"><h3>我的今日排名</h3></div><div id="myRank">' + UI.loading() + '</div></div>' +
                '</div>';

            document.getElementById('goRoom').addEventListener('click', function () { App.go('rooms'); });
            document.getElementById('goStudy').addEventListener('click', function () { App.go('study'); });
            this.load(view);
        },

        load: async function () {
            var stats, bookings, active, rank, top;
            try {
                var res = await Promise.all([
                    API.get('/checkins/statistics'),
                    API.get('/bookings/mine', { current: 1, size: 4 }),
                    API.get('/checkins/mine', { current: 1, size: 20 }),
                    API.get('/ranking/my/daily'),
                    API.get('/ranking/daily', { topN: 5 })
                ]);
                stats = res[0]; bookings = res[1]; active = res[2]; rank = res[3]; top = res[4];
            } catch (err) {
                UI.toast(err.message || '数据加载失败', 'err');
                return;
            }

            var kpis = document.getElementById('kpis');
            if (kpis) {
                kpis.innerHTML =
                    kpi('今日学习', UI.hours(stats.todayMinutes || 0), '小时', '共打卡 ' + (stats.totalDays || 0) + ' 天', 'clock', 'ok') +
                    kpi('本周学习', UI.hours(stats.weekMinutes || 0), '小时', '本月 ' + UI.hours(stats.monthMinutes || 0) + ' 小时', 'chart', 'info') +
                    kpi('连续打卡', stats.currentStreak || 0, '天', '坚持就是最长的复利', 'fire', 'warn') +
                    kpi('累计学习', UI.hours(stats.totalMinutes || 0), '小时', '专注总时长', 'trophy', '');
            }

            var chart = document.getElementById('chartHome');
            if (chart && stats.dailyMinutes) {
                UI.barChart(chart, stats.dailyMinutes.map(function (d) {
                    return { label: String(d.date || '').slice(5), value: d.minutes || 0 };
                }));
            }

            var activeOne = (active && active.records || []).filter(function (c) { return c.status === 0; })[0];
            var banner = document.getElementById('activeBanner');
            if (banner && activeOne) {
                banner.innerHTML = '<div class="card mt16" style="border-color:#c7f0dd;background:linear-gradient(100deg,#f3fdf8,#fff)">' +
                    '<div class="row"><span class="pulse"></span><b>正在学习</b>' +
                    '<span class="muted small">' + UI.esc(activeOne.buildingName || '') + ' ' + UI.esc(activeOne.roomName || '') + ' · ' + UI.esc(activeOne.seatNo || '') + '</span>' +
                    '<span class="spacer" style="flex:1"></span>' +
                    '<button class="btn sm" id="endNow">结束本次学习</button></div>' +
                    '<div class="small muted mt8">开始于 ' + UI.fmtDateTime(activeOne.startTime) + '</div></div>';
                document.getElementById('endNow').addEventListener('click', async function () {
                    var ok = await UI.confirm('结束后本次时长将计入打卡记录，确定吗？', { okText: '结束学习' });
                    if (!ok) return;
                    try {
                        await API.post('/checkins/' + activeOne.id + '/end');
                        UI.toast('已结束本次学习', 'ok');
                        App.reload();
                    } catch (e) { UI.toast(e.message, 'err'); }
                });
            }

            var list = document.getElementById('homeBookings');
            if (list) {
                var records = (bookings && bookings.records) || [];
                if (!records.length) {
                    list.innerHTML = UI.empty('还没有预约', '先去选一个座位吧');
                } else {
                    list.innerHTML = '<div class="rank-list">' + records.map(function (b) {
                        return '<div class="rank-item">' +
                            '<div class="no">' + UI.esc((b.seatNo || '').split('-').pop() || '-') + '</div>' +
                            '<div class="who"><div class="n">' + UI.esc(b.buildingName || '') + ' ' + UI.esc(b.roomName || '') + '</div>' +
                            '<div class="s">' + UI.fmtDate(b.startTime) + ' ' + UI.fmtTime(b.startTime) + '-' + UI.fmtTime(b.endTime) + '</div></div>' +
                            '<div class="v">' + UI.badge(UI.BOOKING_STATUS, b.status) + '</div></div>';
                    }).join('') + '</div>';
                }
            }

            var rankBox = document.getElementById('myRank');
            if (rankBox) {
                rankBox.innerHTML = '<div class="row" style="gap:18px;align-items:flex-end">' +
                    '<div><div class="muted small">今日排名</div><div style="font-size:38px;font-weight:700;line-height:1.1">#' + (rank.rank || '-') + '</div></div>' +
                    '<div style="margin-left:auto;text-align:right"><div class="muted small">今日时长</div>' +
                    '<div style="font-size:20px;font-weight:700">' + UI.fmtMinutes(rank.totalMinutes || 0) + '</div></div></div>' +
                    '<div class="divider"></div>' +
                    '<div class="small muted">连续打卡 ' + (stats.currentStreak || 0) + ' 天 · 累计打卡 ' + (stats.totalDays || 0) + ' 天</div>';
            }

            var topBox = document.getElementById('homeRank');
            if (topBox) {
                var arr = top || [];
                if (!arr.length) {
                    topBox.innerHTML = UI.empty('今天还没有人打卡', '你可以成为第一个');
                } else {
                    topBox.innerHTML = '<div class="rank-list">' + arr.map(function (r, i) {
                        var cls = i === 0 ? 'top1' : i === 1 ? 'top2' : i === 2 ? 'top3' : '';
                        return '<div class="rank-item' + (r.userId === (API.user() || {}).userId ? ' me' : '') + '">' +
                            '<div class="no ' + cls + '">' + (r.rank || i + 1) + '</div>' +
                            '<div class="avatar">' + UI.avatarText(r.nickname) + '</div>' +
                            '<div class="who"><div class="n">' + UI.esc(r.nickname || '匿名用户') + '</div></div>' +
                            '<div class="v">' + UI.fmtMinutes(r.totalMinutes) + '</div></div>';
                    }).join('') + '</div>';
                }
            }
        }
    };
})();
