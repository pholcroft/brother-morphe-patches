package app.template.patches.brother

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.stringOption

private const val CLIENT = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpClient;"
private const val REQUEST = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpRequest;"
private const val RESULT = "Lcom/brother/sdk/common/socket/devicemanagement/snmp/SnmpResult;"
private const val EXTENSION = "Lapp/template/extension/extension/FixedResponses;"

@Suppress("unused")
val fastBrotherPreview = patch(
    name = "Fast preview for HL-L8260CDW",
    description = "Experimental: serves this printer model's fixed paper capabilities locally."
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
    extendWith("extensions/extension.mpe")
    execute {
        val target = requireNotNull(printerAddress.value)
        Fingerprint(
            definingClass = CLIENT,
            name = "requestGet",
            returnType = RESULT,
            parameters = listOf(REQUEST, "I", "I")
        ).method.addInstructionsWithLabels(0, """
            iget-object v0, p0, $CLIENT->mSocket:Lcom/brother/sdk/common/socket/ISocket;
            const-string v1, "$target"
            invoke-static {v0, p1, v1}, $EXTENSION->lookup(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
            move-result-object v0
            if-eqz v0, :original
            check-cast v0, $RESULT
            return-object v0
            :original
            nop
        """)
    }
}
