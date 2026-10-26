# 大肥鱼桌宠 · 点击计数版

基于 Tony-Chen-Stark/dsh-pet-android，提交 eca369a。

- 每次有效单击累计功德 +1，桌宠上方显示总数和短暂的「功德 +1」。
- 支持连续快点；拖动、长按、取消触摸不计数。
- 多只桌宠共用总数，保存在应用本地，收起、重建悬浮窗或正常重启后保留。
- 设置页实时显示完整总数，可点击「清零」并确认重置。
- 保留原有点击动画和拍一拍音效，新增计数本身不需要网络或额外权限。

## 安装

APK 为 480p / 24fps，Android 9 或更高版本。
打开应用，授予悬浮窗和通知权限，点击「放出来」。
本次 APK 使用新生成的测试签名，与原 Release 签名不同，不能直接覆盖原版安装。
若选择卸载原版后安装，请先自行备份台词等需要保留的数据；卸载会删除应用数据。

## 源码构建

此源码包已附带原项目 v1.8.1-480p-24fps Release 的 106 段动画、音效和立绘，
配置与素材已核对匹配，无需再执行 README 的素材还原步骤。
素材许可与署名见 NOTICE.md 和 README.md，禁止商用要求继续适用。

准备 JDK 17 和 Android SDK 34，在 local.properties 写入本机的 sdk.dir，然后运行：

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

验证结果：APK 编译与签名验证通过；8 项 Robolectric 单元测试通过；Android Lint 无错误。
测试覆盖快速连点、拖动、长按、取消触摸、多宠共享、计数存储、新视图恢复显示、清零确认和大整数边界。
尚未做真机验证。

源码仓库：https://github.com/Steam10086-1/dsh-pet-counter
发布页：https://github.com/Steam10086-1/dsh-pet-counter/releases/tag/v1.8.1-counter.1

计数版 Release 标签为 v1.8.1-counter.1；APK 内的应用版本号保留上游 1.8.1。
