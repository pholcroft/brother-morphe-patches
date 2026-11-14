# Brother Fast Preview Morphe patch

Experimental Morphe patch for Brother Print Service Plugin **1.17.0** and the **HL-L8260CDW** printer. It replaces a slow, sequential read of 45 paper-capability OIDs with the model-specific answers observed on one printer. This is a fixed-response proof of concept, not a general cache.

When applying the patch, enter the target printer's IPv4 address in the required option. Only single-object SNMP GET requests to that address and in the 45-object range are intercepted; all other requests follow Brother's original code. The address is embedded in the patched APK, not in the published patch bundle. If the printer address changes, reapply the patch. If another device uses that address, the fixed results may be used for it; a DHCP reservation is recommended. If your printer's capabilities differ, do not use this patch.

The patch returns fixed model capabilities, not live printer status. It has been applied to an APK with Morphe Desktop and tested using simulated Brother classes; the Morphe-patched APK has not yet been tested on a phone or physically printed. The earlier manually patched proof of concept showed a faster preview and the targeted queries disappeared from a subsequent packet capture.

## Test with Morphe

Add `https://raw.githubusercontent.com/pholcroft/brother-morphe-patches/main/patches-bundle.json` as a remote source, or choose the `.mpp` file as a local source. Select the original Brother APK, select **Fast preview for HL-L8260CDW**, enter your printer's current IPv4 address, then patch and install. The signed application may not install over an existing version signed with a different key. Do not uninstall an existing installation unless you are prepared to restore it and its settings.

The bundle and source contain no APK, packet capture, credentials or private printer address. Public paper-size values apply only to the tested model/configuration.

## Development

The project uses Morphe's patches template. Run `./gradlew buildAndroid` with Java 17+, Android SDK 36, and access to Morphe's GitHub Packages dependencies. Source: `patches/src/main/kotlin/app/template/patches/brother/FastPreviewPatch.kt`; Android extension: `extensions/extension/src/main/java/app/template/extension/extension/FixedResponses.java`. Tests under `tests/` use small simulated Brother classes and do not include proprietary APK code.

Licensed under GPL-3.0; see LICENSE and NOTICE.
