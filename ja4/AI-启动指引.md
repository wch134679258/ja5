# AI 快速启动指引（ja4 项目）

> **本文件写给 AI 助手看**（人类也可参考）。这台电脑每次重置后 `node_modules`、`target` 文件夹会被删除，请依赖本文件避免重复探索，直接执行标准流程。
> 最后更新：2026-10-16（新增：文章详情/收藏/搜索建议 + 《凡人修仙传》小说内容 + 前端多页面；详见「八、2026-10-16 功能升级」）

**测试账号**：昵称 `verify用户`，密码 `abc12345`（2026-10-16 重新入库，可直接登录验证；注意数据库当天被重置过，`段先生` 账号的密码未知）
**版本管理**：项目根目录已是 git 仓库（分支 main）。改动代码后请提交；开机后如怀疑文件被动过，可用 `git status` / `git diff` 对比。

## 一、项目结构（都在 C:\Users\Administrator\Desktop\ja3-main\）

```
Desktop\ja4\
├─ 启动项目.bat / 启动前端.bat / 启动后端.bat / 停止项目.bat   ← GBK 编码一键脚本
├─ AI-启动指引.md    ← 本文件
├─ _archive\        冗余副本归档区（2026-10-19 从 ja3-main 移入，可随时删）
└─ ja3-main\
   ├─ aurora-vue-2\     前端：Vue3 + Vite，端口 5173（Aurora 登录/注册页）
   ├─ springBootTest\   后端：Spring Boot 3.4.5 + MyBatis-Plus，端口 8081（自带 mvnw）
   └─ *.rar             原始压缩包，勿删
```

## 二、环境（已确认可用，无需重新检查）

- **Node**：系统 PATH 有 node（D:\Program Files\nodejs，v20.11）；AI 沙盒内优先用托管版：
  `C:\Users\Administrator\.workbuddy\binaries\node\versions\22.22.2-2\node.exe`
- **Java**：Zulu 21（PATH 有 java），满足项目要求（JDK 17+）
- **Maven**：无全局 mvn，用 `springBootTest\mvnw.cmd`（阿里云镜像已配置，~/.m2 已缓存约 800MB）
- **MySQL 8.0**：已安装，服务自启（3306 端口），账号 root / 123456
- **数据库**：`springboot_test` 库已建好（user 表：id/name/password，name 唯一索引，**无 email 列**）。若库被重置，重跑 `springBootTest\init-db.sql`（mysql 客户端在 C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe）
- **npm 缓存**：C:\Users\Administrator\AppData\Local\npm-cache（若缓存保留，安装很快）

## 三、开机后的标准流程（AI 直接照做，不要重新分析）

1. **推荐：一键启动**（AI 请用 PowerShell 工具后台调用，用户可直接双击）：
   ```
   & "C:\Users\Administrator\Desktop\ja3-main\启动后端.bat"    ← 后台运行
   & "C:\Users\Administrator\Desktop\ja3-main\启动前端.bat"    ← 后台运行
   ```
   脚本会自动 npm install / mvnw 编译，**不依赖已删除的 node_modules、target**
2. **验证就绪**（轮询即可）：
   - `curl http://localhost:5173` → 200
   - `curl http://localhost:8081/api/auth/captcha` → JSON
3. **打开页面**：http://localhost:5173 （昵称 + 密码注册/登录，带图形验证码）
4. **停止项目**：运行 `停止项目.bat`（kill 5173、8081 端口进程）

**实测耗时参考**：前端冷启动约 30 秒（依赖重装），热启动约 5 秒；后端冷编译约 60-70 秒，热启动约 20 秒。

## 四、关键事实（避免不必要的思考）

- **登录方式**：昵称 + 密码。**邮箱功能已全部移除**（表单、后端 DTO/Service/实体、数据库列、verify_code 表均已删）
- 接口签名：
  - `POST /api/auth/register { name, password, captchaId, captcha }` → `{ token, userId }`
  - `POST /api/auth/login { name, password, remember }` → `{ token, userId }`
  - `GET /api/auth/me`（Bearer token）→ `{ id, name }`
- `vite.config.js` 已配置 `/api → http://localhost:8081` 代理，前端**无需 .env**；后端 CorsConfig 也放行 5173（双保险）
- JWT 密钥与数据库密码均为演示值（root/123456），生产需替换
- 图形验证码存后端内存 Map，重启后端即失效（正常现象）；4 位字母数字不区分大小写
- 每次开机 node_modules、target 必被清空 → **这是设计如此，不是故障**，脚本已适配

## 五、AI 沙盒环境的坑（脚本已内置规避，若手动执行需注意）

- 沙盒会注入 `SERVER__PORT`（双下划线）环境变量，Spring 宽松绑定会把它当成 `server.port`，导致后端绑到错误端口 → 手动跑后端前先清掉
- 沙盒注入的 HTTP_PROXY/HTTPS_PROXY 指向会失效的本地代理 → 手动 npm 前先清掉
- bash 里 `cmd //c 中文.bat` 传参会乱码 → 用 PowerShell 工具 `& "路径.bat"` 代替
- PowerShell 工具前台运行拿不到 stdout → 用 run_in_background 后台运行再用 TaskOutput 读输出
- cmd 黑名单：sc.exe、wmic.exe、reg.exe、从 bash 调 powershell 均被禁

## 六、首页搜索（2026-10-14 完成）

**数据模型（两级分类）**：
- `category` 一级分类（前端/后端/设计/数据库/工程）
- `tag` 二级标签，`category_id` 归属一级分类
- `article_tag` 文章↔标签多对多；`article.category_id` 冗余一级分类便于过滤
- **建表脚本**：`springBootTest/init-db.sql`（全量，幂等）；已有库增量升级用 `springBootTest/migrate-category.sql`（幂等，自动回填关联）

**接口**（均公开无需登录）：
- `GET /api/search?q=&categoryId=&tagId=&sort=hot|new&page=&size=` —— 关键词命中 标题/摘要/正文/作者/标签串，有关键词时按「标题命中>标签命中>热度」相关度排序；分类与标签可叠加（分级分类交叉检索）
- `GET /api/search/categories` —— 分类树（一级分类+其下标签+文章数）
- `GET /api/search/hot-tags` —— 热门标签（按关联文章数倒序）

**前端**：`HomePage.vue` 已接入（分类导航、二级标签联动、热度/最新排序、筛选面包屑、分页）；API 封装在 `src/api/search.js`。

**AI 沙盒跑后端的坑（新增）**：沙盒的 `SERVER__PORT` 会让后端绑错端口（实测跑到 8080）。正确姿势：
```bash
env -u SERVER_PORT -u SERVER__PORT -u SERVER_ADDRESS -u SERVER__ADDRESS -u SERVER_HOST -u SERVER__HOST -u MAVEN_OPTS \
  HTTP_PROXY= HTTPS_PROXY= http_proxy= https_proxy= \
  java -classpath "$MVN_HOME/boot/plexus-classworlds-2.8.0.jar" -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
  -Dmaven.home="$MVN_HOME" -Dmaven.multiModuleProjectDirectory="$PWD" \
  org.codehaus.plexus.classworlds.launcher.Launcher -o -DskipTests spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8081"
```
（`mvn`/`mvnw` shell 脚本在沙盒内因 classworlds 加载问题不可用，直接用 java 调 Launcher 可行）

## 七、已知缺失 / 可改进（截至 2026-10-14）

- `后端\`、`后端文件\` 冗余副本文件夹，建议归档避免混淆
- 后端无单元测试
- 密码找回功能随邮箱一并移除（如需找回，可后续加管理员重置或手机号方案）
- `user` 表中 `段先生` 的旧密码 hash 与任何已知口令不匹配（历史遗留数据），已新增 `verify用户`/`abc12345` 作为可用测试账号

## 八、2026-10-16 功能升级（文章详情 / 收藏 / 搜索建议 / 小说内容）

### 8.1 数据库变更（幂等迁移脚本，按序执行）

```
springBootTest/migrate-novel.sql     新增「小说」分类 + 7 个标签 + 《凡人修仙传》5 篇文章（含正文）
springBootTest/migrate-favorite.sql  新增 favorite 收藏表（user_id + article_id 联合唯一）
```

> 2026-10-16 当天库被重置过（只剩 `段先生`），上述两个脚本已执行；测试账号 `verify用户`/`abc12345`
> 已用 BCrypt 重新入库（id=2）。若库再被重置，重跑 init-db.sql + 这两个迁移脚本即可。

### 8.2 新增后端接口

```
GET  /api/article/{id}              文章详情（浏览量 +1；带 Bearer token 时返回 favorited）
GET  /api/article/{id}/related      相关文章（共享标签优先，同分类兜底，最多 4 条）
GET  /api/search/suggest?q=         搜索建议（标题 5 条 + 标签 5 条）
POST /api/favorites/{articleId}     收藏（需登录）   -> { message }
DELETE /api/favorites/{articleId}   取消收藏（需登录）
GET  /api/favorites?page=&size=     我的收藏列表（分页，需登录）
GET  /api/favorites/ids             已收藏的文章 id 列表（需登录，列表页标记收藏态）
```

关键类：`ArticleService` / `FavoriteService` / `ArticleController` / `FavoriteController` /
`FavoriteMapper(.xml)`；`ArticleItem.from()` 为搜索/收藏/相关文章共用的实体转 DTO 工厂；
`JwtUtil.tryGetUserId()` 用于「可选登录」接口（详情页：未登录也能看，登录了多返回收藏态）。

### 8.3 前端结构（视图状态机，无 vue-router）

App.vue 管理 view：`auth / home / article / favorites / discover / about`，body 加 `view-home` 类开启滚动。

```
src/api/http.js        统一请求层（token 注入 / 401 清token / {message} 错误提取），auth|search|article|favorites 四个 api 共用
src/api/article.js     详情 / 相关文章
src/api/favorites.js   收藏列表 / ids / 收藏 / 取消
src/components/SiteNav.vue       全站导航（首页/发现/收藏/关于全部可用）
src/components/ArticleCard.vue   文章卡片（整卡可点击、分类/标签可点、星星收藏）
src/components/ArticleDetail.vue 阅读页（正文分段、收藏、相关文章、返回）
src/components/FavoritesPage.vue 我的收藏（未登录引导登录、可直接取消收藏）
src/components/DiscoverPage.vue  发现页（最新/热门/分类入口）
src/components/AboutPage.vue     关于页（项目介绍 + 技术栈 + 测试账号）
src/components/SearchBar.vue     升级：建议下拉（防抖 250ms）+ 搜索历史（localStorage）+ 键盘导航 + 关键词高亮
src/components/HomePage.vue      升级：卡片进详情、卡片收藏、数字分页、支持跨页面初始筛选 initialFilter
```

修复：首页结果卡片 `.rcard` 原本只有媒体查询里的 padding、无基础样式（裸奔），现已由 ArticleCard 提供完整样式。

### 8.4 验证记录（2026-10-16 全部通过）

- 搜索「凡人修仙传」→ 5 条结果，标题命中优先（id 16/18/19/20 + 韩立人物志）
- 搜索建议 `q=凡人` → 3 条标题 + 2 条标签
- 详情/相关文章/收藏增删查/重复收藏 400/未登录 401/收藏不存在文章 400 均符合预期
- 前端 `vite build` 通过，8 个新组件经 dev server 逐一编译 200

### 8.5 环境备注

- `mvnw package` 离线会因缺 surefire 插件依赖失败；重启后端用文档「六」节末尾的 java Launcher 方式跑 `spring-boot:run`（compile 离线可用）
- Windows curl 传中文 JSON body：先 `printf` 写入**Windows 绝对路径**文件，再 `--data-binary @绝对路径`（MSYS 的 /tmp 路径 curl.exe 不认）

---

## 九、2026-10-19 后端加固（稳定性 / 安全 / 日志）

### 9.1 目录结构变化

- `ja3-main/后端/`、`ja3-main/后端文件/` → 归档到 **`_archive/`**（`git mv`，历史可追溯）
- `springBootTest/` 内新增：`config/AuthSupport.java`、`resources/logback-spring.xml`
- `springBootTest/` 内重命名：`application-prob.properties` → **`application-prod.properties`**（原名拼写错误）
- `UserController.java`（遗留 demo，密码硬编码 123、`/user/test` 打印配置）**经用户确认保留原样**，未删除，仅在此记录风险

### 9.2 配置与日志

| 项 | 改动 |
|---|---|
| `application.properties` | 中文注释原先全是 `?`（文件编码已损坏）→ 重写为 UTF-8；修正 `com.fengling`→`com.fenglin` 拼写（原配置从未生效）；新增优雅停机、错误不回传堆栈 |
| SQL 日志 | `StdOutImpl`（直接刷 stdout、绕过日志级别、无法归档）→ `Slf4jImpl`，由 `logging.level.com.fenglin.springboottest.mapper=debug` 控制 |
| 日志文件 | `log/xx.log` 单文件不轮转 → `log/springboot-test.log`，**10MB/天滚动 + gz + 保留 14 天 + 总量 200MB 封顶** |
| 多环境 | 新增 `application-prod.properties`（HikariCP 调参、敏感项走环境变量、SQL 日志关闭） |
| logback 坑 | `logback-spring.xml` 里 **不要 include** `org/springframework/boot/logging/logback/console.xml`——`spring-boot:run` 下解析不到，控制台日志会整段消失；改为自己定义 CONSOLE appender |

### 9.3 安全加固

- **JwtUtil**：`secret.getBytes()` 平台默认编码 → `StandardCharsets.UTF_8`（消除跨平台签名不互认的隐患）；补 HS384 密钥 ≥48 字节前置校验；`JwtParser` 缓存复用（线程安全）
- **AuthService**：`BCryptPasswordEncoder` 提升为单例 bean；注册补长度上限 + `DuplicateKeyException` 竞态转换（不再 500）；**登录失败 5 次锁定 60 秒**（内存计数，重启清零）
- **CaptchaService**：`new Random()` → `ThreadLocalRandom`；内存 Map 加上限 2000，防刷接口撑爆内存
- **AuthSupport**（新）：三处 Controller 各自复制的 Bearer 解析逻辑收敛为 `requireUserId` / `optionalUserId`

### 9.4 接口响应约定（新增）

`GlobalExceptionHandler` 现在兜底所有异常，**永远返回 `{message}` JSON，不再回 HTML 错误页**：

| 场景 | 状态码 | message |
|---|---|---|
| 业务异常 | 400 | 原文案 |
| 未登录 / 令牌无效 | 401 | 请先登录 / 登录已过期，请重新登录 |
| JSON 格式错误 | 400 | 请求体格式不正确，请检查后重试 |
| `@Valid` 校验失败 | 400 | `name 请输入昵称`（字段名 + 原因） |
| 参数类型错误 | 400 | 参数格式不正确：categoryId |
| 方法不支持 | 405 | 不支持的请求方法：POST |
| 路径不存在 | 404 | 接口不存在 |
| 唯一索引冲突 | 409 | 数据冲突，记录可能已存在 |
| 未预期异常 | 500 | 服务器内部错误，请稍后再试（堆栈只进日志） |

### 9.5 验证记录（2026-10-19 全部通过）

- `mvnw compile`：**BUILD SUCCESS**，34 个源文件零警告
- 冒烟：captcha / search / categories / hot-tags / suggest / article 详情 / related / favorites 增删查 / 重复收藏 400 / 未登录 401 / 假 token 401
- 新异常分支逐一验证：坏 JSON 400、空昵称 400、非数字 categoryId 400、POST /api/search 405、不存在路径 404
- 登录限流：连续 5 次失败后第 6 次返回「尝试次数过多，请 60 秒后再试」

### 9.6 本次踩坑记录（供后续参考）

- **数据库被重置过**：本机 MySQL 里 `springboot_test` 库曾不存在（`Unknown database`）→ 重跑 `init-db.sql` + `migrate-novel.sql` + `migrate-favorite.sql`
- `verify用户` 的旧 BCrypt hash 与 `abc12345` 不匹配 → 已用后端同款 `BCryptPasswordEncoder` 重新生成 hash 更新，测试账号恢复可用
- bash 直接 `cmd //c` 会被 MSYS 路径转换坑到；`./mvnw.cmd` 直接可执行
- 用 curl 发中文 JSON body 必须 `--data-binary @Windows绝对路径`（见 8.5）

## 十、2026-10-19 星空贪吃蛇小游戏（前后端联动）

### 10.1 功能概览

- 前端新页面 `aurora-vue-2/src/components/SpaceSnake.vue`（Canvas 实现，无新增依赖），入口：登录后导航栏「游戏」
- 玩法：光蛇吞噬小星球变长；普通星球 +10 分，金色星球 +50 分（约 12% 概率）；每吃一颗航速加快，撞边界 / 撞自己结束
- 交互：方向键 / WASD / 触屏滑动 / 虚拟方向键；空格暂停；切窗口自动暂停；**全屏按钮**（Fullscreen API，Esc 退出）
- 视觉：星空视差背景 + 极光星云（呼应 AuroraBackground）、发光蛇身（青→紫渐变）、带环行星、粒子与浮字特效、死亡震屏
- 本地最高分存 `localStorage`（`aurora_snake_best`）；联网成绩走 `/api/game` 接口

### 10.2 数据库变更

执行 `springBootTest/migrate-game-score.sql`（幂等）：

- 新表 `game_score`（id, user_id, nickname 快照, score, length, duration, created_at）
- 排名口径：分数 DESC → duration ASC → id ASC；`idx_game_score_rank` 联合索引

### 10.3 新增后端接口

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| POST | `/api/game/scores` | 是 | 提交成绩 `{score, length, duration}` → `{message, score, best, isNewBest, rank, playCount, leaderboard}` |
| GET | `/api/game/leaderboard?limit=10` | 否 | 排行榜 Top N（1~50）→ `{list: [ScoreItem]}` |
| GET | `/api/game/my-best` | 是 | 我的最好成绩 → `{best, rank, playCount}` |

- 新增文件：`entity/GameScore.java`、`mapper/GameScoreMapper.java`、`service/GameScoreService.java`、`controller/GameController.java`、`dto/ScoreRequest.java`、`dto/ScoreItem.java`（record）
- 昵称由服务端按 userId 反查（`userMapper.selectById`），客户端伪造不了；分数/长度/时长均有 `@Valid` 上限校验

### 10.4 前端文件

- 新增：`src/components/SpaceSnake.vue`、`src/api/game.js`
- 改动：`App.vue`（视图状态机新增 `game` 分支）、`SiteNav.vue`（菜单新增「游戏」）、`AboutPage.vue`（特性卡片 + 简介）
- `style.css`：**修复「不能滑动」**——`body` 原写死 `overflow:hidden`，窗口矮于登录卡时内容被裁剪且无法滚动；改为 `overflow-y:auto` + `place-items:safe center`（内容超高时从顶部排列，避免居中裁剪）

### 10.5 验证记录（2026-10-19 全部通过）

- `mvnw compile` BUILD SUCCESS；`npm run build` 44 modules 零错误
- 接口冒烟：未登录提交 401、`limit=0` 回落默认 10、越界 score 400（`@Valid`）、正常提交返回 rank + 最新榜单、`my-best` 无成绩时 best=0
- 前端：游戏页渲染 / 开始 / 暂停 / 重开 / 结算遮罩 / 提交按钮状态机均按预期（构建 + 逻辑走查）


