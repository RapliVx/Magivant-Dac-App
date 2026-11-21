#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
rule="$script_dir/70-magivant-dac.rules"
destination="/etc/udev/rules.d/70-magivant-dac.rules"

if [[ ! -r "$rule" ]]; then
    printf 'Cannot read the Magivant DAC udev rule: %s\n' "$rule" >&2
    exit 1
fi

sudo install -Dm644 "$rule" "$destination"
sudo udevadm control --reload-rules
sudo udevadm trigger --subsystem-match=usb
printf 'Installed the udev rule. Unplug and reconnect the DAC before launching Magivant DAC.\n'
