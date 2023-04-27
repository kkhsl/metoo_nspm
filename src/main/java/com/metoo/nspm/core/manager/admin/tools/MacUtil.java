package com.metoo.nspm.core.manager.admin.tools;

import com.metoo.nspm.core.mapper.nspm.TerminalMapper;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.nspm.impl.TerminalServiceImpl;
import com.metoo.nspm.core.utils.MyStringUtils;
import com.metoo.nspm.entity.nspm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MacUtil {


    @Autowired
    private IMacVendorService macVendorService;
    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private ITerminalHistoryService terminalHistoryService;
    @Autowired
    private IDeviceTypeService deviceTypeService;

    public List<Mac> macJoint(List<Mac> macs){
        if(macs != null && macs.size() > 0) {
            for (Mac mac : macs) {
                if (mac.getMac() != null && !mac.getMac().equals("")) {
                    String macAddr = mac.getMac();
                    int index = MyStringUtils.acquireCharacterPosition(macAddr, ":", 3);
                    if(index != -1){
                        macAddr = macAddr.substring(0, index);
                        Map params = new HashMap();
                        params.clear();
                        params.put("mac", macAddr);
                        List<MacVendor> macVendors = this.macVendorService.selectObjByMap(params);
                        if (macVendors.size() > 0) {
                            MacVendor macVendor = macVendors.get(0);
                            mac.setVendor(macVendor.getVendor());
                        }
                    }
                }
            }
        }
        return macs;
    }

    public void macVendor(Mac mac){
        if(mac != null) {
            if (mac.getMac() != null && !mac.getMac().equals("")) {
                String macAddr = mac.getMac();
                int index = MyStringUtils.acquireCharacterPosition(macAddr, ":", 3);
                if(index != -1){
                    macAddr = macAddr.substring(0, index);
                    Map params = new HashMap();
                    params.clear();
                    params.put("mac", macAddr);
                    List<MacVendor> macVendors = this.macVendorService.selectObjByMap(params);
                    if (macVendors.size() > 0) {
                        MacVendor macVendor = macVendors.get(0);
                        mac.setVendor(macVendor.getVendor());
                    }
                }
            }
        }
    }

    public List<Terminal> terminalJoint(List<Terminal> terminals){
        if(terminals != null && terminals.size() > 0) {
            for (Terminal terminal : terminals) {
                if (terminal.getMac() != null && !terminal.getMac().equals("")) {
                    String macAddr = terminal.getMac();
                    int index = MyStringUtils.acquireCharacterPosition(macAddr, ":", 3);
                    if(index != -1){
                        macAddr = macAddr.substring(0, index);
                        Map params = new HashMap();
                        params.clear();
                        params.put("mac", macAddr);
                        List<MacVendor> macVendors = this.macVendorService.selectObjByMap(params);
                        if (macVendors.size() > 0) {
                            MacVendor macVendor = macVendors.get(0);
                            terminal.setVendor(macVendor.getVendor());
                        }
                    }
                }
            }
        }
        return terminals;
    }

    public void writerType(List<Mac> macs){
        if(macs.size() > 0){
            Map params = new HashMap();
            macs.stream().forEach(e -> {
                params.clear();
                params.put("mac", e.getMac());
                List<Terminal> terminals = this.terminalService.selectObjByMap(params);
                if(terminals.size() > 0){
                    Terminal terminal = terminals.get(0);
                    if(terminal.getOnline()){
                        e.setOnline(true);
                    }
//                    if(terminal.getTerminalTypeId() != null){
//                        TerminalType terminalType = this.terminalTypeService.selectObjById(terminal.getTerminalTypeId());
//                        e.setTerminalTypeName(terminalType.getName());
//                    }
                    if(terminal.getDeviceTypeId() != null){
                        DeviceType deviceType = this.deviceTypeService.selectObjById(terminal.getDeviceTypeId());
                        e.setDeviceTypeName(deviceType.getName());
                    }
                }
            });
        }
    }

    public void writerHistoryOnline(List<Mac> macs){
        if(macs.size() > 0){
            Map params = new HashMap();
            macs.stream().forEach(e -> {
                params.clear();
                params.put("mac", e.getMac());
                List<Terminal> terminals = this.terminalService.selectObjByMap(params);
                if(terminals.size() > 0){
                    Terminal terminal = terminals.get(0);
                    if(terminal.getOnline()){
                        e.setOnline(true);
                    }

                }
            });
        }
    }

//    public void writerHistoryOnline(List<Mac> macs, Date time){
//        if(macs.size() > 0){
//            Map params = new HashMap();
//            macs.stream().forEach(e -> {
//                params.clear();
//                params.put("ip", e.getIp());
//                params.put("time", time);
//                List<TerminalHistory> terminalHistorys = this.terminalHistoryService.selectObjByMap(params);
//                if(terminalHistorys.size() > 0){
//                    TerminalHistory terminalHistory = terminalHistorys.get(0);
//                    if(terminalHistory.getOnline()){
//                        e.setOnline(true);
//                    }else{
//                        e.setOnline(false);
//                    }
//                }else{
//                    e.setOnline(false);
//                }
//            });
//        }
//    }

    public void getTerminalHistoryOnline(Terminal terminal, Date time){
        if(terminal != null){
            Map params = new HashMap();
            params.clear();
            params.put("ip", terminal.getIp());
            params.put("time", time);
            List<TerminalHistory> terminalHistorys = this.terminalHistoryService.selectObjByMap(params);
            if(terminalHistorys.size() > 0){
                TerminalHistory terminalHistory = terminalHistorys.get(0);
                if(terminalHistory.getOnline()){
                    terminal.setOnline(true);
                }else{
                    terminal.setOnline(false);
                }
            }else{
                terminal.setOnline(false);
            }
        }
    }

    public static void main(String[] args) {
        String macAddr = "50:0:0:26:0:2";
        String mac = supplement(macAddr);
        System.out.println(mac);
//        String[] strs = macAddr.split(":");
//        StringBuffer stringBuffer = new StringBuffer();
//        int i = 1;
//        for(String str : strs){
//            if(str.length() == 1){
//                stringBuffer.append(0).append(str);
//            }else{
//                stringBuffer.append(str);
//            }
//            if(i < strs.length){
//                stringBuffer.append(":");
//            }
//            i++;
//        }
//        System.out.println(stringBuffer.toString());
    }

    public List<Mac> supplements(List<Mac> macs){
        if(macs != null && macs.size() > 0) {
            for (Mac mac : macs) {
                if (mac.getMac() != null && !mac.getMac().equals("")) {
                    String macAddr = mac.getMac();
                    int one_index = macAddr.indexOf(":");
                    if(one_index != -1){
                        String[] strs = macAddr.split(":");
                        StringBuffer stringBuffer = new StringBuffer();
                        int i = 1;
                        for(String str : strs){
                            if(str.length() == 1){
                                stringBuffer.append(0).append(str);
                            }else{
                                stringBuffer.append(str);
                            }
                            if(i < str.length()){
                                stringBuffer.append(":");
                            }
                            i++;
                        }
                    }
                }
            }
        }
        return macs;
    }

    public static String supplement(String macAddr){
        int one_index = macAddr.indexOf(":");
        if(one_index != -1){
            String[] strs = macAddr.split(":");
            StringBuffer stringBuffer = new StringBuffer();
            int i = 1;
            for(String str : strs){
                if(str.length() == 1){
                    stringBuffer.append(0).append(str);
                }else{
                    stringBuffer.append(str);
                }
                if(i < strs.length){
                    stringBuffer.append(":");
                }
                i++;
            }
            macAddr = stringBuffer.toString();
        }
        return macAddr;
    }
}
