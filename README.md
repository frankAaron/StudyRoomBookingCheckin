# 自习室预约与学习打卡系统（StudyRoomBookingCheckin）

基于 Spring Boot 3 + Vue 3 + Element Plus + ECharts 的校园自习室预约与学习管理平台，覆盖「选座预约 → 签到码签到 → 学习计时 → 数据统计 → 目标激励 → 排行监督 → 后台管理」的完整闭环。

## 1. 项目简介

系统面向高校学生与自习室管理员：

- **学生端**：按楼栋/楼层浏览自习室，查看实时座位状态并按时段预约；签到码签到后自动开始学习计时；离座释放座位并生成打卡记录；查看每日/每周时长统计、热力图、学习目标、连续打卡记录、排行榜与好友监督。
- **管理端**：维护楼栋、自习室与座位，批量生成座位、切换维护状态；查看预约违规/异常记录、自习室使用率与用户学习统计。

## 2. 功能特性

| 模块         | 功能说明                                                                          |
| ------------ | --------------------------------------------------------------------------------- |
| 自习室可视化 | 楼栋 → 楼层 → 自习室 → 座位图逐级展示，座位四色状态（空闲/使用中/已预约/维护） |
| 时段预约     | 选择空闲座位按时段预约，时长可选 1~6 小时（60~360 分钟，按小时递增），支持备注 |
| 签到码签到   | 预约生成 8 位数字签到码，支持按签到码或预约 ID 签到；签到后座位进入使用中并自动开始计时 |
| 离座释放     | 离座后释放座位，同时结束对应的进行中学习打卡                                      |
| 学习计时     | 秒级实时计时，可手动结束并保存打卡记录                                            |
| 数据统计     | 今日/本周/本月/累计时长、近 30 天折线趋势、52 周日历热力图、星期分布              |
| 学习目标     | 设定目标时长与周期，按周期内打卡时长自动计算进度并标记达成                        |
| 排行榜       | 今日/本周/本月/累计四维度榜单，展示我的排名                                       |
| 好友监督     | 按用户 ID 添加好友、处理申请、解除好友，查看好友今日时长                          |
| 违规记录     | 未签到爽约、超时占座自动判定入库，管理端可筛选与标记处理                          |
| 个人中心     | 查看/编辑资料、修改密码、上传头像                                                 |
| 管理后台     | 楼栋/自习室/座位增删改、批量生成座位、座位维护、预约记录、使用率与用户统计         |
| 用户管理     | 管理员新增/编辑/禁用/删除用户，查看管理员操作日志                                  |

## 3. 技术栈

**后端**

- Spring Boot 3.3.5 / Java 21
- MyBatis-Plus 3.5.7（逻辑删除、分页插件）
- MySQL 8（库名 `study_room`，utf8mb4_0900_ai_ci；唯一索引 + 生成列保证预约时段原子唯一）
- Redis（座位实时状态 Hash 缓存 + 日/周/月/总排行榜 String 缓存，含空值防穿透、互斥锁防击穿、TTL 随机抖动）
- JWT（jjwt 0.11）无状态登录，拦截器鉴权 + `@RequireAdmin` 注解做管理端角色校验
- 登录限流（账号 5 次 / IP 20 次 / 10 分钟）+ Redis 分布式锁保护定时任务
- Hutool BCrypt 密码加密、Lombok
- 测试：JUnit 5 + Mockito + AssertJ

**前端**

- Vue 3.5 + Vue Router 4（History 路由，base `/api/`，地址形如 `/api/login`）+ Element Plus 2.8 + ECharts 5.5
- Node.js + Vite 5 构建，产物输出至 `src/main/resources/static`（由后端统一托管）
- 基于 `fetch` 的请求封装，统一注入 Token、处理 401/403 与业务错误码

## 4. 项目结构

```
StudyRoomBookingCheckin
├── pom.xml
├── README.md
├── frontend/                 # Vue 3 前端工程（Node.js + Vite）
│   ├── package.json
│   ├── vite.config.js        # build.outDir 指向 ../src/main/resources/static
│   ├── index.html
│   └── src
│       ├── main.js / App.vue / router.js / store.js / api.js / styles.css
│       └── views/            # Login/Home/Rooms/Bookings/Study/Stats/Goals/Ranking/Admin/Users
└── src
    ├── main
    │   ├── java/com/tyut/studyroombookingcheckin
    │   │   ├── StudyRoomBookingCheckinApplication.java   # 启动类
    │   │   ├── cache/           # SeatCacheService、RankingCacheService、RedisLock
    │   │   ├── common/          # 统一响应 Result、业务异常、错误码、常量
    │   │   ├── config/          # WebMvc、MyBatis-Plus、Redis 配置
    │   │   ├── controller/      # Auth/User/Building/Room/Seat/Booking/Checkin/Goal/Friend/Ranking/Admin
    │   │   ├── dto/ vo/         # 入参与出参对象
    │   │   ├── entity/          # User、Building、Room、Seat、Booking、StudyCheckin、StudyGoal、Friend、Violation、AdminOperationLog
    │   │   ├── mapper/          # MyBatis-Plus Mapper
    │   │   ├── security/        # JwtUtil、RequireAdmin、JwtInterceptor、LoginRateLimiter
    │   │   └── service/         # 业务接口与实现
    │   └── resources
    │       ├── application.yml  # 端口、数据源、Redis、JWT 配置（敏感项来自环境变量）
    │       ├── schema.sql       # 建库建表与初始化数据
    │       └── static           # Vue 构建产物（index.html + assets/）
    └── test/java/.../service/impl  # 预约、统计、排行榜单元测试（含并发用例）
```

## 5. 快速开始

### 5.1 环境要求

- JDK 21
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+（需先启动）
- Node.js 18+（仅重新构建前端时需要）

### 5.2 初始化数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

脚本会创建数据库 `study_room`、全部数据表（含唯一索引与复合索引），并写入管理员账号与示例楼栋/自习室；座位可在管理端「批量生成座位」按行列创建。

### 5.3 配置数据源与密钥

编辑 `src/main/resources/application.yml`，或用环境变量覆盖：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_room?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}          # 无默认值，必须注入
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: ${JWT_SECRET}               # 无默认值，长度需 ≥ 32 字符
```

环境变量方式（Windows）：

```powershell
setx DB_USERNAME root
setx DB_PASSWORD 你的密码
setx JWT_SECRET 至少32字符的随机密钥
```

> `DB_PASSWORD` 与 `JWT_SECRET` 为必填项，缺失时应用启动即失败（`JwtUtil` 会校验密钥长度）。
> 环境变量只对新启动的进程生效，设置后需重新打开终端或完全重启 IDEA。

### 5.4 构建前端（可选）

前端产物已提交在 `src/main/resources/static`，修改前端源码后需重新构建：

```bash
cd frontend
npm install
npm run build     # 输出到 ../src/main/resources/static，并清空旧产物
```

开发模式（热更新 + `/api` 代理到 8080）：

```bash
npm run dev
```

### 5.5 启动

方式一（Maven）：

```bash
mvn spring-boot:run
```

方式二（打包运行）：

```bash
mvn clean package -DskipTests
java -jar target/StudyRoomBookingCheckin-1.0.0.jar
```

### 5.6 访问

| 入口     | 地址                                 |
| -------- | ------------------------------------ |
| 登录页   | http://localhost:8080/api/login |
| 学生端   | 登录后进入 `/api/home`，通过左侧导航切换；也可直接访问 `/api/reserve`、`/api/stats` 等地址 |
| 管理后台 | 管理员登录后进入 `/api/admin`，左侧出现「管理后台」 |

> 应用配置了 `server.servlet.context-path: /api`，所有页面与接口均在 `/api` 前缀下；前端使用 History 路由，刷新或直接访问深层地址由后端转发到 `index.html`，不会 404。

### 5.7 演示账号

| 角色   | 用户名     | 密码         |
| ------ | ---------- | ------------ |
| 管理员 | `admin`  | `admin123` |

学生账号在登录页自行注册（注册默认角色为学生，注册成功即自动登录）。

## 6. 页面导航

| 路由           | 页面       | 说明                                                                     |
| -------------- | ---------- | ------------------------------------------------------------------------ |
| `/home`     | 学习概览   | 今日/本月/连续打卡/进行中预约 KPI 卡、快捷入口、最近预约列表             |
| `/reserve`  | 自习室预约 | 楼栋 → 楼层 → 自习室卡片 → 座位图，点击空闲座位预约；管理员可开启管理模式增删改 |
| `/bookings` | 我的预约   | 状态筛选与分页、8 位签到码签到、离座释放、取消预约                       |
| `/study`    | 学习计时   | 进行中计时的秒级刷新、结束学习、打卡记录分页                             |
| `/stats`    | 学习统计   | 5 项时长 KPI、近 30 天折线图、52 周日历热力图（绝对刻度）、星期分布      |
| `/goals`    | 学习目标   | 目标进度条、新建/编辑/删除目标                                           |
| `/ranking`  | 排行榜     | 日/周/月/总四维度榜单、我的排名、好友申请与好友今日时长                  |
| `/admin`    | 管理后台   | 运营概览、预约记录、违规记录、用户统计（仅管理员）                       |
| `/users`    | 用户管理   | 用户筛选、新增/编辑/禁用/删除、操作日志（仅管理员）                      |

## 7. 核心业务规则

### 7.1 状态定义

| 对象 | 状态值                                               |
| ---- | ---------------------------------------------------- |
| 座位 | 0 空闲 / 1 使用中 / 2 已预约 / 3 维护中              |
| 预约 | 0 待签到 / 1 学习中 / 2 已完成 / 3 已取消 / 4 已过期 |
| 打卡 | 0 进行中 / 1 已结束 / 2 暂离                         |
| 目标 | 0 进行中 / 1 已达成                                  |
| 违规 | 0 未处理 / 1 已处理                                  |
| 用户 | 角色 0 学生 / 1 管理员；状态 0 禁用 / 1 正常         |

### 7.2 关键流程

1. **预约**：`@Transactional` 内用 `SELECT ... FOR UPDATE` 锁座位行 → 校验座位存在/非维护/时段无冲突 → 写入预约（生成 8 位数字签到码，默认时长 120 分钟）→ 座位原子更新为「已预约」→ 同步 Redis 座位缓存。数据库层再用唯一索引 `uk_seat_active_start(seat_id, start_time, active_slot)` 兜底，杜绝并发重复预订。
2. **签到**：仅「待签到」且未超过 `startTime + DEFAULT_TIMEOUT_MINUTES(30 分钟)` 可签到；支持按预约 ID 或 8 位签到码签到 → 预约置「学习中」、座位置「使用中」→ 自动创建进行中打卡。
3. **离座**：预约置「已完成」并记录实际时长 → 座位恢复「空闲」→ 结束对应打卡 → 清理排行榜缓存。
4. **自动释放（定时任务 + Redis 分布式锁，避免重复执行）**：
   - `autoReleaseExpired`（每 5 分钟）：待签到且 `startTime + 30 分钟` 已过 → 置「已过期」、释放座位、写入爽约违规（罚 5）。
   - `autoReleaseTimeout`（每 10 分钟）：学习中且已过 `endTime` → 置「已完成」、释放座位、结束打卡、写入超时占座违规（罚 3）。
5. **统计口径**：学习时长来源于打卡记录（`t_study_checkin`），连续打卡要求单日累计 ≥ 10 分钟；统计接口一次按天聚合，避免热力图 52 周循环查询。

### 7.3 Redis 缓存设计

| Key 模式                     | 类型 | 内容                                   | 过期时间             |
| ---------------------------- | ---- | -------------------------------------- | -------------------- |
| `seat:room:{roomId}`         | Hash | field=seatId，value=SeatVO JSON        | 30 分钟 + 随机 0-5 分钟 |
| `seat:room:empty:{roomId}`   | String | 空结果标记，防穿透                     | 60 秒                |
| `lock:seat:room:{roomId}`    | String | 回源互斥锁（`setIfAbsent`）             | 5 秒                 |
| `ranking:daily:{date}`       | String | 当日榜单 JSON                          | 5 分钟               |
| `ranking:weekly:{date}`      | String | 本周榜单 JSON                          | 10 分钟              |
| `ranking:monthly:{month}`    | String | 本月榜单 JSON                          | 30 分钟              |
| `ranking:all`                | String | 累计榜单 JSON                          | 1 小时               |
| `login:fail:user:{username}` | String | 登录失败计数                           | 10 分钟              |
| `login:fail:ip:{ip}`         | String | 同 IP 登录失败计数                     | 10 分钟              |

- 座位状态变更（预约/签到/离座/维护）时先更新数据库，再 `HSET` 原子更新缓存单字段；座位增删或整批变更时删除该房间缓存。
- 缓存读失败或 Redis 不可用时自动降级为直接查库，不影响主流程。
- 排行榜在离座、结束打卡后主动失效，保证数据与库一致。

## 8. 权限与安全

- **匿名可读**：仅放行 GET 的 `/buildings/**`、`/rooms/**`、`/seats/**`（浏览楼栋与座位），其余接口必须携带 Token。
- **管理端**：`AdminController` 类上标注 `@RequireAdmin`，楼栋/自习室/座位的 POST/PUT/DELETE 方法同样标注；`JwtInterceptor` 统一校验 `role = 1`，学生 Token 访问返回 403。
- **登录限流**：同一账号 10 分钟内失败 5 次、同一 IP 失败 20 次即返回 429。
- **密钥管理**：`application.yml` 不含明文口令，`DB_PASSWORD`、`JWT_SECRET` 必须由环境变量注入；`JWT_SECRET` 长度不足 32 字符时启动失败。

## 9. 接口概览

统一响应结构：`{ "code": 200, "message": "success", "data": ... }`；除匿名可读接口、登录与注册外，请求头需携带 `Authorization: Bearer <token>`。

| 分组   | 主要接口                                                                                                                                                                                    |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 认证   | `POST /auth/register`、`POST /auth/login`                                                                                                                                               |
| 用户   | `GET /user/profile`、`PUT /user/profile`、`PUT /user/password`、`POST /user/avatar`                                                                                                  |
| 楼栋   | `GET /buildings`、`GET /buildings/{id}`、`POST/PUT/DELETE /buildings`                                                                                                                 |
| 自习室 | `GET /rooms?buildingId=&floor=`、`GET /rooms/page`、`GET /rooms/{id}`、`POST/PUT/DELETE /rooms/{id}`                                                                                  |
| 座位   | `GET /seats?roomId=`、`GET /seats/{id}`、`POST /seats`、`POST /seats/batch`、`PUT /seats/{id}`、`PUT /seats/{id}/status`、`DELETE /seats/{id}`、`GET /seats/qrcode/{qrcode}` |
| 预约   | `POST /bookings`、`GET /bookings/mine`、`GET /bookings/{id}`、`POST /bookings/{id}/checkin`、`POST /bookings/checkin/code?code=`、`POST /bookings/{id}/checkout`、`POST /bookings/{id}/cancel` |
| 打卡   | `POST /checkins/start`、`POST /checkins/{id}/end`、`GET /checkins/mine`、`GET /checkins/statistics`                                                                                 |
| 目标   | `GET /goals/mine`、`GET /goals/{id}`、`POST /goals`、`PUT /goals/{id}`、`DELETE /goals/{id}`                                                                                         |
| 好友   | `GET /friends`、`GET /friends/requests`、`POST /friends/request/{friendId}`、`POST /friends/accept/{friendId}`、`POST /friends/reject/{friendId}`、`DELETE /friends/{friendId}` |
| 排行   | `GET /ranking/daily\|weekly\|monthly\|all`、`GET /ranking/my/daily\|weekly\|monthly`                                                                                                         |
| 管理   | `GET /admin/dashboard`、`GET /admin/bookings`、`GET /admin/utilization`、`GET /admin/user-stats`、`GET /admin/users`、`POST /admin/users`、`PUT /admin/users/{id}`、`PUT /admin/users/{id}/status`、`DELETE /admin/users/{id}`、`GET /admin/users/logs`、`GET /admin/violations`、`PUT /admin/violations/{id}/handle` |

## 10. 测试

```bash
# 仅运行单元测试
mvn test

# 打包（跳过测试）
mvn -DskipTests package
```

| 测试类                        | 覆盖内容                                                                 |
| ----------------------------- | ------------------------------------------------------------------------ |
| `BookingServiceImplTest`    | 预约成功/时段冲突/非法时间、签到超时规则、非本人签到、离座释放、取消联动结束打卡、签到码格式校验、两类自动释放与违规写入、20 线程并发抢座仅 1 单成功 |
| `StudyCheckinStatisticsTest` | 单次聚合口径、今日/累计时长、当前连续与最长连续打卡、短时长不计入、52 周热力图补零 |
| `RankingServiceImplTest`    | 榜单排序与累计、topN 截断、未上榜排名、用户信息批量加载（无 N+1）         |

> 单元测试使用 Mockito，不依赖 MySQL / Redis，可离线执行。

## 11. 常见问题

### 11.1 端口 8080 被占用

报错：`Web server failed to start. Port 8080 was already in use.`

**排查流程**：查 PID → 确认进程身份 → 终止或改端口 → 验证访问。

Windows（PowerShell）：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
(Get-CimInstance Win32_Process -Filter "ProcessId=<PID>").CommandLine
Stop-Process -Id <PID> -Force
```

macOS：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
ps -p <PID> -o pid,comm,args
kill -9 <PID>
```

Linux：

```bash
ss -lntp | grep ':8080'
ps -p <PID> -o pid,comm,args
kill -9 <PID>
# 或直接按端口终止：fuser -k 8080/tcp
```

> 终止前务必确认 PID 身份，避免误杀 MySQL/Redis 等其他服务。

**修改监听端口**（优先级：启动参数 > 环境变量 > 配置文件）：

```yaml
# application.yml
server:
  port: 8081
  servlet:
    context-path: /api
```

```bash
java -jar target/StudyRoomBookingCheckin-1.0.0.jar --server.port=8081
```

```powershell
setx SERVER_PORT 8081   # Windows；macOS/Linux 使用 export SERVER_PORT=8081
```

改端口后访问地址变为 `http://localhost:8081/api/login`。

### 11.2 登录返回 500（数据库连接失败）

日志出现 `Access denied for user 'root'@'localhost' (using password: NO)`，说明应用未读到数据库密码：

- 确认 `DB_PASSWORD` 已设置，且应用是在设置之后启动的；
- IDEA 中运行需完全退出 IDE 后重开，或在 Run Configuration 的 Environment variables 中补充；
- 也可直接在 `application.yml` 中填写密码（本地调试）。

### 11.3 访问根路径 404

应用上下文为 `/api`，根路径已重定向到登录页，请访问：

```
http://localhost:8080/api/login
```

### 11.4 页面接口 401

Token 过期或未登录，重新登录即可；前端在收到 401 时会自动跳转登录页。

### 11.5 启动报错 “JWT_SECRET 环境变量缺失或长度不足 32 字符”

`JwtUtil` 在启动时强校验密钥，请设置至少 32 字符的 `JWT_SECRET` 后重新启动应用。

## 12. 说明

- 本项目为教学/课程设计用途，`application.yml` 中已不含任何明文密钥，请通过 `DB_PASSWORD` / `JWT_SECRET` 环境变量注入。
- 前端为 Vue 3 工程，源码在 `frontend/`；修改后执行 `npm run build` 重新生成 `src/main/resources/static` 产物。
