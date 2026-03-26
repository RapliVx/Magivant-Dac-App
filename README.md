# 🎧 Magivant-DAC - Leteciel USB DAC Controller

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-MD3E-4285F4.svg?logo=android)
![Minified](https://img.shields.io/badge/APK-Minified%20%26%20Optimized-success.svg)
![CI/CD](https://img.shields.io/badge/Build-GitHub%20Actions-2088FF.svg?logo=github-actions)

**Magivant-DAC** is a modern, open-source alternative to the stock Leteciel controller for **Leteciel Magivant USB DAC** devices. Designed as a lightweight, high-performance replacement for the official app, it brings advanced hardware settings to a clean *Material Design 3 Expressive (MD3E)* interface with full **Monet** (Material You) support.

---

## How It Works

Unlike standard audio players that simply send PCM audio data to the Android OS, **Magivant-DAC** communicates directly with the DAC's microcontroller to manipulate hardware-level settings.

Here is the breakdown of the application flow:

1. **Device Detection & Permission:** The app uses Android's `UsbManager` to listen for specific **Vendor IDs (VID)** and **Product IDs (PID)** associated with Leteciel Magivant devices (e.g., `VID 0x2FC6`). Once attached, it requests explicit USB Host permission from the user.
2. **USB HID Communication:** Once permission is granted, the app establishes a `UsbDeviceConnection` and claims the appropriate USB Interface. It does not use standard audio APIs (like AudioTrack) for control; instead, it uses the **USB Human Interface Device (HID)** protocol.
3. **Control Transfers:** When you change a setting in the UI (e.g., switching to High Gain), the `ViewModel` translates this action into a specific array of Hexadecimal bytes. The `UsbDacManager` then sends this payload directly to the DAC using `connection.controlTransfer()`. These specific byte payloads were discovered through protocol analysis (reverse-engineering the stock application).
4. **Reactive UI State:** The hardware state is wrapped in Kotlin `StateFlow`. Whenever a successful USB command is dispatched, the UI (built with Jetpack Compose) instantly and reactively updates to reflect the new hardware state.

---

## Key Features

- **Precise Volume Control**: Adjust the *Volume Index* (0-60) synchronized directly with the DAC hardware.
- **Channel Balance**: Fine-tune the Left and Right audio output balance with high precision.
- **Gain Control**: Toggle between *High Gain* (for power-hungry headphones) and *Low Gain* (for sensitive IEMs).
- **Digital Filter Selection**: Alter your sound signature with the device's built-in hardware filters:
  - *Fast roll-off Low latency (Fast LL)*
  - *Fast roll-off Phase-compensated (Fast PC)*
  - *Slow roll-off Low latency (Slow LL)*
  - *Slow roll-off Phase-compensated (Slow PC)*
  - *Non over-sampling (NOS)*
- **LED Configuration**: Customize the DAC's indicator light behavior (*On, Off, Off & Save*).
- **Dynamic Theming (Monet)**: The app interface and *Adaptive Icon* automatically adapt their colors to match your Android system wallpaper.

---

## Screenshots

> **Note for Contributors:** *(Replace the image links below with actual screenshots of the app later)*

<div align="center">
  <img src="https://via.placeholder.com/250x500.png?text=Home+Screen" width="250" alt="Home Screen"/>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://via.placeholder.com/250x500.png?text=Hardware+Config" width="250" alt="Hardware Config"/>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://via.placeholder.com/250x500.png?text=Dynamic+Colors" width="250" alt="Monet Theme"/>
</div>

---

## Local Build Instructions

Follow these steps to set up the development environment and build **Magivant** on your local machine.

#### **1. Prerequisites**
Before you begin, ensure you have the following installed:
* **Android Studio Ladybug (2024.2.1)** or newer.
* **Android SDK Platform 36**.
* **JDK 17** (comes bundled with modern Android Studio).
* A physical **Leteciel Magivant USB DAC** device (Emulators do not support USB Host hardware communication effectively).

#### **2. Clone the Repository**
Open your terminal and run:
```bash
git clone https://github.com/RapliVx/Magivant-Dac-App.git Magivant
cd Magivant
