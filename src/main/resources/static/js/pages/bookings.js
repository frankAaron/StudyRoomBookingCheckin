/* 我的预约：签到 / 扫码签到 / 离座释放 / 取消 */
(function () {
    var P = window.Pages = window.Pages || {};

    var state = { status: null, current: 1, size: 8, total: 0 };

    P.bookings = {
        render: function (view) {
            state.status = null; state.current = 1;
            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">我的预约</h2><div class="d">签到后座位进入使用中状态，离座请及时释放</div></div>' +
                '  <div class="actions"><button class="btn" id="toRooms">新预约</button></div>' +
                '</div>' +
                '<div class="grid side">' +
                '  <div class="card">' +
                '    <div class="card-head"><h3>扫码签到</h3><span class="sub">扫描座位二维码，或手动粘贴二维码内容 / 签到码</span></div>' +
                '    <div class="row">' +
                '      <input class="input" id="qrInput" style="flex:1" placeholder="粘贴座位二维码内容，或 8 位签到码">' +
                '      <button class="btn" id="qrBtn">' + UI.icon('scan') + ' 签到</button>' +
                '    </div>' +
                '    <div class="small muted mt8">规则：需在预约开始时间前后 30 分钟内签到；签到后自动开始学习计时。</div>' +
                '    <div class="divider"></div>' +
                '    <div class="small muted">找不到二维码？可在下方「待签到」预约中直接点击签到，签到码位于预约卡片上。</div>' +
                '  </div>' +
                '  <div class="card"><div class="card-head"><h3>预约状态说明</h3></div>' +
                '    <div class="row" style="gap:8px;flex-wrap:wrap">' +
                Object.keys(UI.BOOKING_STATUS).map(function (k) {
                    return UI.badge(UI.BOOKING_STATUS, Number(k));
                }).join('') +
                '    </div>' +
                '    <div class="small muted mt12">待签到超时的预约会被系统自动标记为已过期，并释放座位。</div>' +
                '  </div>' +
                '</div>' +
                '<div class="card mt16">' +
                '  <div class="card-head"><h3>预约记录</h3><div class="actions"><div class="pill-tabs" id="statusTabs"></div></div></div>' +
                '  <div id="bookingList">' + UI.loading() + '</div>' +
                '  <div class="row between mt12"><span class="small muted" id="pageInfo"></span>' +
                '  <div class="row tight"><button class="btn ghost sm" id="prevPage">上一页</button><button class="btn ghost sm" id="nextPage">下一页</button></div></div>' +
                '</div>';

            document.getElementById('toRooms').addEventListener('click', function () { App.go('rooms'); });
            document.getElementById('qrBtn').addEventListener('click', this.scanCheckin);
            document.getElementById('qrInput').addEventListener('keydown', function (e) {
                if (e.key === 'Enter') P.bookings.scanCheckin();
            });
            document.getElementById('prevPage').addEventListener('click', function () {
                if (state.current > 1) { state.current--; P.bookings.load(); }
            });
            document.getElementById('nextPage').addEventListener('click', function () {
                if (state.current * state.size < state.total) { state.current++; P.bookings.load(); }
            });

            var tabs = document.getElementById('statusTabs');
            var items = [{ v: null, t: '全部' }].concat(Object.keys(UI.BOOKING_STATUS).map(function (k) {
                return { v: Number(k), t: UI.BOOKING_STATUS[k].label };
            }));
            tabs.innerHTML = items.map(function (it) {
                return '<button class="pill' + (it.v === null ? ' active' : '') + '" data-v="' + (it.v === null ? '' : it.v) + '">' + it.t + '</button>';
            }).join('');
            tabs.querySelectorAll('.pill').forEach(function (b) {
                b.addEventListener('click', function () {
                    tabs.querySelectorAll('.pill').forEach(function (x) { x.classList.remove('active'); });
                    b.classList.add('active');
                    state.status = b.dataset.v === '' ? null : Number(b.dataset.v);
                    state.current = 1;
                    P.bookings.load();
                });
            });

            this.load();
        },

        load: async function () {
            var box = document.getElementById('bookingList');
            box.innerHTML = UI.loading();
            var page;
            try {
                page = await API.get('/bookings/mine', { current: state.current, size: state.size, status: state.status });
            } catch (err) {
                box.innerHTML = UI.empty('预约加载失败', err.message);
                return;
            }
            state.total = page.total || 0;
            var records = page.records || [];
            document.getElementById('pageInfo').textContent =
                '共 ' + state.total + ' 条 · 第 ' + state.current + ' / ' + Math.max(1, Math.ceil(state.total / state.size)) + ' 页';
            if (!records.length) {
                box.innerHTML = UI.empty('暂无预约记录', '去自习室选一个座位吧');
                return;
            }
            box.innerHTML = records.map(function (b) {
                return '<div class="card" style="box-shadow:none;margin-bottom:10px;padding:14px 16px">' +
                    '<div class="row between">' +
                    '  <div class="row" style="gap:14px">' +
                    '    <div class="avatar" style="background:linear-gradient(140deg,#6366f1,#8b5cf6)">' + UI.esc((b.seatNo || '').split('-').pop() || '-') + '</div>' +
                    '    <div><div style="font-weight:700">' + UI.esc(b.buildingName || '') + ' · ' + UI.esc(b.roomName || '') + ' · ' + UI.esc(b.seatNo || '') + '</div>' +
                    '    <div class="small muted mt8">' + UI.fmtDateTime(b.startTime) + ' ~ ' + UI.fmtTime(b.endTime) +
                    (b.actualMinutes ? ' · 实际学习 ' + UI.fmtMinutes(b.actualMinutes) : '') + '</div></div>' +
                    '  </div>' +
                    '  <div class="row">' + UI.badge(UI.BOOKING_STATUS, b.status) + '</div>' +
                    '</div>' +
                    '<div class="row between mt12">' +
                    '  <div class="small muted">' +
                    (b.checkinCode ? '签到码：<span class="mono" style="color:var(--brand);font-weight:700">' + UI.esc(b.checkinCode) + '</span>' : '') +
                    (b.checkinTime ? ' · 签到于 ' + UI.fmtTime(b.checkinTime) : '') +
                    (b.checkoutTime ? ' · 离座于 ' + UI.fmtTime(b.checkoutTime) : '') +
                    (b.remark ? ' · 备注：' + UI.esc(b.remark) : '') +
                    '  </div>' +
                    '  <div class="row tight">' + actions(b) + '</div>' +
                    '</div>' +
                    '</div>';
            }).join('');

            box.querySelectorAll('[data-act]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var id = Number(btn.dataset.id);
                    var act = btn.dataset.act;
                    if (act === 'checkin') P.bookings.checkin(id);
                    if (act === 'checkout') P.bookings.checkout(id, btn.dataset.seat);
                    if (act === 'cancel') P.bookings.cancel(id);
                });
            });
        },

        checkin: async function (id) {
            var ok = await UI.confirm('确认已到达座位并签到吗？签到后将自动开始学习计时。', { okText: '确认签到' });
            if (!ok) return;
            try {
                await API.post('/bookings/' + id + '/checkin');
                UI.toast('签到成功，已开始学习计时', 'ok');
                this.load();
            } catch (err) { UI.toast(err.message || '签到失败', 'err'); }
        },

        checkout: async function (id, seatNo) {
            var ok = await UI.confirm('确认离座？座位 ' + (seatNo || '') + ' 将被释放并结束本次学习。', { okText: '离座释放' });
            if (!ok) return;
            try {
                var vo = await API.post('/bookings/' + id + '/checkout');
                UI.toast('已离座，本次学习 ' + UI.fmtMinutes(vo.actualMinutes || 0), 'ok');
                this.load();
            } catch (err) { UI.toast(err.message || '离座失败', 'err'); }
        },

        cancel: async function (id) {
            var ok = await UI.confirm('取消后座位将立即释放，确定取消这条预约吗？', { okText: '取消预约', danger: true });
            if (!ok) return;
            try {
                await API.post('/bookings/' + id + '/cancel');
                UI.toast('预约已取消', 'ok');
                this.load();
            } catch (err) { UI.toast(err.message || '取消失败', 'err'); }
        },

        scanCheckin: async function () {
            var input = document.getElementById('qrInput');
            var code = (input.value || '').trim();
            if (!code) { UI.toast('请先粘贴二维码内容或签到码', 'warn'); return; }
            var btn = document.getElementById('qrBtn');
            btn.disabled = true;

            var target = null;
            try {
                var page = await API.get('/bookings/mine', { current: 1, size: 50, status: 0 });
                var pending = (page && page.records) || [];
                target = pending.filter(function (b) { return b.checkinCode === code; })[0];
                if (!target) {
                    try {
                        var seat = await API.get('/seats/qrcode/' + encodeURIComponent(code));
                        target = pending.filter(function (b) { return b.seatId === seat.id; })[0];
                        if (!target) UI.toast('该座位没有待签到的预约', 'warn');
                    } catch (e) {
                        UI.toast('未找到匹配的预约，请检查二维码或签到码', 'err');
                    }
                }
                if (target) {
                    await API.post('/bookings/' + target.id + '/checkin');
                    UI.toast('扫码签到成功，已开始学习计时', 'ok');
                    input.value = '';
                    this.load();
                }
            } catch (err) {
                UI.toast(err.message || '签到失败', 'err');
            } finally {
                btn.disabled = false;
            }
        }
    };

    function actions(b) {
        var html = '';
        if (b.status === 0) {
            html += '<button class="btn sm" data-act="checkin" data-id="' + b.id + '">签到</button>';
            html += '<button class="btn ghost sm" data-act="cancel" data-id="' + b.id + '">取消</button>';
        } else if (b.status === 1) {
            html += '<button class="btn ok sm" data-act="checkout" data-id="' + b.id + '" data-seat="' + UI.esc(b.seatNo || '') + '">离座释放</button>';
        }
        return html || '<span class="small muted">无操作</span>';
    }
})();
