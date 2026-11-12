/* 管理后台：房间/座位管理、使用率、违规记录、用户统计 */
(function () {
    var P = window.Pages = window.Pages || {};

    var state = {
        buildings: [], rooms: [], utilization: [], seats: [],
        buildingId: null, seatRoomId: null,
        page: 1, size: 8, total: 0
    };

    function padTime(t) {
        if (!t) return null;
        return t.length === 5 ? t + ':00' : t;
    }

    function showTime(t) {
        return t ? String(t).substring(0, 5) : '';
    }

    P.admin = {
        render: function (view) {
            var user = API.user() || {};
            if (user.role !== 1) {
                view.innerHTML = '<div class="card">' + UI.empty('没有访问权限', '管理后台仅对管理员开放') + '</div>';
                return;
            }
            state.buildingId = null; state.seatRoomId = null; state.page = 1;

            view.innerHTML =
                '<div class="page-head">' +
                '  <div><h2 class="t">管理后台</h2><div class="d">自习室资源、使用率与违规记录</div></div>' +
                '  <div class="actions"><button class="btn ghost" id="reloadAdmin">刷新数据</button></div>' +
                '</div>' +
                '<div class="grid c4" id="adminKpis"></div>' +
                '<div class="grid side mt16">' +
                '  <div class="card">' +
                '    <div class="card-head"><h3>自习室管理</h3>' +
                '      <div class="actions"><button class="btn ghost sm" id="newBuilding">新建楼栋</button>' +
                '      <button class="btn sm" id="newRoom">新建自习室</button></div></div>' +
                '    <div class="row" style="margin-bottom:12px"><select class="select" id="adminBuildingFilter" style="max-width:220px"></select></div>' +
                '    <div class="table-wrap"><table class="tb"><thead><tr><th>自习室</th><th>楼层</th><th>座位</th><th>空闲</th><th>开放时间</th><th style="width:190px">操作</th></tr></thead>' +
                '    <tbody id="roomTable"></tbody></table></div>' +
                '  </div>' +
                '  <div class="card"><div class="card-head"><h3>座位使用率</h3><span class="sub">按自习室统计</span></div><div id="utilBox">' + UI.loading() + '</div></div>' +
                '</div>' +
                '<div class="card mt16">' +
                '  <div class="card-head"><h3>座位管理</h3>' +
                '    <div class="actions"><select class="select" id="seatRoomSelect" style="min-width:200px"></select>' +
                '    <button class="btn ghost sm" id="batchSeat">批量生成</button>' +
                '    <button class="btn sm" id="newSeat">新增座位</button></div></div>' +
                '  <div class="row small muted" style="margin-bottom:10px">点击座位可切换空闲 / 维护状态，或删除座位</div>' +
                '  <div id="seatAdminBox">' + UI.empty('请选择自习室') + '</div>' +
                '</div>' +
                '<div class="grid side mt16">' +
                '  <div class="card"><div class="card-head"><h3>违规与异常预约</h3><span class="sub">已过期 / 已取消</span></div>' +
                '    <div class="table-wrap"><table class="tb"><thead><tr><th>用户</th><th>座位</th><th>时段</th><th>状态</th></tr></thead><tbody id="violationTable"></tbody></table></div>' +
                '    <div class="row between mt12"><span class="small muted" id="vInfo"></span>' +
                '    <div class="row tight"><button class="btn ghost sm" id="vPrev">上一页</button><button class="btn ghost sm" id="vNext">下一页</button></div></div>' +
                '  </div>' +
                '  <div class="card"><div class="card-head"><h3>用户与学习统计</h3></div><div id="userStatsBox">' + UI.loading() + '</div></div>' +
                '</div>';

            document.getElementById('reloadAdmin').addEventListener('click', function () { App.reload(); });
            document.getElementById('newBuilding').addEventListener('click', function () { P.admin.openBuildingForm(); });
            document.getElementById('newRoom').addEventListener('click', function () { P.admin.openRoomForm(null); });
            document.getElementById('batchSeat').addEventListener('click', function () { P.admin.openBatchSeat(); });
            document.getElementById('newSeat').addEventListener('click', function () { P.admin.openSeatForm(); });
            document.getElementById('vPrev').addEventListener('click', function () {
                if (state.page > 1) { state.page--; P.admin.loadViolations(); }
            });
            document.getElementById('vNext').addEventListener('click', function () {
                if (state.page * state.size < state.total) { state.page++; P.admin.loadViolations(); }
            });

            this.loadAll();
        },

        loadAll: async function () {
            var res;
            try {
                res = await Promise.all([
                    API.get('/admin/dashboard'),
                    API.get('/buildings'),
                    API.get('/rooms'),
                    API.get('/admin/utilization'),
                    API.get('/admin/user-stats')
                ]);
            } catch (err) {
                UI.toast(err.message || '管理数据加载失败', 'err');
                return;
            }
            var dash = res[0] || {};
            state.buildings = res[1] || [];
            state.rooms = res[2] || [];
            state.utilization = (res[3] && res[3].rooms) || [];
            var us = res[4] || {};

            function kpi(label, value, unit, sub, icon, cls) {
                return '<div class="kpi ' + (cls || '') + '"><div class="k-ico">' + UI.icon(icon) + '</div>' +
                    '<div class="k-label">' + label + '</div><div class="k-value">' + value + (unit ? '<small>' + unit + '</small>' : '') + '</div>' +
                    (sub ? '<div class="k-sub">' + sub + '</div>' : '') + '</div>';
            }
            document.getElementById('adminKpis').innerHTML =
                kpi('注册用户', dash.userCount || 0, '人', '本周活跃 ' + (us.weekActiveUsers || 0) + ' 人', 'users', 'info') +
                kpi('自习室', dash.roomCount || 0, '间', '楼栋 ' + (dash.buildingCount || 0) + ' 栋', 'rooms', '') +
                kpi('座位总数', dash.seatCount || 0, '个', '今日预约 ' + (dash.todayBookings || 0) + ' 次', 'seat', 'ok') +
                kpi('今日学习', UI.hours(dash.todayTotalMinutes || 0), '小时', '活跃 ' + (dash.activeUsers || 0) + ' 人', 'clock', 'warn');

            var filter = document.getElementById('adminBuildingFilter');
            filter.innerHTML = '<option value="">全部楼栋</option>' + state.buildings.map(function (b) {
                return '<option value="' + b.id + '">' + UI.esc(b.name) + '</option>';
            }).join('');
            filter.addEventListener('change', function () {
                state.buildingId = filter.value ? Number(filter.value) : null;
                P.admin.renderRoomTable();
            });

            this.renderRoomTable();
            this.renderUtilization();

            var stats = document.getElementById('userStatsBox');
            stats.innerHTML =
                '<div class="row between" style="padding:10px 0;border-bottom:1px solid #f1f4f9"><span class="muted small">注册用户</span><b>' + (us.totalUsers || 0) + ' 人</b></div>' +
                '<div class="row between" style="padding:10px 0;border-bottom:1px solid #f1f4f9"><span class="muted small">本周活跃用户</span><b>' + (us.weekActiveUsers || 0) + ' 人</b></div>' +
                '<div class="row between" style="padding:10px 0;border-bottom:1px solid #f1f4f9"><span class="muted small">累计学习时长</span><b>' + UI.hours(us.totalStudyMinutes || 0) + ' 小时</b></div>' +
                '<div class="row between" style="padding:10px 0"><span class="muted small">人均学习时长</span><b>' +
                UI.hours(us.totalUsers ? (us.totalStudyMinutes || 0) / us.totalUsers : 0) + ' 小时</b></div>' +
                '<div class="divider"></div>' +
                '<div class="small muted">统计口径：学习计时与预约签到产生的打卡记录</div>';

            var select = document.getElementById('seatRoomSelect');
            select.innerHTML = '<option value="">请选择自习室</option>' + state.rooms.map(function (r) {
                return '<option value="' + r.id + '">' + UI.esc((r.buildingName ? r.buildingName + ' · ' : '') + r.name) + '</option>';
            }).join('');
            select.addEventListener('change', function () {
                state.seatRoomId = select.value ? Number(select.value) : null;
                P.admin.loadSeats();
            });
            if (state.seatRoomId) select.value = String(state.seatRoomId);

            this.loadViolations();
        },

        renderRoomTable: function () {
            var tb = document.getElementById('roomTable');
            if (!tb) return;
            var rooms = state.rooms.filter(function (r) {
                return !state.buildingId || r.buildingId === state.buildingId;
            });
            if (!rooms.length) {
                tb.innerHTML = '<tr><td colspan="6" class="empty-row">暂无自习室，点击右上角新建</td></tr>';
                return;
            }
            tb.innerHTML = rooms.map(function (r) {
                return '<tr>' +
                    '<td><b>' + UI.esc(r.name) + '</b><div class="small muted">' + UI.esc(r.buildingName || '') + (r.code ? ' · ' + UI.esc(r.code) : '') + '</div></td>' +
                    '<td>' + (r.floor != null ? r.floor + ' 层' : '--') + '</td>' +
                    '<td>' + (r.totalSeats || 0) + '</td>' +
                    '<td><span class="badge ' + ((r.availableSeats || 0) > 0 ? 'ok' : 'danger') + '">' + (r.availableSeats || 0) + '</span></td>' +
                    '<td class="small">' + showTime(r.openTime) + ' - ' + showTime(r.closeTime) + '</td>' +
                    '<td><div class="row tight">' +
                    '<button class="btn ghost sm" data-act="edit" data-id="' + r.id + '">编辑</button>' +
                    '<button class="btn ghost sm" data-act="seats" data-id="' + r.id + '">座位</button>' +
                    '<button class="btn ghost sm" data-act="del" data-id="' + r.id + '">删除</button>' +
                    '</div></td></tr>';
            }).join('');

            tb.querySelectorAll('[data-act="edit"]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var room = state.rooms.filter(function (r) { return r.id === Number(btn.dataset.id); })[0];
                    P.admin.openRoomForm(room);
                });
            });
            tb.querySelectorAll('[data-act="seats"]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    state.seatRoomId = Number(btn.dataset.id);
                    var sel = document.getElementById('seatRoomSelect');
                    sel.value = String(state.seatRoomId);
                    P.admin.loadSeats();
                    document.getElementById('seatAdminBox').scrollIntoView({ behavior: 'smooth', block: 'center' });
                });
            });
            tb.querySelectorAll('[data-act="del"]').forEach(function (btn) {
                btn.addEventListener('click', async function () {
                    var ok = await UI.confirm('删除自习室会同时影响其座位与预约，确定删除吗？', { okText: '删除', danger: true });
                    if (!ok) return;
                    try {
                        await API.del('/rooms/' + btn.dataset.id);
                        UI.toast('自习室已删除', 'ok');
                        App.reload();
                    } catch (err) { UI.toast(err.message, 'err'); }
                });
            });
        },

        renderUtilization: function () {
            var box = document.getElementById('utilBox');
            if (!state.utilization.length) {
                box.innerHTML = UI.empty('暂无使用率数据');
                return;
            }
            box.innerHTML = '<div style="display:grid;gap:14px">' + state.utilization.map(function (u) {
                var pct = Math.round((u.utilizationRate || 0) * 100);
                return '<div><div class="row between small" style="margin-bottom:6px">' +
                    '<span style="font-weight:600">' + UI.esc(u.roomName) + '</span>' +
                    '<span class="muted">' + pct + '% · 空闲 ' + (u.availableSeats || 0) + '/' + (u.totalSeats || 0) + '</span></div>' +
                    '<div class="progress ' + (pct > 85 ? 'warn' : 'ok') + '"><span style="width:' + Math.min(100, pct) + '%"></span></div></div>';
            }).join('') + '</div>';
            box.innerHTML += '<div class="small muted mt12">使用率 =（总座位 - 空闲座位）/ 总座位，实时计算</div>';
        },

        loadSeats: async function () {
            var box = document.getElementById('seatAdminBox');
            if (!state.seatRoomId) { box.innerHTML = UI.empty('请选择自习室'); return; }
            box.innerHTML = UI.loading();
            try {
                state.seats = await API.get('/seats', { roomId: state.seatRoomId }) || [];
            } catch (err) {
                box.innerHTML = UI.empty('座位加载失败', err.message);
                return;
            }
            if (!state.seats.length) {
                box.innerHTML = UI.empty('该自习室暂无座位', '可用「批量生成」快速创建座位');
                return;
            }
            var maxRow = 0, maxCol = 0;
            state.seats.forEach(function (s) {
                maxRow = Math.max(maxRow, s.row || 0);
                maxCol = Math.max(maxCol, s.col || 0);
            });
            var grid = maxRow > 0 && maxCol > 0;
            var cols = grid ? maxCol : Math.min(10, state.seats.length);

            box.innerHTML = '<div class="legend" style="margin-bottom:10px">' +
                '  <span class="item"><span class="sw" style="background:#e9fbf2;border-color:#cfe9dc"></span>空闲</span>' +
                '  <span class="item"><span class="sw" style="background:#feeceb;border-color:#f8c9c4"></span>使用中</span>' +
                '  <span class="item"><span class="sw" style="background:#fff6e6;border-color:#f7d9a8"></span>已预约</span>' +
                '  <span class="item"><span class="sw" style="background:#eef0f4;border-color:#d8dde6"></span>维护中</span></div>' +
                '<div class="seat-map" id="adminSeatMap" style="grid-template-columns:repeat(' + cols + ',minmax(42px,1fr))"></div>';

            var map = document.getElementById('adminSeatMap');
            var html = '';
            if (grid) {
                for (var r = 1; r <= maxRow; r++) {
                    for (var c = 1; c <= maxCol; c++) {
                        var s = state.seats.filter(function (x) { return x.row === r && x.col === c; })[0];
                        html += s ? seatCell(s) : '<div class="seat map-aisle" style="background:transparent;border:0"></div>';
                    }
                }
            } else {
                html = state.seats.map(seatCell).join('');
            }
            map.innerHTML = html;
            map.querySelectorAll('.seat[data-id]').forEach(function (el) {
                el.addEventListener('click', function () {
                    P.admin.openSeatActions(Number(el.dataset.id));
                });
            });
        },

        openSeatActions: function (id) {
            var seat = state.seats.filter(function (s) { return s.id === id; })[0];
            if (!seat) return;
            var st = UI.SEAT_STATUS[seat.status] || {};
            var form = document.createElement('div');
            form.innerHTML = '<div class="row between"><div><div class="muted small">座位号</div>' +
                '<div style="font-size:17px;font-weight:700">' + UI.esc(seat.seatNo) + '</div></div>' +
                '<div class="right"><div class="muted small">当前状态</div><div>' + UI.badge(UI.SEAT_STATUS, seat.status) + '</div></div></div>' +
                '<div class="divider"></div>' +
                '<div class="small muted">二维码标识：<span class="mono">' + UI.esc(seat.qrcode || '--') + '</span></div>' +
                '<div class="row mt12"><button class="btn sm ok" id="setFree">设为空闲</button>' +
                '<button class="btn sm warn" id="setMaint">设为维护</button>' +
                '<button class="btn sm danger" id="delSeat">删除座位</button></div>' +
                '<div class="small muted mt12">使用中 / 已预约的座位由预约流程自动维护，不建议手动修改。</div>';

            var m = UI.modal({ title: '座位操作', body: form });
            form.querySelector('#setFree').addEventListener('click', async function () {
                try {
                    await API.put('/seats/' + seat.id + '/status', null, { status: 0 });
                    UI.toast('已设为空闲', 'ok'); m.close(); P.admin.loadSeats();
                } catch (err) { UI.toast(err.message, 'err'); }
            });
            form.querySelector('#setMaint').addEventListener('click', async function () {
                try {
                    await API.put('/seats/' + seat.id + '/status', null, { status: 3 });
                    UI.toast('已设为维护', 'ok'); m.close(); P.admin.loadSeats();
                } catch (err) { UI.toast(err.message, 'err'); }
            });
            form.querySelector('#delSeat').addEventListener('click', async function () {
                var ok = await UI.confirm('删除座位 ' + seat.seatNo + ' 吗？', { okText: '删除', danger: true });
                if (!ok) return;
                try {
                    await API.del('/seats/' + seat.id);
                    UI.toast('座位已删除', 'ok'); m.close(); P.admin.loadSeats();
                } catch (err) { UI.toast(err.message, 'err'); }
            });
        },

        openBuildingForm: function () {
            var form = document.createElement('div');
            form.innerHTML = '<div class="field"><label>楼栋名称</label><input class="input" id="bName" placeholder="例如：图书馆"></div>' +
                '<div class="grid c2" style="gap:12px">' +
                '  <div class="field"><label>楼栋编码</label><input class="input" id="bCode" placeholder="例如：LIB"></div>' +
                '  <div class="field"><label>位置</label><input class="input" id="bLoc" placeholder="例如：东校区中心"></div>' +
                '</div>' +
                '<div class="field"><label>描述</label><input class="input" id="bDesc" placeholder="选填"></div>';

            UI.modal({
                title: '新建楼栋', body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: '创建', cls: '', onClick: async function (x, btn) {
                            var payload = {
                                name: document.getElementById('bName').value.trim(),
                                code: document.getElementById('bCode').value.trim() || null,
                                location: document.getElementById('bLoc').value.trim() || null,
                                description: document.getElementById('bDesc').value.trim() || null
                            };
                            if (!payload.name) { UI.toast('请填写楼栋名称', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                await API.post('/buildings', payload);
                                UI.toast('楼栋已创建', 'ok');
                                x.close(); App.reload();
                            } catch (err) { UI.toast(err.message, 'err'); btn.disabled = false; }
                        }
                    }
                ]
            });
        },

        openRoomForm: function (room) {
            var isEdit = !!room;
            var form = document.createElement('div');
            form.innerHTML =
                '<div class="field"><label>所属楼栋</label><select class="select" id="rBuilding">' +
                state.buildings.map(function (b) {
                    var sel = (room && room.buildingId === b.id) || (!room && state.buildingId === b.id) ? ' selected' : '';
                    return '<option value="' + b.id + '"' + sel + '>' + UI.esc(b.name) + '</option>';
                }).join('') + '</select></div>' +
                '<div class="grid c2" style="gap:12px">' +
                '  <div class="field"><label>自习室名称</label><input class="input" id="rName" value="' + UI.esc(room ? room.name : '') + '" placeholder="例如：三楼自习室 A"></div>' +
                '  <div class="field"><label>编号</label><input class="input" id="rCode" value="' + UI.esc(room ? (room.code || '') : '') + '" placeholder="例如：R301"></div>' +
                '</div>' +
                '<div class="grid c3" style="gap:12px">' +
                '  <div class="field"><label>楼层</label><input class="input" id="rFloor" type="number" min="0" value="' + (room && room.floor != null ? room.floor : 1) + '"></div>' +
                '  <div class="field"><label>座位总数</label><input class="input" id="rSeats" type="number" min="0" value="' + (room && room.totalSeats != null ? room.totalSeats : 0) + '"></div>' +
                '  <div class="field"><label>设施</label><input class="input" id="rEquip" value="' + UI.esc(room ? (room.equipment || '') : '') + '" placeholder="空调/WiFi"></div>' +
                '</div>' +
                '<div class="grid c2" style="gap:12px">' +
                '  <div class="field"><label>开放时间</label><input class="input" id="rOpen" type="time" value="' + (room ? showTime(room.openTime) : '08:00') + '"></div>' +
                '  <div class="field"><label>关闭时间</label><input class="input" id="rClose" type="time" value="' + (room ? showTime(room.closeTime) : '22:00') + '"></div>' +
                '</div>';

            UI.modal({
                title: isEdit ? '编辑自习室' : '新建自习室', body: form, wide: false,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: isEdit ? '保存' : '创建', cls: '', onClick: async function (x, btn) {
                            var payload = {
                                buildingId: Number(document.getElementById('rBuilding').value),
                                name: document.getElementById('rName').value.trim(),
                                code: document.getElementById('rCode').value.trim() || null,
                                floor: Number(document.getElementById('rFloor').value || 0),
                                totalSeats: Number(document.getElementById('rSeats').value || 0),
                                equipment: document.getElementById('rEquip').value.trim() || null,
                                openTime: padTime(document.getElementById('rOpen').value),
                                closeTime: padTime(document.getElementById('rClose').value)
                            };
                            if (!payload.buildingId) { UI.toast('请先创建楼栋', 'warn'); return; }
                            if (!payload.name) { UI.toast('请填写自习室名称', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                if (isEdit) await API.put('/rooms/' + room.id, payload);
                                else await API.post('/rooms', payload);
                                UI.toast(isEdit ? '已保存' : '自习室已创建', 'ok');
                                x.close(); App.reload();
                            } catch (err) { UI.toast(err.message, 'err'); btn.disabled = false; }
                        }
                    }
                ]
            });
        },

        openBatchSeat: function () {
            if (!state.seatRoomId) { UI.toast('请先在下方选择自习室', 'warn'); return; }
            var form = document.createElement('div');
            form.innerHTML = '<div class="grid c2" style="gap:12px">' +
                '<div class="field"><label>行数</label><input class="input" id="bRows" type="number" min="1" value="6"></div>' +
                '<div class="field"><label>列数</label><input class="input" id="bCols" type="number" min="1" value="8"></div></div>' +
                '<div class="small muted">将按 行×列 生成座位号，已存在的座位会自动跳过。</div>';
            UI.modal({
                title: '批量生成座位', body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: '生成', cls: '', onClick: async function (x, btn) {
                            var rows = Number(document.getElementById('bRows').value);
                            var cols = Number(document.getElementById('bCols').value);
                            if (!rows || !cols || rows < 1 || cols < 1) { UI.toast('请输入有效的行列数', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                await API.post('/seats/batch', null, { roomId: state.seatRoomId, rows: rows, cols: cols });
                                UI.toast('座位已生成', 'ok');
                                x.close();
                                P.admin.loadSeats();
                            } catch (err) { UI.toast(err.message, 'err'); btn.disabled = false; }
                        }
                    }
                ]
            });
        },

        openSeatForm: function () {
            if (!state.seatRoomId) { UI.toast('请先在下方选择自习室', 'warn'); return; }
            var form = document.createElement('div');
            form.innerHTML = '<div class="grid c3" style="gap:12px">' +
                '<div class="field"><label>座位号</label><input class="input" id="sNo" placeholder="例如 A-01"></div>' +
                '<div class="field"><label>行</label><input class="input" id="sRow" type="number" min="1" value="1"></div>' +
                '<div class="field"><label>列</label><input class="input" id="sCol" type="number" min="1" value="1"></div></div>' +
                '<div class="small muted">座位号需在自习室内唯一；行/列决定座位图位置。</div>';
            UI.modal({
                title: '新增座位', body: form,
                footer: [
                    { text: '取消', cls: 'ghost', onClick: function (x) { x.close(); } },
                    {
                        text: '创建', cls: '', onClick: async function (x, btn) {
                            var payload = {
                                roomId: state.seatRoomId,
                                seatNo: document.getElementById('sNo').value.trim(),
                                row: Number(document.getElementById('sRow').value || 1),
                                col: Number(document.getElementById('sCol').value || 1),
                                status: 0
                            };
                            if (!payload.seatNo) { UI.toast('请填写座位号', 'warn'); return; }
                            btn.disabled = true;
                            try {
                                await API.post('/seats', payload);
                                UI.toast('座位已创建', 'ok');
                                x.close(); P.admin.loadSeats();
                            } catch (err) { UI.toast(err.message, 'err'); btn.disabled = false; }
                        }
                    }
                ]
            });
        },

        loadViolations: async function () {
            var tb = document.getElementById('violationTable');
            var page;
            try {
                page = await API.get('/admin/bookings', { current: state.page, size: state.size });
            } catch (err) {
                tb.innerHTML = '<tr><td colspan="4" class="empty-row">' + UI.esc(err.message || '加载失败') + '</td></tr>';
                return;
            }
            state.total = page.total || 0;
            var records = page.records || [];
            document.getElementById('vInfo').textContent = '共 ' + state.total + ' 条 · 第 ' + state.page + ' 页';
            if (!records.length) {
                tb.innerHTML = '<tr><td colspan="4" class="empty-row">暂无违规或异常记录</td></tr>';
                return;
            }
            tb.innerHTML = records.map(function (b) {
                return '<tr>' +
                    '<td>' + UI.esc(b.userNickname || ('用户 ' + b.userId)) + '</td>' +
                    '<td><div>' + UI.esc(b.seatNo || '--') + '</div><div class="small muted">' + UI.esc(b.buildingName || '') + ' ' + UI.esc(b.roomName || '') + '</div></td>' +
                    '<td class="small">' + UI.fmtDate(b.startTime) + '<br>' + UI.fmtTime(b.startTime) + ' - ' + UI.fmtTime(b.endTime) + '</td>' +
                    '<td>' + UI.badge(UI.BOOKING_STATUS, b.status) + '</td>' +
                    '</tr>';
            }).join('');
        }
    };

    function seatCell(s) {
        var cls = s.status === 0 ? '' : 's' + s.status;
        return '<div class="seat ' + cls + '" data-id="' + s.id + '" title="' + UI.esc(s.seatNo) + '">' +
            '<span class="no">' + UI.esc(s.seatNo) + '</span></div>';
    }
})();
