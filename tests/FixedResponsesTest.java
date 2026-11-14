import java.net.InetAddress;
import com.brother.sdk.network.BrDatagramSocket;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpRequest;
import com.brother.sdk.common.socket.devicemanagement.snmp.SnmpResult;
import app.template.extension.extension.FixedResponses;

public class FixedResponsesTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String host = "192.0.2.4";
        BrDatagramSocket matching = new BrDatagramSocket(InetAddress.getByName(host));
        BrDatagramSocket other = new BrDatagramSocket(InetAddress.getByName("192.0.2.5"));
        String prefix = "1.3.6.1.4.1.2435.2.3.9.4.2.1.5.1.2.71.12.1.2.";
        Object result = FixedResponses.lookup(matching, new SnmpRequest(prefix + "1", -96), host);
        check(result instanceof SnmpResult, "expected a result");
        SnmpResult good = (SnmpResult)result;
        check(good.state == SnmpResult.SnmpState.SuccessRequest, "expected success");
        check(new String(good.results[0].getValue().bytes, "UTF-8").equals("iso_a4_210x297mm"), "expected matching bytes");
        String[] expected = {"iso_a4_210x297mm","na_letter_8.5x11in","na_legal_8.5x14in","na_executive_7.25x10.5in","iso_a5_148x210mm","iso_a6_105x148mm","iso_b5_176x250mm","na_number-10_4.125x9.5in","iso_dl_110x220mm","iso_c5_162x229mm","na_monarch_3.875x7.5in","na_index-3x5_3x5in","na_foolscap_8.5x13in","na_oficio_8.5x13.4in","custom_min_76.2x127mm","custom_max_215.9x355.6mm"};
        for (int i=1;i<=45;i++) {
            SnmpResult current=(SnmpResult)FixedResponses.lookup(matching,new SnmpRequest(prefix+i,-96),host);
            check(current!=null,"missing fixed response "+i);
            if (i<=16) check(new String(current.results[0].getValue().bytes,"UTF-8").equals(expected[i-1]),"wrong value "+i);
            else check(current.state==SnmpResult.SnmpState.ErrorRequestNoSuchName,"wrong error "+i);
        }
        SnmpResult absent = (SnmpResult)FixedResponses.lookup(matching, new SnmpRequest(prefix + "17", -96), host);
        check(absent.state == SnmpResult.SnmpState.ErrorRequestNoSuchName && absent.results == null, "expected noSuchName");
        check(FixedResponses.lookup(other, new SnmpRequest(prefix+"1", -96),host)==null,"other printer must fall back");
        check(FixedResponses.lookup(matching, new SnmpRequest(prefix+"46", -96),host)==null,"unknown OID must fall back");
        check(FixedResponses.lookup(matching, new SnmpRequest(prefix+"1", -95),host)==null,"GETNEXT must fall back");
        check(FixedResponses.lookup(matching, new SnmpRequest(prefix+"1", -96),"")==null,"empty address must fall back");
        check(FixedResponses.lookup(matching, new SnmpRequest(prefix+"1", -96, 2),host)==null,"multiple OIDs must fall back");
        System.out.println("Fixed-response behaviour checks passed");
    }
}
