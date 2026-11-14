package app.template.extension.extension;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

/** Fixed values tested on an HL-L8260CDW. A failed lookup always returns control to Brother. */
public final class FixedResponses {
    private static final String SNMP = "com.brother.sdk.common.socket.devicemanagement.snmp.";
    private static final String WEST = SNMP + "westhawk.";
    private static final String OID = "1.3.6.1.4.1.2435.2.3.9.4.2.1.5.1.2.71.12.1.2.";
    private static final String[] PAPERS = {
        "iso_a4_210x297mm", "na_letter_8.5x11in", "na_legal_8.5x14in", "na_executive_7.25x10.5in",
        "iso_a5_148x210mm", "iso_a6_105x148mm", "iso_b5_176x250mm", "na_number-10_4.125x9.5in",
        "iso_dl_110x220mm", "iso_c5_162x229mm", "na_monarch_3.875x7.5in", "na_index-3x5_3x5in",
        "na_foolscap_8.5x13in", "na_oficio_8.5x13.4in", "custom_min_76.2x127mm", "custom_max_215.9x355.6mm"
    };
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    public static Object lookup(Object socket, Object request, String target) {
        try {
            if (socket == null || request == null || target == null || target.isEmpty() ||
                !socket.getClass().getName().equals("com.brother.sdk.network.BrDatagramSocket")) return null;
            InetAddress address = (InetAddress) field(socket, "mInetAddress");
            if (address == null || !target.equals(address.getHostAddress()) || ((Byte)field(request,"msg_type")) != (byte)0xa0) return null;
            Vector<?> bindings = (Vector<?>)field(request, "reqVarbinds");
            if (bindings.size() != 1) return null;
            Object oidObject = bindings.get(0).getClass().getMethod("getOid").invoke(bindings.get(0));
            String oid = (String)oidObject.getClass().getMethod("getValue").invoke(oidObject);
            if (!oid.startsWith(OID)) return null;
            String suffix = oid.substring(OID.length());
            int index = Integer.parseInt(suffix);
            if (!suffix.equals(Integer.toString(index)) || index < 1 || index > 45) return null;
            ClassLoader loader = request.getClass().getClassLoader();
            Class<?> resultClass = loader.loadClass(SNMP + "SnmpResult");
            Object result = resultClass.getConstructor().newInstance();
            if (index > PAPERS.length) {
                Class<?> stateClass = loader.loadClass(SNMP + "SnmpResult$SnmpState");
                resultClass.getField("state").set(result, stateClass.getField("ErrorRequestNoSuchName").get(null));
                return result;
            }
            Class<?> asnObject = loader.loadClass(WEST + "AsnObject");
            Class<?> octets = loader.loadClass(WEST + "AsnOctets");
            Class<?> varbind = loader.loadClass(WEST + "Varbind");
            Object value = octets.getConstructor(byte[].class).newInstance((Object)PAPERS[index-1].getBytes(StandardCharsets.UTF_8));
            Object binding = varbind.getConstructor(String.class, asnObject).newInstance(oid, value);
            Object array = Array.newInstance(varbind, 1); Array.set(array, 0, binding);
            resultClass.getField("results").set(result, array);
            return result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }
    private FixedResponses() {}
}
