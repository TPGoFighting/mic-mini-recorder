# Mic Mini Recorder

面向真我 GT8 / ColorOS 的第一版验证工程：使用 DJI Mic Mini 原装发射器的蓝牙输入，手动开始后以麦克风前台服务持续录音。

## 当前行为

- 仅接受 `TYPE_BLUETOOTH_SCO` 或 `TYPE_BLE_HEADSET` 路由。
- 如果 Android 实际路由回手机内置麦克风，应用会拒绝写入录音文件。
- 每 30 分钟切割一个 MP3 文件。
- 文件写入应用专属音乐目录：`Android/data/com.example.micminirecorder/files/Music/MicMini/`。
- 蓝牙断开时保留前台服务并轮询重连；重新连接后生成新的分段文件。
- 录音时常驻通知，通知中提供停止按钮。

## 在真机上验证

1. 用 Android Studio 打开本目录，连接真我 GT8。
2. 先在系统蓝牙设置中配对并连接 DJI Mic Mini 发射器。
3. 安装并打开应用，授予麦克风、蓝牙连接和通知权限。
4. 点击“开始录音”，确认通知显示“录音中：DJI Mic Mini”或类似外部设备名称。
5. 说话 10 秒后停止，检查 `MicMini` 目录中的 MP3 是否可播放。
6. 锁屏等待 1 分钟，再回到应用确认录音仍在进行。

## 当前不能保证的部分

- ColorOS 可能不把直连发射器暴露为普通应用的录音输入；应用无法用公开 Android API 绕过系统音频策略。
- 直连蓝牙的采样率、编码质量和通话抢占行为由手机蓝牙音频栈决定。
- 当前工程只验证 Android 录音服务，不包含自动开机录音、云同步、转写和文件导出。
- MP3 依赖 `TAndroidLame`（GPL-3.0）；公开发布前需要评估许可证和该依赖的 16 KB 页大小兼容性。
