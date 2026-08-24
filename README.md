# DJI Mic Mini 专用原生 Android 高清录音应用 (v1.0.0.0)

专为 **DJI Mic Mini / Mic 2** 与全系蓝牙/USB外置麦克风打造的广播级 Android 录音工具。基于 **Kotlin + Jetpack Compose + 现代响应式架构** 构建。

---

## 🌟 核心特性与黑科技

### 1. 🎯 硬件输入通道锁死（双模自适应）
- **蓝牙 SCO 模式**：深度绑定 `MediaRecorder.AudioSource.VOICE_COMMUNICATION`，确保 ColorOS / Realme / 各大厂商 Android 系统 100% 走蓝牙外设，杜绝回退手机内置麦克风。
- **Type-C 2.4G 接收器模式**：插入 Type-C 接收器（RX）时，系统自动无缝热切换至 `TYPE_USB_DEVICE`，跑满 **48,000 Hz / 24-bit 无损录音棚母带音质**。
- **断连自停保护**：实时监听 `AudioDeviceCallback`，麦克风断开瞬间立即安全封存文件。

### 2. 🎮 DJI Mic 机身物理按键控制
- **单击 Link 配对键**：无需掏出手机，单击麦克风发射机侧边配对键即可随时 **开启录音 / 停止录音**。
- **全局 Session 拦截**：集成 `MediaSession` 与 `dispatchKeyEvent`，灭屏与后台运行时均可精准响应。

### 3. 🫧 自研流体云悬浮胶囊 (System Floating Capsule)
- 适配 ColorOS 15/16 流体云与前台服务保活机制。
- 极简磨砂黑药丸视觉、呼吸红点、实时走秒、全屏拖拽。
- 支持 `FLAG_SHOW_WHEN_LOCKED` 锁屏置顶显示，点击平滑展开 [🛑 停止录音] 与 [↗️ 打开 App]。

### 4. 🎛️ 工业级音频引擎与多格式转码
- **多格式无损导出**：支持 48kHz WAV / MP3 (纯算法 LAME 编码，支持 320kbps) / AAC。
- **降噪架构**：
  - `原声直通 (OFF)`：100% 原始信号，配合机身黄灯硬件消噪效果最佳。
  - `演播室专业降噪`：Audacity / WebRTC APM 工业标准平滑降噪 + Android 硬件级 `NoiseSuppressor`。
- **SAF 自定义路径**：支持保存至系统录音机、公共下载、音乐目录或系统任意自选文件夹。

---

## 🛠️ 构建与安装

```bash
cd dji-recorder
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。
