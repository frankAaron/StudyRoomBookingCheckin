/* 学习目标与连续打卡成就 */
(function () {
    var P = window.Pages = window.Pages || {};

    var ACHIEVEMENTS = [
        { key: 'first', name: '初次打卡', desc: '完成第一次学习打卡', test: function (s) { return (s.totalDays || 0) >= 1; }, icon: 'clock' },
        { key: 'streak3', name: '三日坚持', desc: '连续打卡 3 天', test: function (s) { return (s.maxStreak || 0) >= 3; }, icon: 'fire' },
        { key: 'streak7', name: '七日不辍', desc: '连续打卡 7 天', test: function (s) { return (s.maxStreak || 0) >= 7; }, icon: 'fire' },
        { key: 'streak30', name: '月度坚持', desc: '连续打卡 30 天', test: function (s) { return (s.maxStreak || 0) >= 30; }, icon: 'trophy' },
        { key: 'h10', name: '十小时', desc: '累计学习 10 小时', test: function (s) { return (s.totalMinutes || 0) >= 600; }, icon: 'chart' },
        { key: 'h50', name: '五十小时', desc: '累计学习 50 小时', test: function (s) { return (s.totalMinutes || 0) >= 3000; }, icon: 'chart' },
        { key: 'h100', name: '百小时', desc: '累计学习 100 小时', test: function (s) { return (s.totalMinutes || 0) >= 6000; }, icon: 'trophy' },
        { key: 'goal', name: '目标达成者', desc: '完成一个学习目标', needGoals: true, icon: 'target' }
    ];

    P.goals = {
        render: function (view) {
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">学习目标</h2><div class="d">设定阶段目标，用连续打卡解锁成就</div></div>' +
                '  <div class="actions"><button class="btn" id="newGoal">新建目标</button></div>' +
                '</div>' +
                '<div class="grid c4" id="goalKpis"><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div><div class="card" style="height:106px"></div></div>' +
                '<div class="card mt16"><div class="card-head"><h3>我的目标</h3><span class="sub">进度按目标周期内的打卡时长计算</span></div><div id="goalList">' + UI.loading() + '</div></div>' +
                '<div class="card mt16"><div class="card-head"><h3>打卡成就</h3><span class="sub">连续打卡与累计时长自动解锁</span></div><div class="grid c4" id="achBox">' + UI.loading() + '</div></div>';

            document.getElementById('newGoal').addEventListener('click', function () { P.goals.openForm(null); });
            this.load();
        },

        load: async function () {
            var goals, stats;
            try {
                var res = await Promise.all([API.get('/goals/mine'), API.get('/checkins/statistics')]);
                goals = res[0] || []; stats = res[1] || {};
            } catch (err) {
                UI.toast(err.message || '加载失败', 'err');
                return;
            }

            var done = goals.filter(function (g) { return g.status === 1; }).length;
            var running = goals.filter(function (g) { return g.status === 0; }).length;
            function kpi(label, value, unit, sub, icon, cls) {
                return '<div class="kpi ' + (cls || '') + '"><div class="k-ico">' + UI.icon(icon) + '</div>' +
                    '<div class="k-label">' + label + '</div><div class="k-value">' + value + (unit ? '<small>' + unit + '</small>' : '') + '</div>' +
                    (sub ? '<div class="k-sub">' + sub + '</div>' : '') + '</div>';
            }
            document.getElementById('goalKpis').innerHTML =
                kpi('进行中目标', running, '个', '共 ' + goals.length + ' 个目标', 'target', '') +
                kpi('已达成', done, '个', '达成率 ' + (goals.length ? Math.round(done * 100 / goals.length) : 0) + '%', 'trophy', 'ok') +
                kpi('连续打卡', stats.currentStreak || 0, '天', '最长 ' + (stats.maxStreak || 0) + ' 天', 'fire', 'warn') +
                kpi('累计学习', UI.hours(stats.totalMinutes || 0), '小时', '共 ' + (stats.totalDays || 0) + ' 天', 'clock', 'info');

            var list = document.getElementById('goalList');
            if (!goals.length) {
                list.innerHTML = UI.empty('还没有学习目标', '点击右上角新建一个吧');
            } else {
                list.innerHTML = goals.map(function (g) {
                    var pct = Math.max(0, Math.min(100, g.progressPercent || 0));
                    var barCls = g.status === 1 ? 'ok' : '';
                    return '<div style="padding:14px 0;border-bottom:1px solid #f1f4f9">' +
                        '<div class="row between">' +
                        '  <div><span style="font-weight:700">' + UI.esc(g.title) + '</span> ' + UI.badge(UI.GOAL_STATUS, g.status) + '</div>' +
                        '  <div class="row tight">' +
                        '    <button class="btn ghost sm" data-act="edit" data-id="' + g.id + '">编辑</button>' +
                        '    <button class="btn ghost sm" data-act="del" data-id="' + g.id + '">删除</button>' +
                        '  </div>' +
                        '</div>' +
                        '<div class="row between mt8 small muted">' +
                        '  <span>' + (g.startDate || '不限') + ' ~ ' + (g.endDate || '不限') + '</span>' +
                        '  <span>' + (g.completedMinutes || 0) + ' / ' + g.targetMinutes + ' 分钟 · ' + pct + '%</span>' +
                        '</div>' +
                        '<div class="progress ' + barCls + ' mt8"><span style="width:' + pct + '%"></span></div>' +
                        '</div>';
                }).join('');

                list.querySelectorAll('[data-act="edit"]').forEach(function (btn) {
                    btn.addEventListener('click', function () {
                        var g = goals.filter(function (x) { return x.id === Number(btn.dataset.id); })[0];
                        P.goals.openForm(g);
                    });
                });
                list.querySelectorAll('[data-act="del"]').forEach(function (btn) {
                    btn.addEventListener('click', async function () {
                        var ok = await UI.confirm('删除后不可恢复，确定删除该目标吗？', { okText: '删除', danger: true });
                        if (!ok) return;
                        try {
                            await API.del('/goals/' + btn.dataset.id);
                            UI.toast('已删除', 'ok');
                            P.goals.load();
                        } catch (err) { UI.toast(err.message, 'err'); }
                    });
                });
            }

            document.getElementById('achBox').innerHTML = ACHIEVEMENTS.map(function (a) {
                var unlocked = a.needGoals ? done > 0 : a.test(stats);
                return '<div class="card" style="box-shadow:none;padding:16px;' +
                    (unlocked ? 'border-color:#c7f0dd;background:linear-gradient(140deg,#f3fdf8,#fff)' : 'opacity:.62') + '">' +
                    '<div class="row between"><div class="k-ico" style="position:static;width:34px;height:34px;border-radius:10px;display:grid;place-items:center;background:' +
                    (unlocked ? '#e7f8f0;color:#12b76a' : '#f2f4f8;color:#98a2b3') + '">' + UI.icon(a.icon) + '</div>' +
                    '<span class="badge ' + (unlocked ? 'ok' : '') + '">' + (unlocked ? '已解锁' : '未解锁') + '</span></div>' +
                    '<div style="font-weight:700;margin-top:10px">' + a.name + '</div>' +
                    '<div class="small muted mt8">' + a.desc + '</div></div>';
            }).join('');
        },

        openForm: function (goal) {
            var isEdit = !!goal;
            var form = document.createElement('div');
            form.innerHTML =
                '<div class="field"><label>目标名称</label><input class="input" id="gTitle" placeholder="例如：本月冲刺 60 小时" value="' + UI.esc(goal ? goal.title : '') + '"></div>' +
                '<div class="field"><label>目标时长（小时）</label><input class="input" id="gHours" type="number" min="0.5" step="0.5" value="' + (goal ? (goal.targetMinutes / 60) : 20) + '"></div>' +
                '<div class="grid c2" style="gap:12px">' +
                '  <div class="field"><label>开始日期</label><input class="input" id="gStart" type="date" value="' + (goal && goal.startDate ? goal.startDate : UI.todayStr()) + '"></div>' +
                '  <div class="field"><label>结束日期</label><input class="input" id="gEnd" type="date" value="' + (goal && goal.endDate ? goal.endDate : '') + '"></div>' +
                '</div>' +
                '<div class="small muted">目标周期内累计的打卡时长会计入进度，达成后自动标记为已达成。</div>';

            UI.modal({
                title: isEdit ? '编辑学习目标' : '新建学习目标',
                body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: isEdit ? '保存' : '创建', cls: '', onClick: async function (x, btn) {
                            var payload = {
                                title: document.getElementById('gTitle').value.trim(),
                                targetMinutes: Math.round(Number(document.getElementById('gHours').value) * 60),
                                startDate: document.getElementById('gStart').value || null,
                                endDate: document.getElementById('gEnd').value || null
                            };
                            if (!payload.title) { UI.toast('请填写目标名称', 'warn'); return; }
                            if (!payload.targetMinutes || payload.targetMinutes < 30) { UI.toast('目标时长至少 0.5 小时', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                if (isEdit) {
                                    await API.put('/goals/' + goal.id, payload);
                                    UI.toast('目标已更新', 'ok');
                                } else {
                                    await API.post('/goals', payload);
                                    UI.toast('目标已创建', 'ok');
                                }
                                x.close();
                                P.goals.load();
                            } catch (err) {
                                UI.toast(err.message || '保存失败', 'err');
                                btn.disabled = false;
                            }
                        }
                    }
                ]
            });
        }
    };
})();
