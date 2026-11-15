import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.InetAddress;
import com.brother.sdk.network.BrDatagramSocket;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpRequest;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpResult;
import com.brother.sdk.common.socket.devicemanagement.snmp.westhawk.AsnInteger;
import app.template.extension.extension.FixedResponses;

public final class CapturedSnapshotTest {
    public static void main(String[] args) throws Exception {
        String encoded=new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8").trim();
        String raw=new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8");
        String address="192.0.2.4"; BrDatagramSocket socket=new BrDatagramSocket(InetAddress.getByName(address));
        int count=0;
        for(String row:raw.split(";")) {
            String[] fields=row.split(",",-1); int tag=Integer.parseInt(fields[1]);
            SnmpResult result=(SnmpResult)FixedResponses.lookup(socket,new SnmpRequest(fields[0],-96),address,encoded);
            if(result==null) throw new AssertionError("Unexpected network fallback");
            if(tag==0) { if(result.state!=SnmpResult.SnmpState.ErrorRequestNoSuchName) throw new AssertionError("Wrong absent result"); }
            else if(tag==4) {
                byte[] value=result.results[0].getValue().bytes; StringBuilder hex=new StringBuilder();for(byte b:value)hex.append(String.format("%02x",b&255));
                if(!hex.toString().equals(fields[2]))throw new AssertionError("Octet mismatch");
            } else {
                int actual=((AsnInteger)result.results[0].getValue()).number;
                int expected=new java.math.BigInteger(fields[2],16).intValue();
                int bits=fields[2].length()*4; if(bits<32 && (expected&(1<<(bits-1)))!=0)expected-=1<<bits;
                if(actual!=expected)throw new AssertionError("Integer mismatch");
            }
            count++;
        }
        if(count!=115)throw new AssertionError("Wrong count");
        System.out.println("All 115 captured responses reproduced locally");
    }
}
