# Brother Fast Preview Morphe patch

Experimental Morphe patch for Brother Print Service Plugin **1.17.0**. This speed-only experiment intercepts single-object SNMP GETs to a selected printer using a snapshot supplied when patching. It returns the snapshot value when known and `noSuchName` for unknown OIDs. This is a fixed-response experiment, not a general cache.

When applying the patch, enter the target printer's IPv4 address and a private snapshot in the two required options. The snapshot is compressed, encoded text made from recorded SNMP replies; it is not encryption and can contain printer identifiers. Keep it private. Both options are embedded in the resulting APK, not in the published patch bundle. Only single-object SNMP GET requests to that address are intercepted; discovery broadcasts, other printer addresses, other request types, multiple-object requests and internal exceptions still follow Brother's original code. If the printer address changes, reapply the patch. A DHCP reservation is recommended.

The patch can fabricate stale or missing device state and deliberately ignores live paper, toner and printer availability. It may break preview or printing. The earlier 45-OID proof of concept showed a faster preview and the targeted queries disappeared from a subsequent packet capture. The aggressive snapshot version must be verified on an Android device with a new capture.

## Test with Morphe

Add `https://raw.githubusercontent.com/pholcroft/brother-morphe-patches/refs/heads/main/patches-bundle.json` as a remote source, or choose the `.mpp` file as a local source. Select the original Brother APK, select **Fast preview for HL-L8260CDW**, enter the printer's current IPv4 address and snapshot text, then patch and install. The signed application may not install over an existing version signed with a different key. Do not uninstall an existing installation unless you are prepared to restore it and its settings.

The bundle and source contain no APK, packet capture, snapshot, credentials or private printer address. Create a private snapshot from your own authorized capture using `oid,tag,hex` rows separated by semicolons, UTF-8 encoded, GZIP-compressed and Base64-encoded. Tags are 4 for octets, 2 for integers, and 0 for noSuchName. The installed patched APK contains the snapshot and must not be shared without reviewing it.

## Development

The project uses Morphe's patches template. Run `./gradlew buildAndroid` with Java 17+, Android SDK 36, and access to Morphe's GitHub Packages dependencies. Source: `patches/src/main/kotlin/app/template/patches/brother/FastPreviewPatch.kt`; Android extension: `extensions/extension/src/main/java/app/template/extension/extension/FixedResponses.java`. Tests under `tests/` use small simulated Brother classes and do not include proprietary APK code.

Licensed under GPL-3.0; see LICENSE and NOTICE.
