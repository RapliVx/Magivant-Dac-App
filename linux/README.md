# Linux desktop build

The Linux desktop app is a separate Compose Desktop application. Its controls and preset format use the shared Kotlin module, while USB communication uses libusb through usb4java. Android remains in the `app` module.

The first native builds support Linux `x86_64` and `AArch64` (ARM64). Build on the same CPU architecture as the package you intend to distribute; the Gradle packaging tasks do not cross-compile. The app image includes its JVM runtime and the usb4java native library, including libusb. Linux still needs its normal graphical desktop libraries at runtime.

## Build and run

Use JDK 21. The Linux desktop tasks do not require the Android SDK:

```bash
./gradlew :desktopApp:run
```

To create a self-contained Linux application image without an installer:

```bash
./gradlew :desktopApp:createDistributable
```

The app image is written under `desktopApp/build/compose/binaries/main/app/`. It can be copied to `/opt` or another location and launched from its `bin/` directory. The app image bundles a JVM, so an end-user Java installation is not required.

## Install the DAC USB permission rule

The app recognizes Magivant DAC IDs `2FC6:F13A` and `2FC6:F13B`. Linux requires a udev permission rule before a regular desktop user can access the USB control interface. On a system with `sudo` and `udevadm`, run:

```bash
bash linux/install-udev-rule.sh
```

For a GitHub release, download and extract the matching `Magivant-DAC-Linux-<architecture>-USB-access.tar.gz`, then run `bash install-udev-rule.sh` from the extracted folder.

Then unplug and reconnect the DAC. The rule grants access to the active logged-in user through the `uaccess` tag. Systems without a logind-compatible session manager may need an administrator to adapt the rule to their local USB access group. Audio playback itself remains managed by the system audio stack (ALSA, PipeWire, or PulseAudio); this app only changes DAC hardware settings.

## Distribution packages

Build a Debian package on Debian or Ubuntu with `dpkg-deb` installed:

```bash
./gradlew :desktopApp:packageDeb
```

Build an RPM package on Fedora, openSUSE, or another RPM-based distribution with `rpmbuild` installed:

```bash
./gradlew :desktopApp:packageRpm
```

The Compose Desktop packaging tasks are host-native, so create each package on the matching Linux environment. Debian and RPM packages do not install the udev rule; run the supplied script on the target system and reconnect the DAC. The Arch package installs the rule in the system udev rules directory.

For Arch Linux, build the included package recipe:

```bash
cd linux
makepkg -si
```

The PKGBUILD supports `x86_64` and `aarch64`, builds the same app image, installs a launcher and application icon, and places the udev rule in `/usr/lib/udev/rules.d/`. It requires `jdk21-openjdk` to build.
