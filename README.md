# Brother Fast Preview Morphe patches

Experimental patches for Brother Print Service Plugin **1.17.0**. Select only one of the two patches when patching the original APK.

## Automatic printer response cache

Select **Automatic printer response cache** and deselect **Fast preview for HL-L8260CDW**. No printer address or snapshot is needed. Brother discovers printers as usual. The patch learns supported single-object SNMP responses as the service requests them, saves them privately on the Android device per IP address, and reuses them on later requests. A first preview may be slower while values are learned. Timeouts and unsupported response types are not cached; unrecognized requests go to the printer. Other traffic, including discovery, is unchanged.

In Android's print service settings, open the Brother service settings to enable or disable caching, clear data for one or all learned printers, and optionally check reachability and readiness. The optional background check uses IPP to report printer state and whether it accepts jobs. It does not delay preview, change Brother's print status, or guarantee that a job will print. A MAC address is shown if learned, but the cache is keyed by IP and does not verify the MAC on reuse. An IP change causes a new cache; an IP reassigned to a different printer may reuse old data until cleared.

Cached replies may be stale and give incorrect paper, toner or availability information. This mode has been built and tested locally but still needs testing on an Android device.

## Fixed snapshot (existing fast version)

Select **Fast preview for HL-L8260CDW** and deselect the automatic patch. Enter the printer IPv4 address and private snapshot when patching. This speed-only experiment intercepts single-object SNMP GETs to that address, returns recorded values and responds `noSuchName` for unknown OIDs. Discovery and other request types are unchanged. The snapshot can contain identifiers and is embedded in the resulting APK. The original version 1.1.1 bundle remains available in this repository.

## Morphe source

Add `https://raw.githubusercontent.com/pholcroft/brother-morphe-patches/refs/heads/main/patches-bundle.json` as a remote source and refresh it. Select the original Brother APK, choose one patch and install the resulting APK. A differently signed installation may need to be removed before installing; removing it also removes its data and settings.

The public bundle contains no APK, capture, private snapshot, or private printer address.

## Development

Build with `./gradlew buildAndroid`, Java 17+, Android SDK 36 and access to Morphe's GitHub Packages dependencies. Tests under `tests/` use simulated Brother classes and contain no proprietary APK code.

Licensed under GPL-3.0; see LICENSE and NOTICE.
