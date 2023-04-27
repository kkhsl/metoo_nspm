package com.metoo.nspm.core.manager.admin.tools;

import com.metoo.nspm.core.service.nspm.IDepartmentService;
import com.metoo.nspm.core.service.nspm.IGroupService;
import com.metoo.nspm.core.service.nspm.IRsmsDeviceService;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.entity.nspm.Department;
import com.metoo.nspm.entity.nspm.Group;
import com.metoo.nspm.entity.nspm.RsmsDevice;
import com.metoo.nspm.entity.nspm.Terminal;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RsmsDeviceUtils {

    @Autowired
    private IRsmsDeviceService rsmsDeviceService;
    @Autowired
    private IGroupService groupService;

    public int syncUpdateDevice(String ip,String mac,  String location, String duty,
                                Long departmentId){
        if(ip != null && IpUtil.verifyIp(ip)){
            Map params = new HashMap();
            params.put("ip", ip);
            List<RsmsDevice> deviceList = this.rsmsDeviceService.selectObjByMap(params);
            if(deviceList.size() > 0){
                RsmsDevice rsmsDevice = deviceList.get(0);
                rsmsDevice.setDepartmentId(departmentId);
                rsmsDevice.setMac(mac);
                rsmsDevice.setLocation(location);
                rsmsDevice.setDuty(duty);
                int i = this.rsmsDeviceService.update(rsmsDevice);
                return i;
            }else{
                RsmsDevice rsmsDevice = new RsmsDevice();
                rsmsDevice.setAddTime(new Date());
                rsmsDevice.setDepartmentId(departmentId);
                rsmsDevice.setMac(mac);
                rsmsDevice.setLocation(location);
                rsmsDevice.setDuty(duty);
                int i = this.rsmsDeviceService.save(rsmsDevice);
                return i;
            }
        }
        return 0;
    }

    public int syncUpdateDevice(String ip, String name, String mac,  String location, String duty,
                                Long departmentId){
        if(ip != null && IpUtil.verifyIp(ip)){
            Map params = new HashMap();
            params.put("ip", ip);
            List<RsmsDevice> deviceList = this.rsmsDeviceService.selectObjByMap(params);
            if(deviceList.size() > 0){
                RsmsDevice rsmsDevice = deviceList.get(0);
                rsmsDevice.setDepartmentId(departmentId);
                rsmsDevice.setMac(mac);
                rsmsDevice.setLocation(location);
                rsmsDevice.setDuty(duty);
                int i = this.rsmsDeviceService.update(rsmsDevice);
                return i;
            }else{
                RsmsDevice rsmsDevice = new RsmsDevice();
                rsmsDevice.setAddTime(new Date());
                rsmsDevice.setName(name);
                rsmsDevice.setDepartmentId(departmentId);
                rsmsDevice.setMac(mac);
                rsmsDevice.setLocation(location);
                rsmsDevice.setDuty(duty);
                int i = this.rsmsDeviceService.save(rsmsDevice);
                return i;
            }
        }
        return 0;
    }

    public void syncUpdateDevice(String ip, String name, Long deviceTypeId, String mac,  String location, String duty,
                                Long departmentId){
        if(ip != null && IpUtil.verifyIp(ip)) {
            Map params = new HashMap();
            params.put("ip", ip);
            List<RsmsDevice> deviceList = this.rsmsDeviceService.selectObjByMap(params);
            RsmsDevice rsmsDevice = null;
            if (deviceList.size() > 0) {
                rsmsDevice = deviceList.get(0);
            } else {
                rsmsDevice = new RsmsDevice();
            }
            rsmsDevice.setIp(ip);
            rsmsDevice.setName(name);
            rsmsDevice.setDepartmentId(departmentId);
            rsmsDevice.setMac(mac);
            rsmsDevice.setLocation(location);
            rsmsDevice.setDuty(duty);
            rsmsDevice.setDeviceTypeId(deviceTypeId);
            if (rsmsDevice.getId() == null || rsmsDevice.getId().equals("")) {
                rsmsDevice.setAddTime(new Date());
                this.rsmsDeviceService.save(rsmsDevice);
            } else {
                this.rsmsDeviceService.update(rsmsDevice);
            }
        }
    }

    public Map getDeviceInfo(String ip){
        Map map = new HashMap();
        if(Strings.isNotBlank(ip)){
            boolean flag = IpUtil.verifyIp(ip);
            if(flag){
                Map params = new HashMap();
                params.put("ip", ip);
                List<RsmsDevice> rsmsDevices = this.rsmsDeviceService.selectObjByMap(params);
                if(rsmsDevices.size() > 0){
                    RsmsDevice device = rsmsDevices.get(0);
                    map.put("mac", device.getMac());
                    map.put("location", device.getLocation());
                    map.put("duty", device.getDuty());
                    if(device.getDepartmentId() != null){
                        Group department = this.groupService.selectObjById(device.getDepartmentId());
                        map.put("departmentName", department.getBranchName());
                        map.put("departmentId", department.getId());
                    }
                    return map;
                }
            }
        }
        return map;
    }

    public Map getDeviceInfo(Terminal terminal){
        Map map = new HashMap();
        map.put("location", terminal.getLocation());
        map.put("duty", terminal.getDuty());
        if(terminal.getDepartmentId() != null){
            Group department = this.groupService.selectObjById(terminal.getDepartmentId());
            map.put("departmentName", department.getBranchName());
            map.put("departmentId", department.getId());
        }
        return map;
    }
}
