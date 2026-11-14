package com.brother.sdk.common.socket.devicemanagement.snmp;
import java.util.Vector;
import com.brother.sdk.common.socket.devicemanagement.snmp.westhawk.Varbind;
public class SnmpRequest { private byte msg_type; private Vector<Varbind> reqVarbinds = new Vector<>(); public SnmpRequest(String oid,int type) { this(oid,type,1); } public SnmpRequest(String oid,int type,int n) { msg_type=(byte)type; for (int i=0;i<n;i++) reqVarbinds.add(new Varbind(oid,null)); } }
