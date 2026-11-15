import java.net.InetAddress;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import com.brother.sdk.network.BrDatagramSocket;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpRequest;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpResult;
import app.template.extension.extension.FixedResponses;

public class FixedResponsesTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static String snapshot(String input) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(GZIPOutputStream gzip=new GZIPOutputStream(out)){gzip.write(input.getBytes("UTF-8"));}
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    public static void main(String[] args) throws Exception {
        String host="192.0.2.4";
        BrDatagramSocket matching=new BrDatagramSocket(InetAddress.getByName(host));
        BrDatagramSocket other=new BrDatagramSocket(InetAddress.getByName("192.0.2.5"));
        String oid="1.3.6.1.2.1.1.1.0", integer="1.3.6.1.2.1.43.10.2.1.2.1.1", absent="1.3.6.1.2.1.43.9.2.1.7.1.3";
        String snap=snapshot(oid+",4,0041ff;"+integer+",2,ff;"+absent+",0,");
        SnmpResult octets=(SnmpResult)FixedResponses.lookup(matching,new SnmpRequest(oid,-96),host,snap);
        check(java.util.Arrays.equals(octets.results[0].getValue().bytes,new byte[]{0,65,-1}),"octet bytes must match exactly");
        SnmpResult n=(SnmpResult)FixedResponses.lookup(matching,new SnmpRequest(integer,-96),host,snap);
        check(((com.brother.sdk.common.socket.devicemanagement.snmp.westhawk.AsnInteger)n.results[0].getValue()).number==-1,"signed integer mismatch");
        SnmpResult missing=(SnmpResult)FixedResponses.lookup(matching,new SnmpRequest(absent,-96),host,snap);
        check(missing.state==SnmpResult.SnmpState.ErrorRequestNoSuchName,"recorded missing object");
        SnmpResult unknown=(SnmpResult)FixedResponses.lookup(matching,new SnmpRequest(oid+".999",-96),host,snap);
        check(unknown.state==SnmpResult.SnmpState.ErrorRequestNoSuchName,"unknown object must not contact printer");
        check(FixedResponses.lookup(other,new SnmpRequest(oid,-96),host,snap)==null,"other printer must fall back");
        check(FixedResponses.lookup(matching,new SnmpRequest(oid,-95),host,snap)==null,"GETNEXT must fall back");
        check(FixedResponses.lookup(matching,new SnmpRequest(oid,-96),"",snap)==null,"empty address must fall back");
        check(FixedResponses.lookup(matching,new SnmpRequest(oid,-96,2),host,snap)==null,"multiple OIDs must fall back");
        check(FixedResponses.lookup(matching,new SnmpRequest(oid,-96),host,"invalid")==null,"invalid snapshot must fail safely");
        System.out.println("Aggressive snapshot behaviour checks passed");
    }
}
