package com.metoo.nspm.core.websocket.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.metoo.nspm.core.config.websocket.demo.NoticeWebsocketResp;
import com.metoo.nspm.core.manager.admin.tools.DateTools;
import com.metoo.nspm.core.manager.admin.tools.GroupTools;
import com.metoo.nspm.core.manager.admin.tools.MacUtil;
import com.metoo.nspm.core.manager.zabbix.tools.InterfaceUtil;
import com.metoo.nspm.core.service.api.zabbix.ZabbixService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.service.zabbix.IProblemService;
import com.metoo.nspm.core.service.zabbix.InterfaceService;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.core.utils.collections.ListSortUtil;
import com.metoo.nspm.dto.NetworkElementDto;
import com.metoo.nspm.entity.nspm.*;
import com.metoo.nspm.entity.zabbix.Interface;
import com.metoo.nspm.entity.zabbix.Item;
import com.metoo.nspm.entity.zabbix.Problem;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sun.management.snmp.util.SnmpCachedData;

import java.util.*;

/**
 * webwocket 网元列表
 */
@RequestMapping("/websocket/api/network")
@RestController
public class NetWorkManagerApi {

    @Autowired
    private INetworkElementService networkElementService;
    @Autowired
    private InterfaceService interfaceService;
    @Autowired
    private IGroupService groupService;
    @Autowired
    private GroupTools groupTools;
    @Autowired
    private InterfaceUtil interfaceUtil;
    @Autowired
    private IProblemService problemService;
    @Autowired
    private IMacHistoryService macHistoryService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ZabbixService zabbixService;
    @Autowired
    private MacUtil macUtil;
    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private ITerminalTypeService terminalTypeService;
    @Autowired
    private RedisResponseUtils redisResponseUtils;
    @Autowired
    private IDeviceTypeService deviceTypeService;
    @Autowired
    private IHostSnmpService hostSnmpService;

    public static void main(String[] args) {
        String i = "\\.";
        String ii = i;
        System.out.println(ii);
    }

    @ApiOperation("")
    @RequestMapping("/list")
    public NoticeWebsocketResp testApi(@RequestParam(value = "requestParams") String requestParams){
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(requestParams != null && !requestParams.isEmpty()){
            Map param = JSONObject.parseObject(requestParams, Map.class);
            String sessionId = (String) param.get("sessionId");
            Map result = new HashMap();
            // 获取类型
            NetworkElementDto dto = JSONObject.parseObject(param.get("params").toString(), NetworkElementDto.class);
            if (dto == null) {
                dto = new NetworkElementDto();
            }
            dto.setUserId(Long.parseLong(param.get("userId").toString()));
            if (dto.getGroupId() != null) {
                Group group = this.groupService.selectObjById(dto.getGroupId());
                if (group != null) {
                    Set<Long> ids = this.groupTools.genericGroupId(group.getId());
                    dto.setGroupIds(ids);
                }
            }
            Page<NetworkElement> page = this.networkElementService.selectConditionQuery(dto);
            if (page.getResult().size() > 0) {
                for (NetworkElement ne : page.getResult()) {
                    if (ne.getIp() != null) {
                        Interface obj = this.interfaceService.selectObjByIp(ne.getIp());
                        if (obj != null) {
                            ne.setAvailable(obj.getAvailable());
                            ne.setError(obj.getError());
                            result.put(ne.getIp(), obj.getAvailable());
                        }
                    }
                }
                rep.setNoticeType("1");
                rep.setNoticeStatus(1);
                rep.setNoticeInfo(result);
                this.redisResponseUtils.syncRedis(sessionId, result, 1);
            }
        }
        rep.setNoticeStatus(0);
        return rep;
    }

    @ApiOperation("5:拓扑端口事件")
    @GetMapping("/interface/event")
    public Object interfaceEvent(@RequestParam(value = "requestParams") String requestParams){
        List result = new ArrayList();
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(requestParams != null && !requestParams.equals("")){
            Map param = JSONObject.parseObject(String.valueOf(requestParams), Map.class);
            String sessionId = (String) param.get("sessionId");
            List<Object> list = JSONObject.parseObject(String.valueOf(param.get("params")), List.class);
            for (Object obj : list){
                Map ele = JSONObject.parseObject(obj.toString(), Map.class);
                Set<Map.Entry<String, String>> entrySet = ele.entrySet();
                for(Map.Entry<String, String> entry : entrySet){
                    if(entry.getKey().equals("from") || entry.getKey().equals("to")){
                        String[] data = entry.getValue().split("&");
                        if(data.length >= 2){
                            String ip = data[0];
                            String interfaceName = data[1];
                            // 获取端口事件状态
                            Map params = new HashMap();
                            params.clear();
                            params.put("ip", ip);
                            params.put("interfaceName", interfaceName);
                            params.put("event", "is not null");
                            params.put("objectid", "is not null");
                            List<Problem> problemList = this.problemService.selectObjByMap(params);
                            if(problemList.size() > 0){
                                List events = new ArrayList();
                                for(Problem problem : problemList){
                                    Map event = new HashMap();
                                    if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 0){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  3);
                                        events.add(event);
                                        break;
                                    }else if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 1){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  1);
                                    }else if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 2){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  1);
                                    }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 0){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  2);
                                    }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 1){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  1);
                                    }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 2){
                                        event.put("event", problem.getEvent());
                                        event.put("status", problem.getStatus());
                                        event.put("level",  1);
                                    }
                                    if(event.size() > 0){
                                        events.add(event);
                                    }
                                }
                                if(events.size() == 1){
                                    ele.put(entry.getKey(), events.get(0));
                                }else{
                                    if(events.size() > 0){
                                        ListSortUtil.intSort(events);
                                        ele.put(entry.getKey(), events.get(0));
                                    }else{
                                        Map event = new HashMap();
                                        event.put("status", 2);
                                        ele.put(entry.getKey(), event);
                                    }
                                }
                            }else{
                                Map event = new HashMap();
                                event.put("event", "");
                                event.put("status", 2);
                                ele.put(entry.getKey(), event);
                            }
                        }else{
                            ele.put(entry.getKey(), "");
                        }
                    }
                }
                result.add(ele);
            }
            rep.setNoticeStatus(1);
            rep.setNoticeType("5");
            rep.setNoticeInfo(result);
//            this.redisResponseUtils.syncRedis(sessionId, result, 5);

            this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 5);
            return rep;
        }
        rep.setNoticeType("5");
        rep.setNoticeStatus(0);
        return rep;
    }

//    @ApiOperation("拓扑端口事件")
//    @GetMapping("/interface/event")
//    public Object interfaceEvent(@RequestParam(value = "requestParams") String param){
//        Map map = new HashMap();
//        if(param != null && !param.equals("")){
//            List<String> requestParams = JSONObject.parseObject(param, List.class);
//            for (String requestParam : requestParams){
//                String[] data = requestParam.split("&");
//                String ip = data[0];
//                String interfaceName = data[1];
//                // 获取端口snmp可用性
////                String avaliable = this.interfaceUtil.getInterfaceAvaliable(ip);
////                result.put("snmp", avaliable);
//                // 获取端口事件状态
//                Map params = new HashMap();
//                params.clear();
//                params.put("ip", ip);
//                params.put("interfaceName", interfaceName);
//                params.put("event", "is not null");
//                List<Problem> problemList = this.problemService.selectObjByMap(params);
//                if(problemList.size() > 0){
//                    List list = new ArrayList();
//                    for(Problem problem : problemList){
//                        Map event = new HashMap();
//                        if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 0){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  3);
//                            list.add(event);
//                            break;
//                        }else if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 1){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  1);
//                        }else if(problem.getEvent().equals("interfacestatus") && problem.getStatus() == 2){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  1);
//                        }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 0){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  2);
//                        }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 1){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  1);
//                        }else if(problem.getEvent().equals("trafficexceeded") && problem.getStatus() == 2){
//                            event.put("event", problem.getEvent());
//                            event.put("status", problem.getStatus());
//                            event.put("level",  1);
//                        }
//                        if(event.size() > 0){
//                            list.add(event);
//                        }
//                    }
//                    if(list.size() == 1){
//                        map.put(requestParam, list.get(0));
//                    }else{
//                        if(list.size() > 0){
//                            ListSortUtil.intSort(list);
//                            map.put(requestParam, list.get(0));
//                        }else{
//                            Map event = new HashMap();
//                            event.put("status", 2);
//                            map.put(requestParam, event);
//                        }
//                    }
//                }else{
//                    Map event = new HashMap();
//                    event.put("event", "");
//                    event.put("status", 2);
//                    map.put(requestParam, event);
//                }
//            }
//        }
//        NoticeWebsocketResp rep = new NoticeWebsocketResp();
//        if (!map.isEmpty()) {
//            rep.setNoticeStatus(1);
//            rep.setNoticeType("5");
//            rep.setNoticeInfo(map);
//        }else{
//            rep.setNoticeType("5");
//            rep.setNoticeStatus(0);
//        }
//        return rep;
//    }

//    @ApiOperation("拓扑设备状态")
//    @GetMapping("/snmp/status")
//    public Object snmapStatus(String ips){
//        Map map = new HashMap();
//        if(ips != null && !ips.equals("")){
//            String[] iparray = ips.split(",");
//            for (String ip : iparray){
//                // 获取端口snmp可用性
//                String avaliable = this.interfaceUtil.getInterfaceAvaliable(ip);
//                map.put(ip, avaliable);
//            }
//        }
//        NoticeWebsocketResp rep = new NoticeWebsocketResp();
//        if (!map.isEmpty()) {
//            rep.setNoticeStatus(1);
//            rep.setNoticeType("2");
//            rep.setNoticeInfo(map);
//        }else{
//            rep.setNoticeType("2");
//            rep.setNoticeStatus(0);
//        }
//        return rep;
//    }

    @ApiOperation("拓扑|设备状态")
    @GetMapping("/snmp/status")
    public Object status(@RequestParam(value = "requestParams") String requestParams){
        List result = new ArrayList();
        String sessionId = "";
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(requestParams != null && !requestParams.equals("")){
            Map param = JSONObject.parseObject(String.valueOf(requestParams), Map.class);
            sessionId = (String) param.get("sessionId");
            Date time = DateTools.parseDate(String.valueOf(param.get("time")), "yyyy-MM-dd HH:mm");
            List<Object> ips = JSONObject.parseObject(param.get("params").toString(), List.class);
            if(time == null){
                for (Object ip : ips){
                    try {
                        Map map = new HashMap();
                        String[] str = ip.toString().split("&");
                        // 获取端口snmp可用性
                        String avaliable = this.interfaceUtil.getInterfaceAvaliable(str[0]);
                        map.put("snmp", avaliable);
                        map.put("uuid", str[1]);
                        result.add(map);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }else{
                for (Object ip : ips){
                    try {
                        Map map = new HashMap();
                        String[] str = ip.toString().split("&");
                        // 获取端口snmp可用性
                        Map args = new HashMap();
                        args.put("time", time);
                        args.put("uuid", str[1]);
                        List<HostSnmp> snmp = this.hostSnmpService.selectObjByMap(args);
                        if(snmp.size() > 0){
                            map.put("snmp", snmp.get(0).getAvaliable());
                            map.put("uuid", str[1]); result.add(map);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
            rep.setNoticeType("2");
            rep.setNoticeInfo(result);
            this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 2);
//            this.redisResponseUtils.syncRedis(sessionId, result, 2);
            return rep;
        }
        rep.setNoticeType("2");
        return rep;
    }


    @ApiOperation("9：网元|端口列表")
    @GetMapping("/ne/interface/all")
    public NoticeWebsocketResp neInterfaces(@RequestParam(value = "requestParams", required = false) String requestParams){
        Map requestParam = JSONObject.parseObject(requestParams, Map.class);
        if(!requestParam.isEmpty()){
            String sessionId = (String) requestParam.get("sessionId");
            Map result = new HashMap();
            Map param = JSONObject.parseObject(String.valueOf(requestParam.get("params")), Map.class);
            Date time = DateTools.parseDate(String.valueOf(requestParam.get("time")), "yyyy-MM-dd HH:mm");
            for(Object key : param.keySet()){
                String value = param.get(key).toString();
                JSONArray ary = JSONArray.parseArray(value);
                if(ary.size() > 0){
                    Map map = new HashMap();
                    for (Object obj : ary) {
                        JSONObject ele = JSONObject.parseObject(obj.toString());
                        String uuid = ele.getString("uuid");
                        NetworkElement ne = this.networkElementService.selectObjByUuid(uuid);
                        if(ne != null){
                            // 端口列表
                            String interfaces = ele.getString("interface");
                            JSONArray array = JSONArray.parseArray(interfaces);
                            if(array.size() > 0){
                                List list = new ArrayList();
                                for(Object inf : array){
                                    Map args = new HashMap();
                                    args.put("uuid", uuid);
                                    args.put("interfaceName", inf);
                                    args.put("tag", "DT");
                                    args.put("orderBy", "ip");
                                    args.put("orderType", "ASC");
                                    List<Mac> macs = null;
                                    if(time != null && !"".equals(time)){
                                        args.put("time", time);
                                        macs = this.macHistoryService.selectObjByMap(args);
                                        if(macs != null && macs.size() > 0){
                                            this.macUtil.macJoint(macs);
                                            list.addAll(macs);
                                        }
                                    }else {
                                        args.put("online", 1);
                                        List<Terminal> terminals = this.terminalService.selectObjByMap(args);
                                        if (terminals != null && terminals.size() > 0) {
                                            macUtil.terminalJoint(terminals);
                                            terminals.stream().forEach(e -> {
                                                DeviceType deviceType = this.deviceTypeService.selectObjById(e.getDeviceTypeId());
                                                if(deviceType != null){
                                                    e.setDeviceTypeName(deviceType.getName());
                                                }
                                            });
                                            this.macUtil.terminalJoint(terminals);
                                            if (terminals != null && terminals.size() > 0) {
                                                list.addAll(terminals);
                                            }
                                        }
                                    }
                                }
                                map.put(uuid, list);
                            }
                        }
                    }
                    result.put(key, map);
                }
            }
            NoticeWebsocketResp rep = new NoticeWebsocketResp();
            rep.setNoticeType("9");
            rep.setNoticeStatus(1);
            rep.setNoticeInfo(result);
            this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 9);
            return rep;
        }
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        rep.setNoticeType("9");
        rep.setNoticeStatus(0);
        return rep;
    }

//
//
//    @ApiOperation("9：网元|端口列表")
//    @GetMapping("/ne/interface/all")
//    public NoticeWebsocketResp neInterfaces(@RequestParam(value = "requestParams", required = false) String requestParams){
//        Map requestParam = JSONObject.parseObject(requestParams, Map.class);
//        if(!requestParam.isEmpty()){
//            String sessionId = (String) requestParam.get("sessionId");
//            Map subarea = new HashMap();
//            Map param = JSONObject.parseObject(String.valueOf(requestParam.get("params")), Map.class);
//            Date time = DateTools.parseDate(String.valueOf(param.get("time")), "yyyy-MM-dd HH:mm");
//            for(Object key : param.keySet()){
//                String value = param.get(key).toString();
//                JSONArray ary = JSONArray.parseArray(value);
//                if(ary.size() > 0){
//                    Map map = new HashMap();
//                    for (Object obj : ary) {
//                        JSONObject ele = JSONObject.parseObject(obj.toString());
//                        String uuid = ele.getString("uuid");
//                        NetworkElement ne = this.networkElementService.selectObjByUuid(uuid);
//                        if(ne != null){
//                            // 端口列表
//                            String interfaces = ele.getString("interface");
//                            JSONArray array = JSONArray.parseArray(interfaces);
//                            if(array.size() > 0){
//                                Map interfaceMap = new HashMap();
//                                for(Object inf : array){
//                                    Map flux_terminal = new HashMap();
//                                    Map args = new HashMap();
//                                    args.put("uuid", uuid);
//                                    args.put("interfaceName", inf);
//                                    args.put("tag", "DT");
//                                    args.put("orderBy", "ip");
//                                    args.put("orderType", "ASC");
//                                    List<Mac> macs = null;
//                                    if(time != null && !"".equals(time)){
//                                        args.put("time", time);
//                                        macs = this.macHistoryService.selectObjByMap(args);
//                                        if(macs != null && macs.size() > 0){
//                                            this.macUtil.macJoint(macs);
//                                            this.macUtil.writerType(macs);
//                                            flux_terminal.put("terminal", macs);
//                                        }else{
//                                            List a = new ArrayList<>();
//                                            flux_terminal.put("terminal", a);
//                                        }
//                                    }else {
//                                        args.put("online", 1);
//                                        List<Terminal> terminals = this.terminalService.selectObjByMap(args);
//                                        if (terminals != null && terminals.size() > 0) {
//                                            macUtil.terminalJoint(terminals);
//                                            terminals.stream().forEach(e -> {
//                                                TerminalType terminalType = this.terminalTypeService.selectObjById(e.getTerminalTypeId());
//                                                e.setTerminalTypeName(terminalType.getName());
//                                            });
//                                            this.macUtil.terminalJoint(terminals);
//                                            if (terminals != null && terminals.size() > 0) {
//                                                flux_terminal.put("terminal", terminals);
//                                            } else {
//                                                List a = new ArrayList<>();
//                                                flux_terminal.put("terminal", a);
//                                            }
//                                        }
//                                    }
//
//                                    if(time == null || "".equals(time)){
//                                        // 流量
//                                        Map flux = new HashMap();
//                                        args.clear();
//                                        args.put("ip", ne.getIp());
//                                        // 采集ifbasic,然后查询端口对应的历史流量
//                                        args.put("tag", "ifreceived");
//                                        args.put("available", 1);
//                                        List<Item> items = this.itemService.selectTagByMap(args);
////                                Map ele = new HashMap();
//                                        if(items.size() > 0){
//                                            for (Item item : items) {
//                                                String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
//                                                flux.put("received", lastvalue);
//                                                break;
//                                            }
//                                        } else{
//                                            flux.put("received", "0");
//                                        }
//                                        args.clear();
//                                        args.put("ip", ne.getIp());
//                                        // 采集ifbasic,然后查询端口对应的历史流量
//                                        args.put("tag", "ifsent");
//                                        args.put("available", 1);
//                                        List<Item> ifsents = this.itemService.selectTagByMap(args);
//                                        if(ifsents.size() > 0){
//                                            for (Item item : ifsents) {
//                                                String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
//                                                flux.put("sent", lastvalue);
//                                                break;
//                                            }
//                                        }else{
//                                            flux.put("sent", "0");
//                                        }
//                                        flux_terminal.put("flux", flux);
//                                    }
//                                    interfaceMap.put(inf, flux_terminal);
//                                }
//                                map.put(uuid, interfaceMap);
//                            }
//                        }
//                    }
//                    subarea.put(key, map);
//                }
//            }
//
//            NoticeWebsocketResp rep = new NoticeWebsocketResp();
//            rep.setNoticeType("9");
//            rep.setNoticeStatus(1);
//            rep.setNoticeInfo(subarea);
//            this.redisResponseUtils.syncRedis(sessionId, subarea, 9);
//            return rep;
//        }
//        NoticeWebsocketResp rep = new NoticeWebsocketResp();
//        rep.setNoticeType("9");
//        rep.setNoticeStatus(0);
//        return rep;
//    }

    @ApiOperation("11：终端")
    @GetMapping("/ne/partition/terminal")
    public NoticeWebsocketResp partitionTerminal(@RequestParam(value = "requestParams", required = false) String requestParams){
        Map<String, Object> requestParam = JSONObject.parseObject(requestParams, Map.class);
        String sessionId = (String) requestParam.get("sessionId");
        Date time = DateTools.parseDate(String.valueOf(requestParam.get("time")), "yyyy-MM-dd HH:mm");
        Map<String, JSONArray> param = JSONObject.parseObject(String.valueOf(requestParam.get("params")), Map.class);
        Map result = new HashMap();
        if(!param.isEmpty()){
            Map params = new HashMap();
            Set<Map.Entry<String, JSONArray>> keys = param.entrySet();
            if(time == null){
                for (Map.Entry<String, JSONArray> entry : keys) {
                    if(entry.getValue() != null && entry.getValue().size() > 0){
                        List list = new ArrayList<>();
                        entry.getValue().stream().forEach(m ->{
                            params.clear();
                            params.put("mac", m);
                            List<Terminal> terminals = this.terminalService.selectObjByMap(params);
                            macUtil.terminalJoint(terminals);
                            if(terminals.size() > 0){
                                Terminal terminal = terminals.get(0);
//                            TerminalType terminalType = this.terminalTypeService.selectObjById(terminal.getTerminalTypeId());
//                            terminal.setTerminalTypeName(terminalType.getName());
                                DeviceType deviceType = this.deviceTypeService.selectObjById(terminal.getDeviceTypeId());
                                if(deviceType != null){
                                    terminal.setDeviceTypeName(deviceType.getName());
                                }
                                list.add(terminal);
                            }
                        });
                        result.put(entry.getKey(), list);
                    }
                }
            }else{
                for (Map.Entry<String, JSONArray> entry : keys) {
                    if(entry.getValue() != null && entry.getValue().size() > 0){
                        List list = new ArrayList<>();
                        entry.getValue().stream().forEach(e ->{
                            params.clear();
                            params.put("mac", e);
                            params.put("time", time);
                            params.put("tag", "DT");
                            List<Mac> macs = this.macHistoryService.selectObjByMap(params);
                            if(macs.size() > 0){
                                macUtil.macVendor(macs.get(0));
                                Mac mac = macs.get(0);
                                list.add(mac);
                            }
                        });
                        result.put(entry.getKey(), list);
                    }
                }
            }
            NoticeWebsocketResp rep = new NoticeWebsocketResp();
            rep.setNoticeType("11");
            rep.setNoticeStatus(1);
            rep.setNoticeInfo(result);
            this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 11);
            return rep;
        }
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        rep.setNoticeType("11");
        rep.setNoticeStatus(0);
        return rep;
    }

//    @ApiOperation("11：网元|设备状态")
//    @GetMapping("/ne/partition/terminal")
//    public NoticeWebsocketResp partitionTerminal(@RequestParam(value = "requestParams", required = false) String requestParams){
//
//        Map<String, Object> requestParam = JSONObject.parseObject(requestParams, Map.class);
//        String sessionId = (String) requestParam.get("sessionId");
//
//        Map<String, JSONArray> param = JSONObject.parseObject(String.valueOf(requestParam.get("params")), Map.class);
//        Map result = new HashMap();
//        if(!param.isEmpty()){
//            Map params = new HashMap();
//            Set<Map.Entry<String, JSONArray>> keys = param.entrySet();
//            for (Map.Entry<String, JSONArray> entry : keys) {
//                if(entry.getValue() != null && entry.getValue().size() > 0){
//                    List list = new ArrayList<>();
//                    entry.getValue().stream().forEach(m ->{
//                        Map flux_terminal = new HashMap();
//                        params.clear();
//                        params.put("mac", m);
//                        List<Terminal> terminals = this.terminalService.selectObjByMap(params);
//                        macUtil.terminalJoint(terminals);
//                        if(terminals.size() > 0){
//                            Terminal terminal = terminals.get(0);
//                            TerminalType terminalType = this.terminalTypeService.selectObjById(terminal.getTerminalTypeId());
//                            terminal.setTerminalTypeName(terminalType.getName());
//                            // 流量
//                            Map flux = new HashMap();
//                            Map args = new HashMap();
//                            args.clear();
//                            args.put("ip", terminal.getDeviceIp());
//                            // 采集ifbasic,然后查询端口对应的历史流量
//                            args.put("tag", "ifreceived");
//                            args.put("available", 1);
//                            List<Item> items = this.itemService.selectTagByMap(args);
//                            if(items.size() > 0){
//                                for (Item item : items) {
//                                    String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
//                                    flux.put("received", lastvalue);
//                                    break;
//                                }
//                            } else{
//                                flux.put("received", "0");
//                            }
//                            args.clear();
//                            args.put("ip", terminal.getDeviceIp());
//                            // 采集ifbasic,然后查询端口对应的历史流量
//                            args.put("tag", "ifsent");
//                            args.put("available", 1);
//                            List<Item> ifsents = this.itemService.selectTagByMap(args);
//                            if(ifsents.size() > 0){
//                                for (Item item : ifsents) {
//                                    String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
//                                    flux.put("sent", lastvalue);
//                                    break;
//                                }
//                            }else{
//                                flux.put("sent", "0");
//                            }
//                            flux_terminal.put("terminal", terminal);
//                            flux_terminal.put("flux", flux);
//                            list.add(flux_terminal);
//                        }
//                    });
//                    result.put(entry.getKey(), list);
//                }
//            }
//
//            NoticeWebsocketResp rep = new NoticeWebsocketResp();
//            rep.setNoticeType("11");
//            rep.setNoticeStatus(1);
//            rep.setNoticeInfo(result);
//            this.redisResponseUtils.syncRedis(sessionId, result, 11);
//            return rep;
//        }
//        NoticeWebsocketResp rep = new NoticeWebsocketResp();
//        rep.setNoticeType("11");
//        rep.setNoticeStatus(0);
//        return rep;
//    }

//    @ApiOperation("11：网元|设备状态")
//    @GetMapping("/ne/partition/terminal")
//    public NoticeWebsocketResp partitionTerminal(@RequestParam(value = "requestParams", required = false) String requestParams){
//        Map<String, JSONArray> requestParam = JSONObject.parseObject(requestParams, Map.class);
//        Map result = new HashMap();
//        if(!requestParam.isEmpty()){
//            Map params = new HashMap();
//            Set<Map.Entry<String, JSONArray>> keys = requestParam.entrySet();
//            for (Map.Entry<String, JSONArray> entry : keys) {
//                if(entry.getValue() != null && entry.getValue().size() > 0){
//                    Map map = new HashMap();
//                    entry.getValue().stream().forEach(m ->{
//                        params.clear();
//                        params.put("mac", m);
//                        List<Terminal> terminals = this.terminalService.selectObjByMap(params);
//                        if(terminals.size() > 0){
//                            Terminal terminal = terminals.get(0);
//                            Map mm = new HashMap();
//                            mm.put("online", terminal.getOnline());
//                            TerminalType terminalType = this.terminalTypeService.selectObjById(terminal.getTerminalTypeId());
//                            mm.put("terminalTypeName", terminalType.getName());
//                            mm.put("interfaceStatus", terminal.getInterfaceStatus());
//                            map.put(m, mm);
//                        }
//                    });
//                    result.put(entry.getKey(), map);
//                }
//            }
//
//            NoticeWebsocketResp rep = new NoticeWebsocketResp();
//            rep.setNoticeType("11");
//            rep.setNoticeStatus(1);
//            rep.setNoticeInfo(result);
//            return rep;
//        }
//        NoticeWebsocketResp rep = new NoticeWebsocketResp();
//        rep.setNoticeType("11");
//        rep.setNoticeStatus(0);
//        return rep;
//    }

}
