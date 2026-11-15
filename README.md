# Faster print previews for Brother printers

**Speed up print previews by remembering your printer's details instead of asking for them every time.** The first preview may be slower while the app learns; later previews will open much faster. No printer address or snapshot to enter.

This is an unofficial, experimental Morphe patch for **Brother Print Service Plugin 1.17.0**. It has been tested on a Google Pixel 9 with a Brother HL-L8260CDW. Other Brother printers may benefit, but have not yet been verified.

## Get started

1. Add this Morphe source and refresh it:
   `https://raw.githubusercontent.com/pholcroft/brother-morphe-patches/refs/heads/main/patches-bundle.json`
2. Select the original Brother Print Service Plugin 1.17.0 APK and apply **Faster print previews**.
3. Install the patched APK and open a print preview. Subsequent previews can reuse saved responses.

The first preview may still be slow while responses are learned. The patched APK may have a different signing key from your existing installation; replacing that installation may require uninstalling it, which can delete its settings and data. Keep a way to restore the original app.

## Settings

Open Android's printing settings, then Brother Print Service Plugin's settings. Under **Faster print previews**, you can turn the cache on or off, enable an optional background readiness check, or choose **Manage saved printer data** to clear a printer or all saved printers. Turning the cache off leaves saved data intact and uses normal requests.

The readiness check reports the printer's IPP state and whether it accepts jobs. It does not block preview, update Brother's own print status, or guarantee a successful print.

## How it works and limitations

The patch saves supported individual SNMP GET responses per printer address in the app's private storage and reuses them. Timeouts, unsupported response types, multi-object requests, discovery broadcasts and other traffic follow the original path. The cache can contain stale status or capability information, so the service may show incorrect paper, toner or availability information. Clear a printer's saved data to relearn it.

The cache uses the printer's IP address; a MAC address may be recorded but is not checked on reuse. A changed IP is learned as a new printer. If an address is reassigned to another printer, clear the old saved data first. There is no automatic expiry.

Read the [technical analysis and packet timelines](TECHNICAL-ANALYSIS.md) for the measurements, proposed improvements and evidence limits. The public source contains no APK, packet capture, private snapshot or private printer address.

## Help improve support for more printers

Have another Brother model or one with extra features? A short recording of the print service's network traffic can help us understand its requests and improve the cache. Follow the [PCAPdroid capture and contribution guide](CAPTURING-PRINTER-TRAFFIC.md), including the privacy checks before sharing.

## Development

Build with `./gradlew buildAndroid`, Java 17+, Android SDK 36 and access to Morphe's GitHub Packages dependencies. Tests under `tests/` use simulated Brother classes and contain no proprietary APK code.

Licensed under GPL-3.0; see LICENSE and NOTICE.
