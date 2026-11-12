/* 排行榜与好友监督 */
(function () {
    var P = window.Pages = window.Pages || {};
    var state = { period: 'daily' };
    var PERIODS = [
        { key: 'daily', label: '今日' },
        { key: 'weekly', label: '本周' },
        { key: 'monthly', label: '本月' },
        { key: 'all', label: '累计' }
    ];

    P.ranking = {
        render: function (view) {
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">排行榜</h2><div class="d">按时长排名，和好友互相监督</div></div>' +
                '  <div class="actions"><div class="pill-tabs" id="periodTabs">' +
                PERIODS.map(function (p) {
                    return '<button class="pill' + (p.key === state.period ? ' active' : '') + '" data-k="' + p.key + '">' + p.label + '</button>';
                }).join('') + '</div></div>' +
                '</div>' +
                '<div class="grid side">' +
                '  <div>' +
                '    <div class="card" id="myRankCard">' + UI.loading() + '</div>' +
                '    <div class="card mt16"><div class="card-head"><h3>榜单 TOP 50</h3><span class="sub">同等时长按昵称排序</span></div><div id="rankList">' + UI.loading() + '</div></div>' +
                '  </div>' +
                '  <div>' +
                '    <div class="card"><div class="card-head"><h3>好友监督</h3><button class="btn ghost sm" id="addFriend">添加好友</button></div>' +
                '      <div id="friendList">' + UI.loading() + '</div></div>' +
                '    <div class="card mt16"><div class="card-head"><h3>好友申请</h3><span class="sub">待处理</span></div><div id="requestList">' + UI.loading() + '</div></div>' +
                '  </div>' +
                '</div>';

            document.getElementById('periodTabs').querySelectorAll('.pill').forEach(function (b) {
                b.addEventListener('click', function () {
                    state.period = b.dataset.k;
                    document.getElementById('periodTabs').querySelectorAll('.pill').forEach(function (x) { x.classList.remove('active'); });
                    b.classList.add('active');
                    P.ranking.loadRank();
                });
            });
            document.getElementById('addFriend').addEventListener('click', function () { P.ranking.openAddFriend(); });

            this.loadRank();
            this.loadFriends();
            this.loadRequests();
        },

        loadRank: async function () {
            var mine = document.getElementById('myRankCard');
            var listBox = document.getElementById('rankList');
            listBox.innerHTML = UI.loading();
            var tasks = [API.get('/ranking/' + state.period, { topN: 50 })];
            if (state.period !== 'all') tasks.push(API.get('/ranking/my/' + state.period));
            var res;
            try {
                res = await Promise.all(tasks);
            } catch (err) {
                listBox.innerHTML = UI.empty('榜单加载失败', err.message);
                mine.innerHTML = UI.empty('暂无我的排名', err.message);
                return;
            }
            var list = res[0] || [];
            var me = (API.user() || {});
            var my = state.period === 'all'
                ? list.filter(function (r) { return r.userId === me.userId; })[0]
                : res[1];

            var label = (PERIODS.filter(function (p) { return p.key === state.period; })[0] || {}).label || '';
            mine.innerHTML = '<div class="row" style="gap:18px;align-items:flex-end">' +
                '<div><div class="muted small">' + label + '排名</div>' +
                '<div style="font-size:40px;font-weight:700;line-height:1.05">#' + (my && my.rank ? my.rank : '-') + '</div></div>' +
                '<div style="margin-left:auto;text-align:right"><div class="muted small">' + label + '时长</div>' +
                '<div style="font-size:20px;font-weight:700">' + UI.fmtMinutes(my ? my.totalMinutes : 0) + '</div></div></div>' +
                '<div class="divider"></div>' +
                '<div class="small muted">' + UI.esc(me.nickname || me.username || '') + ' · 继续加油，进榜靠的是每一次专注</div>';

            if (!list.length) {
                listBox.innerHTML = UI.empty('该周期还没有打卡数据', '开始一次学习就能上榜');
                return;
            }
            listBox.innerHTML = '<div class="rank-list">' + list.map(function (r, i) {
                var rank = r.rank || i + 1;
                var cls = rank === 1 ? 'top1' : rank === 2 ? 'top2' : rank === 3 ? 'top3' : '';
                return '<div class="rank-item' + (r.userId === me.userId ? ' me' : '') + '">' +
                    '<div class="no ' + cls + '">' + rank + '</div>' +
                    '<div class="avatar">' + UI.avatarText(r.nickname) + '</div>' +
                    '<div class="who"><div class="n">' + UI.esc(r.nickname || '匿名用户') + (r.userId === me.userId ? '（我）' : '') + '</div>' +
                    '<div class="s">' + UI.fmtMinutes(r.totalMinutes) + '</div></div>' +
                    '<div class="v">' + UI.hours(r.totalMinutes) + '<span class="small muted"> 小时</span></div>' +
                    '</div>';
            }).join('') + '</div>';
        },

        loadFriends: async function () {
            var box = document.getElementById('friendList');
            var list;
            try {
                var me = API.user() || {};
                list = (await API.get('/friends') || []).filter(function (f) {
                    return f.userId !== me.userId;
                });
            } catch (err) {
                box.innerHTML = UI.empty('好友加载失败', err.message);
                return;
            }
            if (!list.length) {
                box.innerHTML = UI.empty('还没有好友', '添加好友后可以互相监督');
                return;
            }
            box.innerHTML = '<div class="rank-list">' + list.map(function (f) {
                return '<div class="rank-item">' +
                    '<div class="avatar">' + UI.avatarText(f.nickname) + '</div>' +
                    '<div class="who"><div class="n">' + UI.esc(f.nickname || '好友') + '</div>' +
                    '<div class="s">今日 ' + UI.fmtMinutes(f.totalMinutes || 0) + '</div></div>' +
                    '<button class="btn ghost sm" data-uid="' + f.userId + '">解除</button>' +
                    '</div>';
            }).join('') + '</div>';

            box.querySelectorAll('[data-uid]').forEach(function (btn) {
                btn.addEventListener('click', async function () {
                    var ok = await UI.confirm('确定解除该好友关系吗？', { okText: '解除', danger: true });
                    if (!ok) return;
                    try {
                        await API.del('/friends/' + btn.dataset.uid);
                        UI.toast('已解除好友关系', 'ok');
                        P.ranking.loadFriends();
                    } catch (err) { UI.toast(err.message, 'err'); }
                });
            });
        },

        loadRequests: async function () {
            var box = document.getElementById('requestList');
            var page;
            try {
                page = await API.get('/friends/requests', { current: 1, size: 20 });
            } catch (err) {
                box.innerHTML = UI.empty('申请加载失败', err.message);
                return;
            }
            var records = (page && page.records) || [];
            if (!records.length) {
                box.innerHTML = '<div class="small muted">暂无待处理的好友申请</div>';
                return;
            }
            box.innerHTML = '<div class="rank-list">' + records.map(function (r) {
                return '<div class="rank-item">' +
                    '<div class="avatar">' + UI.avatarText(r.nickname) + '</div>' +
                    '<div class="who"><div class="n">' + UI.esc(r.nickname || '用户 ' + r.userId) + '</div>' +
                    '<div class="s">请求添加你为好友</div></div>' +
                    '<div class="row tight">' +
                    '  <button class="btn sm" data-act="accept" data-uid="' + r.userId + '">接受</button>' +
                    '  <button class="btn ghost sm" data-act="reject" data-uid="' + r.userId + '">拒绝</button>' +
                    '</div></div>';
            }).join('') + '</div>';

            box.querySelectorAll('[data-act]').forEach(function (btn) {
                btn.addEventListener('click', async function () {
                    var act = btn.dataset.act;
                    try {
                        await API.post('/friends/' + act + '/' + btn.dataset.uid);
                        UI.toast(act === 'accept' ? '已添加为好友' : '已拒绝申请', 'ok');
                        P.ranking.loadRequests();
                        P.ranking.loadFriends();
                    } catch (err) { UI.toast(err.message, 'err'); }
                });
            });
        },

        openAddFriend: function () {
            var form = document.createElement('div');
            form.innerHTML = '<div class="field"><label>好友用户 ID</label>' +
                '<input class="input" id="friendId" type="number" min="1" placeholder="输入对方的用户 ID">' +
                '<div class="hint">用户 ID 可在排行榜中查看，或让对方告诉你；发送申请后需对方接受。</div></div>';
            UI.modal({
                title: '添加好友',
                body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: '发送申请', cls: '', onClick: async function (x, btn) {
                            var id = Number(document.getElementById('friendId').value);
                            if (!id) { UI.toast('请输入用户 ID', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                await API.post('/friends/request/' + id);
                                UI.toast('好友申请已发送', 'ok');
                                x.close();
                            } catch (err) {
                                UI.toast(err.message || '发送失败', 'err');
                                btn.disabled = false;
                            }
                        }
                    }
                ]
            });
        }
    };
})();
