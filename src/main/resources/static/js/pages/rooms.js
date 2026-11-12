/* 自习室：楼栋 → 楼层 → 房间 → 座位图 → 预约 */
(function () {
    var P = window.Pages = window.Pages || {};

    var state = {
        buildings: [],
        buildingId: null,
        rooms: [],
        floor: 'all',
        roomId: null,
        room: null,
        seats: [],
        selectedSeat: null
    };

    P.rooms = {
        render: function (view) {
            state.buildingId = null;
            state.rooms = [];
            state.floor = 'all';
            state.roomId = null;
            state.seats = [];
            state.selectedSeat = null;

            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">自习室预约</h2><div class="d">选择楼栋与楼层，点击空闲座位即可预约时段</div></div>' +
                '  <div class="actions"><button class="btn ghost" id="refreshRooms">刷新座位状态</button></div>' +
                '</div>' +
                '<div class="card"><div class="row between"><div class="building-tabs" id="buildingTabs">' + UI.loading() + '</div>' +
                '<div class="muted small" id="buildingInfo"></div></div></div>' +
                '<div id="floorArea" class="mt16"></div>' +
                '<div id="seatArea" class="mt16"></div>';

            document.getElementById('refreshRooms').addEventListener('click', function () {
                if (state.roomId) P.rooms.loadSeats();
                else P.rooms.loadBuildings();
            });
            this.loadBuildings();
        },

        loadBuildings: async function () {
            var tabs = document.getElementById('buildingTabs');
            try {
                state.buildings = await API.get('/buildings') || [];
            } catch (err) {
                tabs.innerHTML = '<span class="muted small">' + UI.esc(err.message || '楼栋加载失败') + '</span>';
                return;
            }
            if (!state.buildings.length) {
                tabs.innerHTML = '<span class="muted small">暂无楼栋数据，请先由管理员创建</span>';
                document.getElementById('floorArea').innerHTML = '';
                document.getElementById('seatArea').innerHTML = '';
                return;
            }
            if (!state.buildingId) state.buildingId = state.buildings[0].id;
            tabs.innerHTML = state.buildings.map(function (b) {
                return '<button type="button" class="pill' + (b.id === state.buildingId ? ' active' : '') + '" data-id="' + b.id + '">' +
                    UI.esc(b.name) + '<span class="n">' + (b.roomCount || 0) + ' 间</span></button>';
            }).join('');
            tabs.querySelectorAll('.pill').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    state.buildingId = Number(btn.dataset.id);
                    state.roomId = null; state.seats = []; state.floor = 'all';
                    document.getElementById('seatArea').innerHTML = '';
                    tabs.querySelectorAll('.pill').forEach(function (b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                    P.rooms.loadRooms();
                });
            });
            this.loadRooms();
        },

        loadRooms: async function () {
            var area = document.getElementById('floorArea');
            area.innerHTML = '<div class="card">' + UI.loading('正在加载自习室...') + '</div>';
            var building = state.buildings.filter(function (b) { return b.id === state.buildingId; })[0] || {};
            document.getElementById('buildingInfo').textContent =
                (building.location ? building.location + ' · ' : '') + '可用座位 ' + (building.availableSeats || 0);

            try {
                state.rooms = await API.get('/rooms', { buildingId: state.buildingId }) || [];
            } catch (err) {
                area.innerHTML = '<div class="card">' + UI.empty('自习室加载失败', err.message) + '</div>';
                return;
            }
            if (!state.rooms.length) {
                area.innerHTML = '<div class="card">' + UI.empty('该楼栋还没有自习室', '请联系管理员创建房间') + '</div>';
                return;
            }
            var floors = [];
            state.rooms.forEach(function (r) {
                if (floors.indexOf(r.floor) < 0) floors.push(r.floor);
            });
            floors.sort(function (a, b) { return (a || 0) - (b || 0); });

            area.innerHTML =
                '<div class="card">' +
                '  <div class="card-head"><h3>楼层分布</h3><span class="sub">共 ' + state.rooms.length + ' 间自习室</span></div>' +
                '  <div class="floor-tabs" id="floorTabs">' +
                '    <button class="pill' + (state.floor === 'all' ? ' active' : '') + '" data-floor="all">全部楼层</button>' +
                floors.map(function (f) {
                    return '<button class="pill' + (String(state.floor) === String(f) ? ' active' : '') + '" data-floor="' + UI.esc(f) + '">' +
                        (f || f === 0 ? f + ' 层' : '未标注楼层') + '</button>';
                }).join('') +
                '  </div>' +
                '  <div class="mt16" id="roomsGrid"></div>' +
                '</div>';

            document.getElementById('floorTabs').querySelectorAll('.pill').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    state.floor = btn.dataset.floor === 'all' ? 'all' : Number(btn.dataset.floor);
                    document.getElementById('floorTabs').querySelectorAll('.pill').forEach(function (b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                    P.rooms.renderRooms();
                });
            });
            this.renderRooms();
        },

        renderRooms: function () {
            var grid = document.getElementById('roomsGrid');
            var rooms = state.rooms.filter(function (r) {
                return state.floor === 'all' || String(r.floor) === String(state.floor);
            });
            if (!rooms.length) {
                grid.innerHTML = UI.empty('该楼层暂无自习室');
                return;
            }
            grid.innerHTML = '<div class="grid rooms">' + rooms.map(function (r) {
                var total = r.totalSeats || 0;
                var available = r.availableSeats || 0;
                var rate = total > 0 ? Math.round(available * 100 / total) : 0;
                return '<div class="card room-card' + (r.id === state.roomId ? ' active' : '') + '" data-id="' + r.id + '">' +
                    '<div class="row between"><div class="r-name">' + UI.esc(r.name) + '</div>' +
                    '<span class="badge ' + (available > 0 ? 'ok' : 'danger') + '">' + (available > 0 ? '有空位' : '已满') + '</span></div>' +
                    '<div class="r-meta"><span>' + (r.floor || r.floor === 0 ? r.floor + ' 层' : '楼层未标注') + '</span>' +
                    '<span>' + UI.esc(r.code || '') + '</span>' +
                    '<span>' + UI.fmtTime('2000-01-01T' + (r.openTime || '08:00:00')) + '-' + UI.fmtTime('2000-01-01T' + (r.closeTime || '22:00:00')) + '</span></div>' +
                    '<div class="r-meta"><span>空位 <b>' + available + '</b> / ' + total + '</span></div>' +
                    '<div class="r-bar"><span style="width:' + rate + '%"></span></div>' +
                    '</div>';
            }).join('') + '</div>';

            grid.querySelectorAll('.room-card').forEach(function (card) {
                card.addEventListener('click', function () {
                    state.roomId = Number(card.dataset.id);
                    state.room = state.rooms.filter(function (r) { return r.id === state.roomId; })[0];
                    grid.querySelectorAll('.room-card').forEach(function (c) { c.classList.remove('active'); });
                    card.classList.add('active');
                    P.rooms.loadSeats();
                });
            });
        },

        loadSeats: async function () {
            var area = document.getElementById('seatArea');
            var r = state.room;
            if (!r) return;
            area.innerHTML = '<div class="card">' + UI.loading('正在加载座位...') + '</div>';
            try {
                state.seats = await API.get('/seats', { roomId: r.id }) || [];
            } catch (err) {
                area.innerHTML = '<div class="card">' + UI.empty('座位加载失败', err.message) + '</div>';
                return;
            }

            var maxRow = 0, maxCol = 0;
            state.seats.forEach(function (s) {
                maxRow = Math.max(maxRow, s.row || 0);
                maxCol = Math.max(maxCol, s.col || 0);
            });
            var useGrid = maxRow > 0 && maxCol > 0;
            var cols = useGrid ? maxCol : Math.min(10, Math.max(state.seats.length, 1));

            area.innerHTML =
                '<div class="card">' +
                '  <div class="card-head"><h3>' + UI.esc(r.buildingName || '') + ' · ' + UI.esc(r.name) + ' · 座位图</h3>' +
                '    <span class="sub">共 ' + state.seats.length + ' 个座位</span>' +
                '    <div class="actions"><div class="legend">' +
                '      <span class="item"><span class="sw" style="background:#e9fbf2;border-color:#cfe9dc"></span>空闲</span>' +
                '      <span class="item"><span class="sw" style="background:#feeceb;border-color:#f8c9c4"></span>使用中</span>' +
                '      <span class="item"><span class="sw" style="background:#fff6e6;border-color:#f7d9a8"></span>已预约</span>' +
                '      <span class="item"><span class="sw" style="background:#eef0f4;border-color:#d8dde6"></span>维护中</span>' +
                '    </div></div>' +
                '  </div>' +
                '  <div class="row small muted" style="margin-bottom:10px">' +
                '    <span>开放时间 ' + UI.fmtTime('2000-01-01T' + (r.openTime || '08:00:00')) + ' - ' + UI.fmtTime('2000-01-01T' + (r.closeTime || '22:00:00')) + '</span>' +
                (r.equipment ? '<span>· 设施：' + UI.esc(r.equipment) + '</span>' : '') +
                '  </div>' +
                (state.seats.length ? '' : UI.empty('该自习室还没有座位', '管理员可在后台批量生成座位')) +
                '  <div class="seat-map" id="seatMap" style="grid-template-columns:repeat(' + cols + ',minmax(38px,1fr))"></div>' +
                '</div>';

            var map = document.getElementById('seatMap');
            if (!map || !state.seats.length) return;

            var html = '';
            if (useGrid) {
                for (var rr = 1; rr <= maxRow; rr++) {
                    for (var cc = 1; cc <= maxCol; cc++) {
                        var s = state.seats.filter(function (x) { return x.row === rr && x.col === cc; })[0];
                        html += s ? seatHtml(s) : '<div class="seat map-aisle" style="background:transparent;border:0;cursor:default"></div>';
                    }
                }
            } else {
                html = state.seats.map(seatHtml).join('');
            }
            map.innerHTML = html;

            map.querySelectorAll('.seat[data-id]').forEach(function (el) {
                el.addEventListener('click', function () {
                    var id = Number(el.dataset.id);
                    var seat = state.seats.filter(function (s) { return s.id === id; })[0];
                    if (!seat || seat.status !== 0) {
                        if (seat) UI.toast('该座位当前不可预约（' + (UI.SEAT_STATUS[seat.status] || {}).label + '）', 'warn');
                        return;
                    }
                    state.selectedSeat = seat;
                    map.querySelectorAll('.seat').forEach(function (x) { x.classList.remove('selected'); });
                    el.classList.add('selected');
                    P.rooms.openBooking(seat);
                });
            });
        },

        openBooking: function (seat) {
            var form = document.createElement('div');
            form.innerHTML =
                '<div class="row between" style="gap:12px">' +
                '  <div><div class="muted small">已选座位</div><div style="font-size:17px;font-weight:700">' + UI.esc(seat.seatNo) + '</div></div>' +
                '  <div class="right"><div class="muted small">自习室</div><div class="small">' + UI.esc((state.room && state.room.buildingName) || '') + ' ' + UI.esc((state.room && state.room.name) || '') + '</div></div>' +
                '</div>' +
                '<div class="divider"></div>' +
                '<div class="grid c2" style="gap:12px">' +
                '  <div class="field mb0"><label>开始时间</label><input class="input" type="datetime-local" id="bkStart" value="' + UI.nowLocalInput(0) + '"></div>' +
                '  <div class="field mb0"><label>结束时间</label><input class="input" type="datetime-local" id="bkEnd" value="' + UI.nowLocalInput(120) + '"></div>' +
                '</div>' +
                '<div class="row tight mt12" id="quickDur">' +
                ['1 小时', '2 小时', '3 小时', '4 小时'].map(function (t, i) {
                    return '<button type="button" class="pill" data-min="' + ((i + 1) * 60) + '">' + t + '</button>';
                }).join('') +
                '</div>' +
                '<div class="field mt12 mb0"><label>备注</label><input class="input" id="bkRemark" placeholder="选填，例如 备考复习"></div>' +
                '<div class="small muted mt12">签到请在开始时间前后 30 分钟内完成，超时座位会自动释放。</div>';

            var m = UI.modal({
                title: '预约座位',
                body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: '确认预约', cls: '', onClick: async function (x, btn) {
                            var start = document.getElementById('bkStart').value;
                            var end = document.getElementById('bkEnd').value;
                            var remark = document.getElementById('bkRemark').value.trim();
                            if (!start || !end) { UI.toast('请选择完整的时段', 'warn'); return; }
                            if (start >= end) { UI.toast('结束时间需晚于开始时间', 'warn'); return; }
                            btn.disabled = true; btn.textContent = '提交中...';
                            try {
                                var vo = await API.post('/bookings', {
                                    seatId: seat.id,
                                    startTime: start.length === 16 ? start + ':00' : start,
                                    endTime: end.length === 16 ? end + ':00' : end,
                                    remark: remark || null
                                });
                                x.close();
                                UI.toast('预约成功，签到码 ' + (vo.checkinCode || ''), 'ok');
                                P.rooms.loadSeats();
                                setTimeout(function () {
                                    UI.confirm('预约已创建，是否前往「我的预约」查看？', { okText: '去查看' }).then(function (ok) {
                                        if (ok) App.go('bookings');
                                    });
                                }, 400);
                            } catch (err) {
                                UI.toast(err.message || '预约失败', 'err');
                                btn.disabled = false; btn.textContent = '确认预约';
                            }
                        }
                    }
                ]
            });

            form.querySelectorAll('#quickDur .pill').forEach(function (b) {
                b.addEventListener('click', function () {
                    form.querySelectorAll('#quickDur .pill').forEach(function (x) { x.classList.remove('active'); });
                    b.classList.add('active');
                    var startInput = document.getElementById('bkStart');
                    if (!startInput.value) startInput.value = UI.nowLocalInput(0);
                    var mins = Number(b.dataset.min);
                    var d = new Date(startInput.value);
                    var t = new Date(d.getTime() + mins * 60000);
                    var pp = function (n) { return n < 10 ? '0' + n : n; };
                    document.getElementById('bkEnd').value = t.getFullYear() + '-' + pp(t.getMonth() + 1) + '-' + pp(t.getDate()) +
                        'T' + pp(t.getHours()) + ':' + pp(t.getMinutes());
                });
            });
        }
    };

    function seatHtml(s) {
        var st = UI.SEAT_STATUS[s.status] || { label: '未知', cls: '' };
        var cls = s.status === 0 ? '' : 's' + s.status;
        return '<div class="seat ' + cls + '" data-id="' + s.id + '" title="' + UI.esc(s.seatNo) + ' · ' + st.label + '">' +
            '<span class="no">' + UI.esc(s.seatNo) + '</span></div>';
    }
})();
