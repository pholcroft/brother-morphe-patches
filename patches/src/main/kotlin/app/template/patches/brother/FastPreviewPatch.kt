package app.template.patches.brother

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.stringOption
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val CLIENT = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpClient;"
private const val REQUEST = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpRequest;"
private const val RESULT = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpResult;"
private const val EXTENSION = "Lapp/template/extension/extension/FixedResponses;"

private fun validSnapshot(input: String?): Boolean = try {
    val clean = input?.filterNot(Char::isWhitespace)
    require(clean != null && clean.length in 1..12000 && clean.matches(Regex("[A-Za-z0-9+/]+={0,2}")))
    val bytes = GZIPInputStream(ByteArrayInputStream(Base64.getDecoder().decode(clean))).use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            require(output.size() + count <= 65536)
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
    require(bytes.size in 1..65536)
    val seen = mutableSetOf<String>()
    bytes.toString(Charsets.UTF_8).split(';').all { row ->
        val fields = row.split(',', limit = 3)
        fields.size == 3 && fields[0].matches(Regex("[0-9]+(\\.[0-9]+)+")) && seen.add(fields[0]) &&
            fields[1] in listOf("0", "2", "4") && fields[2].matches(Regex("([0-9a-f]{2})*")) &&
            (fields[1] != "0" || fields[2].isEmpty()) &&
            (fields[1] != "2" || fields[2].length in 2..8)
    }
} catch (_: Exception) { false }

@Suppress("unused")
val fastBrotherPreview = patch(
    name = "Fast preview for HL-L8260CDW",
    description = "Aggressive experiment: answer captured SNMP GETs locally using a private snapshot; unknown GETs return noSuchName."
) {
    compatibleWith(Compatibility(
        name = "Brother Print Service Plugin",
        packageName = "com.brother.printservice",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0033A0,
        targets = listOf(AppTarget(version = "1.17.0"))
    ))
    val printerAddress = stringOption(
        key = "printerAddress",
        title = "Printer IPv4 address",
        description = "Required. Fixed responses are used only for this address.",
        required = true
    ) { input ->
        input != null && input.split('.').let { parts ->
            parts.size == 4 && parts.all { part ->
                part.isNotEmpty() && part.all(Char::isDigit) &&
                    (part == "0" || !part.startsWith('0')) && (part.toIntOrNull() ?: -1) in 0..255
            }
        }
    }
    val snapshotData = stringOption(
        key = "snapshotData",
        title = "Captured response snapshot",
        description = "Required. Paste the private snapshot text generated from your captures.",
        required = true,
        validator = { validSnapshot(it) }
    )
    extendWith("extensions/extension.mpe")
    execute {
        val target = requireNotNull(printerAddress.value)
        val snapshot = requireNotNull(snapshotData.value).filterNot(Char::isWhitespace)
        Fingerprint(
            definingClass = CLIENT,
            name = "requestGet",
            returnType = RESULT,
            parameters = listOf(REQUEST, "I", "I")
        ).method.addInstructionsWithLabels(0, """
            iget-object v0, p0, $CLIENT->mSocket:Lcom/brother/sdk/common/socket/ISocket;
            const-string v1, "$target"
            const-string v2, "$snapshot"
            invoke-static {v0, p1, v1, v2}, $EXTENSION->lookup(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
            move-result-object v0
            if-eqz v0, :original
            check-cast v0, $RESULT
            return-object v0
            :original
            nop
        """)
    }
}
