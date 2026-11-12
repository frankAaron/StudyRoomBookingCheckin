/* 学习计时与打卡记录 */
(function () {
    var P = window.Pages = window.Pages || {};
    var timerId = null;

    var state = { current: 1, size: 8, total: 0, buildings: [], rooms: [], seats: [] };

    function clearTimer() {
        if (timerId) { clearInterval(timerId); timerId = null; }
    }

    function diffText(startTime) {
        var start = new Date(String(startTime).replace(' ', 'T')).getTime();
        var s = Math.max(0, Math.floor((Date.now() - start) / 1000));
        var h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
        var p = function (n) { return n < 10 ? '0' + n : n; };
        return p(h) + ':' + p(m) + ':' + p(sec);
    }

    P.study = {
        render: function (view) {
            clearTimer();
            state.current = 1;
            state.buildings = []; state.rooms = []; state.seats = [];
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">学习计时</h2><div class="d">开始计时后会自动写入打卡记录，作为统计与排行榜依据</div></div>' +
                '</div>' +
                '<div class="grid side">' +
                '  <div id="timerPanel">' + UI.loading() + '</div>' +
                '  <div class="card"><div class="card-head"><h3>打卡记录</h3></div><div id="checkinList">' + UI.loading() + '</div>' +
                '    <div class="row between mt12"><span class="small muted" id="ckInfo"></span>' +
                '    <div class="row tight"><button class="btn ghost sm" id="ckPrev">上一页</button><button class="btn ghost sm" id="ckNext">下一页</button></div></div>' +
                '  </div>' +
                '</div>';

            document.getElementById('ckPrev').addEventListener('click', function () {
                if (state.current > 1) { state.current--; P.study.loadRecords(); }
            });
            document.getElementById('ckNext').addEventListener('click', function () {
                if (state.current * state.size < state.total) { state.current++; P.study.loadRecords(); }
            });
            this.loadActive();
            this.loadRecords();
        },

        loadActive: async function () {
            var panel = document.getElementById('timerPanel');
            var records = [];
            try {
                var page = await API.get('/checkins/mine', { current: 1, size: 20 });
                records = (page && page.records) || [];
            } catch (err) {
                panel.innerHTML = '<div class="card">' + UI.empty('加载失败', err.message) + '</div>';
                return;
            }
            var active = records.filter(function (c) { return c.status === 0; })[0];
            if (active) {
                panel.innerHTML =
                    '<div class="card" style="background:linear-gradient(150deg,#eef2ff,#fff 55%)">' +
                    '  <div class="row"><span class="pulse"></span><b>学习中</b>' +
                    '    <span class="muted small">' + UI.esc(active.buildingName || '') + ' ' + UI.esc(active.roomName || '') + ' · ' + UI.esc(active.seatNo || '') + '</span>' +
                    '  </div>' +
                    '  <div class="timer" id="liveTimer" style="margin:18px 0 6px;text-align:center">00:00:00</div>' +
                    '  <div class="center small muted">开始于 ' + UI.fmtDateTime(active.startTime) + '</div>' +
                    '  <div class="row center mt20" style="justify-content:center">' +
                    '    <button class="btn ok" id="endStudy">结束并保存打卡</button>' +
                    '    <button class="btn ghost" id="toGoals">设个目标</button>' +
                    '  </div>' +
                    '</div>';

                var label = document.getElementById('liveTimer');
                var tick = function () { label.textContent = diffText(active.startTime); };
                tick();
                clearTimer();
                timerId = setInterval(tick, 1000);

                document.getElementById('endStudy').addEventListener('click', async function () {
                    var ok = await UI.confirm('结束本次学习？时长将写入打卡记录。', { okText: '结束' });
                    if (!ok) return;
                    try {
                        var vo = await API.post('/checkins/' + active.id + '/end');
                        clearTimer();
                        UI.toast('本次学习 ' + UI.fmtMinutes(vo.durationMinutes || 0) + '，已记录', 'ok');
                        P.study.loadActive();
                        P.study.loadRecords();
                    } catch (err) { UI.toast(err.message || '结束失败', 'err'); }
                });
                document.getElementById('toGoals').addEventListener('click', function () { App.go('goals'); });
                return;
            }

            panel.innerHTML =
                '<div class="card">' +
                '  <div class="card-head"><h3>开始一次专注</h3><span class="sub">选择座位后开始计时</span></div>' +
                '  <div class="grid c3" style="gap:12px">' +
                '    <div class="field mb0"><label>楼栋</label><select class="select" id="stBuilding"><option value="">加载中...</option></select></div>' +
                '    <div class="field mb0"><label>自习室</label><select class="select" id="stRoom"><option value="">请选择</option></select></div>' +
                '    <div class="field mb0"><label>座位</label><select class="select" id="stSeat"><option value="">请选择</option></select></div>' +
                '  </div>' +
                '  <div class="row mt16"><button class="btn" id="startStudy">' + UI.icon('timer') + ' 开始计时</button>' +
                '    <span class="small muted">已有进行中的打卡时需先结束</span></div>' +
                '  <div class="divider"></div>' +
                '  <div class="small muted">提示：通过「我的预约」签到后会自动开始计时，无需在此重复开始。</div>' +
                '</div>';
            document.getElementById('startStudy').addEventListener('click', P.study.startStudy);
            this.loadBuildings();
        },

        loadBuildings: async function () {
            var sel = document.getElementById('stBuilding');
            if (!sel) return;
            try {
                state.buildings = await API.get('/buildings') || [];
            } catch (e) {
                sel.innerHTML = '<option value="">加载失败</option>';
                return;
            }
            sel.innerHTML = '<option value="">请选择楼栋</option>' + state.buildings.map(function (b) {
                return '<option value="' + b.id + '">' + UI.esc(b.name) + '</option>';
            }).join('');
            sel.addEventListener('change', async function () {
                var opt = document.getElementById('stRoom');
                var seatOpt = document.getElementById('stSeat');
                opt.innerHTML = '<option value="">加载中...</option>';
                seatOpt.innerHTML = '<option value="">请选择</option>';
                if (!sel.value) { opt.innerHTML = '<option value="">请选择</option>'; return; }
                try {
                    state.rooms = await API.get('/rooms', { buildingId: sel.value }) || [];
                } catch (e) { state.rooms = []; }
                opt.innerHTML = '<option value="">请选择自习室</option>' + state.rooms.map(function (r) {
                    return '<option value="' + r.id + '">' + UI.esc(r.name) + '（' + (r.floor || 0) + ' 层）</option>';
                }).join('');
            });
            document.getElementById('stRoom').addEventListener('change', async function () {
                var seatOpt = document.getElementById('stSeat');
                seatOpt.innerHTML = '<option value="">加载中...</option>';
                var roomId = this.value;
                if (!roomId) { seatOpt.innerHTML = '<option value="">请选择</option>'; return; }
                try {
                    state.seats = await API.get('/seats', { roomId: roomId }) || [];
                } catch (e) { state.seats = []; }
                var free = state.seats.filter(function (s) { return s.status === 0; });
                seatOpt.innerHTML = free.length
                    ? '<option value="">请选择座位</option>' + free.map(function (s) {
                        return '<option value="' + s.id + '">' + UI.esc(s.seatNo) + '</option>';
                    }).join('')
                    : '<option value="">暂无空闲座位</option>';
            });
        },

        startStudy: async function () {
            var seatId = document.getElementById('stSeat').value;
            if (!seatId) { UI.toast('请选择座位', 'warn'); return; }
            var btn = document.getElementById('startStudy');
            btn.disabled = true;
            try {
                await API.post('/checkins/start', null, { seatId: seatId });
                UI.toast('已开始计时', 'ok');
                P.study.loadActive();
                P.study.loadRecords();
            } catch (err) {
                UI.toast(err.message || '开始失败', 'err');
                btn.disabled = false;
            }
        },

        loadRecords: async function () {
            var box = document.getElementById('checkinList');
            box.innerHTML = UI.loading();
            var page;
            try {
                page = await API.get('/checkins/mine', { current: state.current, size: state.size });
            } catch (err) {
                box.innerHTML = UI.empty('打卡记录加载失败', err.message);
                return;
            }
            state.total = page.total || 0;
            var records = page.records || [];
            document.getElementById('ckInfo').textContent = '共 ' + state.total + ' 条';
            if (!records.length) {
                box.innerHTML = UI.empty('还没有打卡记录', '开始一次学习计时吧');
                return;
            }
            box.innerHTML = records.map(function (c) {
                return '<div class="row between" style="padding:10px 0;border-bottom:1px solid #f1f4f9">' +
                    '<div><div style="font-weight:600">' + UI.fmtDate(c.startTime) + ' ' + UI.fmtTime(c.startTime) +
                    (c.endTime ? ' - ' + UI.fmtTime(c.endTime) : '') + '</div>' +
                    '<div class="small muted mt8">' + UI.esc(c.buildingName || '') + ' ' + UI.esc(c.roomName || '') +
                    (c.seatNo ? ' · ' + UI.esc(c.seatNo) : '') + '</div></div>' +
                    '<div class="right">' + UI.badge(UI.CHECKIN_STATUS, c.status) +
                    '<div class="small" style="font-weight:600;margin-top:4px">' + (c.durationMinutes != null ? UI.fmtMinutes(c.durationMinutes) : '进行中') + '</div></div>' +
                    '</div>';
            }).join('');
        }
    };
})();
