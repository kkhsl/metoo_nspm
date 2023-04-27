package com.metoo.nspm.core.manager.admin.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.util.StringUtil;
import com.metoo.nspm.core.manager.admin.tools.*;
import com.metoo.nspm.core.service.api.zabbix.ZabbixService;
import com.metoo.nspm.core.service.nspm.*;
import com.metoo.nspm.core.manager.admin.tools.CompareUtils;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.core.utils.ResponseUtil;
import com.metoo.nspm.core.utils.file.DownLoadFileUtil;
import com.metoo.nspm.core.utils.network.IpUtil;
import com.metoo.nspm.core.utils.network.IpV4Util;
import com.metoo.nspm.core.utils.query.PageInfo;
import com.metoo.nspm.dto.TopologyDTO;
import com.github.pagehelper.Page;
import com.metoo.nspm.dto.zabbix.RoutDTO;
import com.metoo.nspm.entity.nspm.*;
import com.metoo.nspm.entity.zabbix.History;
import com.metoo.nspm.entity.zabbix.Item;
import com.metoo.nspm.entity.zabbix.ItemTag;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.nutz.json.Json;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.annotation.Repeat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/admin/topology")
@RestController
public class TopologyManagerController {

    @Autowired
    private ITopologyService topologyService;
    @Autowired
    private ITopologyHistoryService topologyHistoryService;
    @Autowired
    private IGroupService groupService;
    @Autowired
    private GroupTools groupTools;
    @Autowired
    private IPresetPathService presetPathService;
    @Autowired
    private IRoutService routService;
    @Autowired
    private IAccessoryService accessoryService;
    @Autowired
    private IArpService arpService;
    @Autowired
    private IArpHistoryService arpHistoryService;
    @Autowired
    private IIPAddressService ipAddressServie;
    @Autowired
    private IIPAddressHistoryService ipAddressHistoryServie;
    @Autowired
    private IRoutTableService routTableService;
    @Autowired
    private IRoutHistoryService routHistoryService;
    @Autowired
    private ISubnetService zabbixSubnetService;
    @Autowired
    private INetworkElementService networkElementService;
    @Autowired
    private SubnetTool subnetTool;
    @Autowired
    private RoutTool routTool;
    @Autowired
    private ITerminalService terminalService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ZabbixService zabbixService;
    @Autowired
    private RsmsDeviceUtils rsmsDeviceUtils;

    @RequestMapping("/list")
    public Object list(@RequestBody(required = false) TopologyDTO dto){
        if(dto == null){
            dto = new TopologyDTO();
        }
        User user = ShiroUserHolder.currentUser();
        if(dto.getGroupId() == null){
            dto.setGroupId(user.getGroupId());
        }
        if(dto.getGroupId() != null){
            Group group = this.groupService.selectObjById(dto.getGroupId());
            if(group != null){
                Set<Long> ids = this.groupTools.genericGroupId(group.getId());
                dto.setGroupIds(ids);
            }
        }
        Page<Topology> page = this.topologyService.selectConditionQuery(dto);
        if(page.getResult().size() > 0) {
            if(page.getResult().size() == 1){
                // 设置默认拓扑
                try {
                    this.setTopologyDefualt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ResponseUtil.ok(new PageInfo<Topology>(page));
        }
        return ResponseUtil.ok();
    }

    @GetMapping("/add")
    public Object add(){
        Map map = new HashMap();
        User user = ShiroUserHolder.currentUser();
        Group parent = this.groupService.selectObjById(user.getGroupId());
        List<Group> groupList = new ArrayList<>();
        if(parent != null){
            this.groupTools.genericGroup(parent);
            groupList.add(parent);
        }
        map.put("group", groupList);
        return ResponseUtil.ok(map);
    }

    @GetMapping("/update")
    public Object update(Long id){
        if(id == null){
            return  ResponseUtil.badArgument();
        }
        Map map = new HashMap();
        User user = ShiroUserHolder.currentUser();
        Group parent = this.groupService.selectObjById(user.getGroupId());
        List<Group> groupList = new ArrayList<>();
        if(parent != null){
            this.groupTools.genericGroup(parent);
            groupList.add(parent);
        }
        return ResponseUtil.ok(map);
    }

    @ApiOperation("拓扑修改名称")
    @GetMapping("/rename")
    public Object rename(Long id, String name){
        if(name == null || name.equals("")){
            return  ResponseUtil.badArgument("拓扑名称不能为空");
        }
        Map params = new HashMap();
//        params.put("id", id);
        params.put("name", name);
        params.put("NotId", id);
        List<Topology> topologies = this.topologyService.selectObjByMap(params);
        if(topologies.size() > 0){
            return  ResponseUtil.badArgument("拓扑名称已存在");
        }else{
            if(name != null && !name.equals("")){
                Topology topology = this.topologyService.selectObjById(id);
                if(topology != null){
                    topology.setName(name);
                    if(topology.getSuffix() != null && !topology.getSuffix().equals("")){
                        topology.setSuffix(null);
                    }
                    int i = this.topologyService.update(topology);
                    if(i == 1){
                        return ResponseUtil.ok();
                    }else{
                        return ResponseUtil.error();
                    }
                }else{
                    return  ResponseUtil.resourceNotFound();
                }
            }
        }
        return ResponseUtil.badArgument();
    }

    @ApiOperation("拓扑复制")
    @GetMapping("/copy")
    public Object copy(String id, String name, String groupId){
        Map params = new HashMap();
        if(name != null && !name.equals("")){
            params.clear();
            params.put("name", name);
            params.put("NotId", id);
            List<Topology> Topos = this.topologyService.selectObjByMap(params);
            if(Topos.size() > 0){
                return  ResponseUtil.badArgument("拓扑名称已存在");
            }
        }
        // 分组
        Group group = this.groupService.selectObjById(Long.parseLong(groupId));
        if(group == null){
            return ResponseUtil.badArgument("分组不存在");
        }
        params.clear();
        params.put("id", id);
        List<Topology> topologies = this.topologyService.selectObjByMap(params);
        if(topologies.size() > 0){
            Topology copyTopology = topologies.get(0);
            Long returnId = this.topologyService.copy(copyTopology);
            if(returnId != null){
                Topology topology = this.topologyService.selectObjById(Long.parseLong(String.valueOf(returnId)));
                if(topology != null){
                    if(name != null && !name.equals("")){
                        topology.setName(name);
                    }else{
                        String suffix = this.changName(copyTopology.getSuffix(), 1);
                        topology.setSuffix(suffix);
                    }
                    topology.setGroupId(group.getId());
                    topology.setGroupName(group.getBranchName());
                    this.topologyService.update(topology);
                    return ResponseUtil.ok();
                }
            }else{
                return ResponseUtil.error();
            }
        }
        return ResponseUtil.badArgument();
    }

    @RequestMapping("/test")
    public void test(String name){
        this.changName(name, 1);
    }


    public String changName(String suffix, int num){
        if(suffix == null || suffix.equals("")){
            int number = num;
            if(number == 0){
                number = 1;
            }
            String name = "副本" + " (" + number + ")";
            Topology topology = this.topologyService.selectObjBySuffix(name);
            if(topology != null){
                number ++;
                return this.changName(null, number);
            }
            return name;
        }else{
            int number = num;
            if(number == 0){
                number = 1;
            }
            String name = suffix + " 副本 (" + number + ")";
            Topology topology = this.topologyService.selectObjBySuffix(name);
            if(topology != null){
                number ++;
                return this.changName(suffix, number);
            }
            return name;
        }
    }

    @ApiOperation("保存拓扑")
    @RequestMapping("/save")
    public Object save(@RequestBody(required = false) Topology instance){
        // 校验拓扑名称是否重复
        Map params = new HashMap();
        if(instance.getId() == null
                || instance.getId().equals("")
                ){
            if(StringUtils.isEmpty(instance.getName())){
                return ResponseUtil.badArgument("拓扑名称不能为空");
            }
        }
        if(StringUtils.isNotEmpty(instance.getName())){
            params.put("topologyId", instance.getId());
            params.put("name", instance.getName());
            List<Topology> topologList = this.topologyService.selectObjByMap(params);
            if(topologList.size() > 0){
                return ResponseUtil.badArgument("拓扑名称重复");
            }
        }
        // 校验分组
        if(instance.getGroupId() != null){
            Group group = this.groupService.selectObjById(instance.getGroupId());
            if(group != null){
                instance.setGroupId(group.getId());
                instance.setGroupName(group.getBranchName());
            }
        }else{
            User user = ShiroUserHolder.currentUser();
            Group group = this.groupService.selectObjById(user.getGroupId());
            if(group != null){
                instance.setGroupId(group.getId());
                instance.setGroupName(group.getBranchName());
            }
        }
        if(instance.getContent() != null && !instance.getContent().equals("")){
            String str = JSONObject.toJSONString(instance.getContent());
            instance.setContent(str);
        }

        int result = this.topologyService.save(instance);
        if(result >= 1){
            // 设置默认拓扑
            try {
                this.setTopologyDefualt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseUtil.ok(result);
        }
        return ResponseUtil.badArgument();
    }

    @ApiOperation("二层信息弹框位置")
    @PutMapping("/update/location")
    public Object updateTopologyLocation(@RequestBody(required = false) Topology instance){
        if(instance.getId() == null || instance.getId().equals("")){
            return ResponseUtil.badArgument();
        }
        Topology topology = this.topologyService.selectObjById(instance.getId());
        if(topology == null){
            return ResponseUtil.badArgument();
        }
        User user = ShiroUserHolder.currentUser();
        if(!topology.getGroupId().equals(user.getGroupId())){
            return ResponseUtil.badArgument();
        }
        int i = this.topologyService.update(instance);
        if(i >= 1){
            return ResponseUtil.ok();
        }
        return ResponseUtil.error();
    }

    @DeleteMapping("/delete")
    public Object delete(String ids){
        if(ids != null && !ids.equals("")){
            for (String id : ids.split(",")){
                Topology obj = this.topologyService.selectObjById(Long.parseLong(id));
                if(obj.getIsDefault()){
                    return ResponseUtil.badArgument("拓扑【" + obj.getName() + "】为默认地址");
                }
                Map params = new HashMap();
                try {
                    User user = ShiroUserHolder.currentUser();
                    List<Topology> topologies = this.selectObjById(user, obj.getId(), null);
                    if(topologies != null && topologies.size() >= 1){
                        int i = this.topologyService.delete(obj.getId());
                        if(i >= 1){
                            // 设置默认拓扑
                            try {
                                this.setTopologyDefualt();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if(obj != null){
                                params.clear();
                                params.put("topologyId", obj.getId());
                                List<PresetPath> presetPaths = this.presetPathService.selectObjByMap(params);
                                for(PresetPath presetPath : presetPaths){
                                    try {
                                        presetPath.setTopologyName(null);
                                        presetPath.setTopologyId(null);
                                        this.presetPathService.update(presetPath);
//                                        this.presetPathService.delete(presetPath.getId());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }else{
                        return ResponseUtil.badArgument();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseUtil.ok("拓扑【" + obj.getName() + "】删除失败");
                }
            }
            return ResponseUtil.ok();
        }
        return ResponseUtil.badArgument();
    }

    /**
     * 设置乐观锁，多用户同时登陆，避免并发提交
     * @param id
     * @return
     */
    @ApiOperation("设置默认拓扑")
    @RequestMapping("/default")
    public Object isDefault(String id){
        Topology obj = this.topologyService.selectObjById(Long.parseLong(id));
        if(obj != null){
            obj.setIsDefault(true);
            User user = ShiroUserHolder.currentUser();
            List<Topology> topologyList = this.defaultList(user, true);
            if(topologyList.size() > 0){
                Topology topology = topologyList.get(0);
                if(obj == topology){
                    return ResponseUtil.ok();
                }else{
                    topology.setIsDefault(false);
                    this.topologyService.update(topology);
                }
            }
            this.topologyService.update(obj);
            return ResponseUtil.ok();
        }
        return ResponseUtil.ok();
    }

    @ApiOperation("拓扑信息")
    @GetMapping("/info")
    public Object info(
            @RequestParam(value = "id") Long id,
            @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
            @RequestParam(value = "time", required = false) Date time){
        if(id == null){
            return  ResponseUtil.badArgument();
        }
        User user = ShiroUserHolder.currentUser();
        List<Topology> topologies = selectObjById(user, id, time);
        if(topologies != null && topologies.size() > 0){
            Topology topology = topologies.get(0);
            if(topology.getContent() != null && !topology.getContent().equals("")){
                JSONObject content = JSONObject.parseObject(topology.getContent().toString());
                topology.setContent(content);
            }
            return ResponseUtil.ok(topology);
        }
        return ResponseUtil.ok();
    }

    @ApiOperation("默认拓扑")
    @GetMapping("/default/topology")
    public Object defaultTopology(){
        Map map = new HashMap();
        User user = ShiroUserHolder.currentUser();
        Map params = new HashMap();
        Group group = this.groupService.selectObjById(user.getGroupId());
        if(group != null){
            Set<Long> ids = this.groupTools.genericGroupId(group.getId());
            params.put("groupIds", ids);
            params.put("isDefault", true);
            List<Topology> topologies = this.topologyService.selectObjByMap(params);
            if (topologies.size() > 0){
                Topology topology = topologies.get(0);
                if(topology.getContent() != null && !topology.getContent().equals("")){
                    System.out.println(JSON.toJSONString(topology.getContent()));
                    JSONObject content = JSONObject.parseObject(topology.getContent().toString());
                    topology.setContent(content);
                }
                map.put("topology", topologies.get(0));
                return ResponseUtil.ok(map);
            }
        }
        return ResponseUtil.ok(map);
    }


    @ApiOperation("拓扑路径列表")
    @PostMapping("/path")
    public Object path(@RequestParam(value = "id") Long id){
        // 是否改为当前用户所在组
        User user = ShiroUserHolder.currentUser();
        if(id != null && !id.equals("")){
            List<Topology> topologies = selectObjById(user, id, null);
            if(topologies != null && topologies.size() > 0){
                Map params = new HashMap();
                Topology topology = this.topologyService.selectObjById(id);
                params.put("topologyId", topology.getId());
                List<PresetPath> presetPaths = this.presetPathService.selectObjByMap(params);
                return ResponseUtil.ok(presetPaths);
            }
            return ResponseUtil.badArgument();
        }
        return ResponseUtil.ok();
    }

    public void setTopologyDefualt(){
        User user = ShiroUserHolder.currentUser();
        Map params = new HashMap();
        Group group = this.groupService.selectObjById(user.getGroupId());
        if(group != null) {
            Set<Long> ids = this.groupTools.genericGroupId(group.getId());
            params.put("groupIds", ids);
            List<Topology> topologies = this.topologyService.selectObjByMap(params);
            if(topologies.size() == 1){
                Topology topology = topologies.get(0);
                topology.setIsDefault(true);
                this.topologyService.update(topology);
            }
        }
    }

    public  List<Topology> topologyList(User user){
        Map params = new HashMap();
        Group group = this.groupService.selectObjById(user.getGroupId());
        if(group != null) {
            Set<Long> ids = this.groupTools.genericGroupId(group.getId());
            params.put("groupIds", ids);
            List<Topology> topologies = this.topologyService.selectObjByMap(params);
            return topologies;
        }
        return null;
    }

    public  List<Topology> defaultList(User user, boolean isDefault){
        Map params = new HashMap();
        Group group = this.groupService.selectObjById(user.getGroupId());
        if(group != null) {
            Set<Long> ids = this.groupTools.genericGroupId(group.getId());
            params.put("groupIds", ids);
            params.put("isDefault", true);
            List<Topology> topologies = this.topologyService.selectObjByMap(params);
            return topologies;
        }
        return null;
    }

    public  List<Topology> selectObjById(User user, Long id, Date time){
        Map params = new HashMap();
        Group group = this.groupService.selectObjById(user.getGroupId());
        if(group != null) {
            Set<Long> ids = this.groupTools.genericGroupId(group.getId());
            List<Topology> topologies = null;
            params.put("groupIds", ids);
            params.put("id", id);
            if(time == null){
                topologies = this.topologyService.selectObjByMap(params);
            }else{
                params.put("time", time);
                topologies = this.topologyHistoryService.selectObjByMap(params);
            }
            return topologies;
        }
        return null;
    }

    @ApiOperation("设备路由列表")
    @PostMapping("/device/rout")
    public Object deviceRoutList(@RequestBody RoutDTO dto){
        String ip = "";
        Integer maskBit = 0;
        if(StringUtil.isEmpty(dto.getDeviceUuid())){
            return ResponseUtil.badArgument("请选择设备");
        }else{
            String destination = dto.getDestination();
            if(destination != null && !destination.equals("")){
                boolean cidr = IpV4Util.verifyCidr(destination);
                if(cidr){
                    maskBit = Integer.parseInt(destination.replaceAll(".*/",""));
                    ip = destination.replaceAll("/.*","");
                    dto.setDestination(ip);
                }else{
                    boolean flag = IpV4Util.verifyIp(destination);
                    if(flag){
                        ip = destination;
                        maskBit = 32;
                    }else{
                        return ResponseUtil.badArgument();
                    }
                }
                dto.setDestination(IpUtil.ipConvertDec(dto.getDestination()));
            }
        }
        if(dto.getTime() == null){
            return this.routService.queryDeviceRout(dto, ip);
        }else{
            return this.routHistoryService.queryDeviceRout(dto, ip);
        }
    }

    public void sort(List<Route> list){
        Collections.sort(list, new Comparator<Route>() {
            @Override
            public int compare(Route o1, Route o2) {
                String key1 = IpUtil.ipConvertDec(o1.getDestination());
                String key2 = IpUtil.ipConvertDec(o2.getDestination());
                Long ikey1 = Long.parseLong(key1);
                Long ikey2 = Long.parseLong(key2);
                return ikey1 - (ikey2) > 0 ? 1 : -1; // 升序
            }
        });
    }

    @ApiOperation("设备路由列表（历史）")
    @PostMapping("/device/rout/history")
    public Object deviceRoutListHistory(@RequestBody RoutDTO dto){
        if(StringUtil.isEmpty(dto.getDeviceUuid())){
            return ResponseUtil.badArgument();
        }else{
            if(dto.getDestination() != null && !dto.getDestination().equals("")){
                dto.setDestination(IpUtil.ipConvertDec(dto.getDestination()));
            }
        }
        Page<Route> page = null;
        page = this.routHistoryService.selectConditionQuery(dto);
        if(page != null && page.getResult().size() > 0) {
            return ResponseUtil.ok(new PageInfo<Route>(page));
        }
        return ResponseUtil.ok();
    }

    @RequestMapping("/upload")
    public Object upload(
            @RequestParam(value = "file", required = false) MultipartFile file){
        if(file != null && file.getSize() > 0){
            boolean accessory = this.uploadFile(file);
            if(accessory){
                return ResponseUtil.ok();
            }else{
                return ResponseUtil.error();
            }
        }
        return ResponseUtil.badArgument();
    }

    public boolean uploadFile(@RequestParam(required = false) MultipartFile file){
        String path = "C:\\Users\\46075\\Desktop\\新建文件夹";
        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String picNewName = fileName + ext;
        String imgRealPath = path  + File.separator + picNewName;
        Date currentDate = new Date();
        String fileName1 = DateTools.getCurrentDate(currentDate);
        System.out.println(path + "\\" + fileName1 + ".conf");
        java.io.File imageFile = new File(path +  "\\" + fileName1 + ".conf");
        if (!imageFile.getParentFile().exists()) {
            imageFile.getParentFile().mkdirs();
        }
        try {
            file.transferTo(imageFile);
            Accessory accessory = new Accessory();
            accessory.setA_name(picNewName);
            accessory.setA_path(path);
            accessory.setA_ext(ext);
            accessory.setA_size((int)file.getSize());
            accessory.setType(3);
            this.accessoryService.save(accessory);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @GetMapping("/down")
    public Object down(HttpServletResponse response){
        String path = "C:\\Users\\46075\\Desktop\\新建文件夹\\20221017122442.conf";
        if(path != null && !path.equals("")){
            File file = new File(path);
            if(file != null){
                DownLoadFileUtil.downloadZip(file, response);
                return ResponseUtil.ok();
            }else{
                return ResponseUtil.error();
            }
        }
        return ResponseUtil.badArgument();
    }

//    @ApiOperation("二层设备查询")
//    @RequestMapping("/layer_2_device")
//    public Object routTable(String srcIp, Integer srcMask, String destIp, Integer destMask){
//        if(StringUtil.isEmpty(srcIp)){
//            return ResponseUtil.badArgument("起点Ip不能为空");
//        }
//        if(!IpUtil.verifyIp(srcIp)){
//            return ResponseUtil.badArgument("起点Ip格式错误");
//        }
//        Arp srcArp = this.arpService.selectObjByIp(IpUtil.ipConvertDec(srcIp));
//        if(srcArp == null){
//            return ResponseUtil.badArgument("起点Ip不存在");
//        }
//        if(StringUtil.isEmpty(destIp)){
//            return ResponseUtil.badArgument("终点Ip不能为空");
//        }
//        if(!IpUtil.verifyIp(destIp)){
//            return ResponseUtil.badArgument("终点Ip格式错误");
//        }
//        Map map = new HashMap();
//        Arp destArp = this.arpService.selectObjByIp(IpUtil.ipConvertDec(destIp));
//        if(destArp == null){
//            return ResponseUtil.badArgument("终点Ip不存在");
//        }
//        Map params = new HashMap();
//        // 获取起点ip网络地址和广播地址
//        Map origin =  null;
//        List<Subnet> subnets = this.zabbixSubnetService.selectSubnetByParentId(null);
//        if(subnets.size() > 0){
//            if(srcIp != null){
//                if(srcIp.equals("0.0.0.0")){
//                }
//                if(!IpUtil.verifyIp(srcIp)){
//                }
//                // 判断ip地址是否属于子网
//                for(Subnet subnet : subnets){
//                    Subnet sub = this.subnetTool.verifySubnetByIp(subnet, srcIp);
//                    if(sub != null){
//                        String mask = IpUtil.bitMaskConvertMask(sub.getMask());
//                        origin = IpUtil.getNetworkIpDec(sub.getIp(), mask);
//                        break;
//                    }
//                }
//            }
//        }
//        if(origin == null || origin.isEmpty()){
//            return ResponseUtil.badArgument("起点Ip不存在");
//        }
//        IpAddress srcIpAddress = this.ipAddressServie.querySrcDevice(origin);
//        if(srcIpAddress == null){
//            return ResponseUtil.badArgument("起点Ip不存在");
//        }
//
//        Map dest = null;
//        // 获取起点ip网络地址和广播地址
//        if(subnets.size() > 0){
//            if(destIp != null){
//                if(destIp.equals("0.0.0.0")){
//                }
//                if(!IpUtil.verifyIp(destIp)){
//                }
//                // 判断ip地址是否属于子网
//                for(Subnet subnet : subnets){
//                    Subnet sub = this.subnetTool.verifySubnetByIp(subnet, destIp);
//                    if(sub != null){
//                        String mask = IpUtil.bitMaskConvertMask(sub.getMask());
//                        dest = IpUtil.getNetworkIpDec(sub.getIp(), mask);
//                        break;
//                    }
//                }
//            }
//        }
//        if(dest == null || dest.isEmpty()){
//            return ResponseUtil.badArgument("终点Ip不存在");
//        }
//        // 终点设备
//        IpAddress destIpAddress = this.ipAddressServie.querySrcDevice(dest);
//        if(destIpAddress == null){
//            return ResponseUtil.badArgument("终点Ip不存在");
//        }
//        map.put("destinationDevice", destIpAddress);
//            this.routTableService.truncateTable();
//            // 保存起点设备
//            params.clear();
//            params.put("ip", srcIpAddress.getIp());
//            params.put("mask", srcIpAddress.getMask());
//            params.put("deviceName", srcIpAddress.getDeviceName());
//            params.put("interfaceName", srcIpAddress.getInterfaceName());
//            params.put("mac", srcIpAddress.getMac());
//            List<RouteTable> routTables = this.routTableService.selectObjByMap(params);
//            RouteTable routTable = null;
//            if(routTables.size() > 0){
//                routTable = routTables.get(0);
//            }else{
//                routTable = new RouteTable();
//            }
//            String[] IGNORE_ISOLATOR_PROPERTIES = new String[]{"id"};
//            BeanUtils.copyProperties(srcIpAddress, routTable, IGNORE_ISOLATOR_PROPERTIES);
//            this.routTableService.save(routTable);
//
//            // 路由查询
//            this.routTool.generatorRout(srcIpAddress, destIp, destIpAddress.getMask());
//            List<RouteTable> routTableList = this.routTableService.selectObjByMap(null);
//            map.put("routTable", routTableList);
//            // 二层路径
//            if(true){
//                // 起点二层路径
//                List<Mac> src_layer_2_device = this.routTool.generetorSrcLayer_2_device(srcArp.getMac(), srcIpAddress.getDeviceName());
//                int number = src_layer_2_device.size();
//                boolean flag = true;
//                if(number == 1){
//                    Mac mac = src_layer_2_device.get(0);
//                    if(mac.getTag().equals("L")){
//                        flag = false;
//                    }
//                }
//                if(number > 1 && flag){
//                    flag = false;
//                    map.put("src_layer_2_device", src_layer_2_device);
//                    map.put("srcRemoteDevice", new ArrayList<>());
//                }
//                if(flag){
//                    // 查找 二层路径 路由起点设备的remote设备为有起点ip的mac地址记录且与起点设备相连的那台设备
//                    params.clear();
//                    params.put("mac", srcArp.getMac());
//                    List<Mac> srcRemoteDevice = this.macService.selectByMap(params);
//                    if(srcRemoteDevice.size() > 0){
//                        Mac mac = srcRemoteDevice.get(0);
//                        params.clear();
//                        params.put("deviceName", mac.getDeviceName());
//                        List<Mac> macs = this.macService.selectByMap(params);
//                        map.put("srcRemoteDevice", new ArrayList<>());
//                        map.put("src_layer_2_device", new ArrayList<>());
//                        for(Mac obj : macs){
//                            if(obj.getRemoteDevice() != null && srcIpAddress.getDeviceName() != null){
//                                if(obj.getRemoteDevice().equals(srcIpAddress.getDeviceName())){
//                                    map.put("srcRemoteDevice", obj);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // 查询终点二层路径
//                if(routTableList.size() > 0){
//                    List<RouteTable> destToutTables = routTableList.stream().filter(item -> item.getStatus() == 1 || item.getStatus() == 3).collect(Collectors.toList());
//                    Map routTableMap = new HashMap();
//                    List list = new ArrayList();
//                    List destRemoteList = new ArrayList();
//                    for(RouteTable item : destToutTables){
//                        List<Mac> dest_layer_2_device = this.routTool.generetorSrcLayer_2_device(destArp.getMac(), item.getDeviceName());
//                        int number1 = dest_layer_2_device.size();
//                        boolean flag1 = true;
//                        if(number1 == 1){
//                            Mac mac = dest_layer_2_device.get(0);
//                            if(mac.getTag().equals("L")){
//                                flag1 = false;
//                            }
//                        }
//                        if(number1 > 1 && flag1){
//                            flag1 = false;
//                            routTableMap.put(item.getIp(), dest_layer_2_device);
//                            list.add(dest_layer_2_device);
//                        }
//                        if(flag1){
//                            // 查找 二层路径 路由起点设备的remote设备为有起点ip的mac地址记录且与起点设备相连的那台设备
//                            params.clear();
//                            params.put("mac", destArp.getMac());
//                            List<Mac> destRemoteDevice = this.macService.selectByMap(params);
//                            if(destRemoteDevice.size() > 0){
//                                params.clear();
//                                params.put("remoteDevice", destIpAddress.getDeviceName());
//                                List<Mac> mac = this.macService.selectByMap(params);
//                                for(Mac obj : mac){
//                                    if(obj.getRemoteDevice() != null && destIpAddress.getDeviceName() != null){
//                                        if(obj.getRemoteDevice().equals(destIpAddress.getDeviceName())){
//                                            destRemoteList.add(obj);
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    map.put("dest_layer_2_device", list);
//                    map.put("destRemoteDevice", destRemoteList);
//                }
//            }
//            return ResponseUtil.ok(map);
//    }

    @ApiOperation("路由查询")
    @RequestMapping("/layer_2_device")
    public Object routTable(String srcIp, Integer srcMask, String destIp, Integer destMask,
                            @RequestParam(value = "time", required = false)
                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date time,
                            @RequestParam(value = "type", defaultValue = "0") Integer type){
        if(StringUtil.isEmpty(srcIp)){
            return ResponseUtil.badArgument("起点Ip不能为空");
        }
        if(!IpUtil.verifyIp(srcIp)){
            return ResponseUtil.badArgument("起点Ip格式错误");
        }
        String srcMac = "";// 起点Mac
        String destMac = "";// 终点Mac
        Arp srcArp = null;
        if(type == 1){
            srcArp = this.queryArp(srcIp, time);
            if(srcArp == null){
                return ResponseUtil.badArgument("起点Ip不存在");
            }
            srcMac = srcArp.getMac();
        }
        if(StringUtil.isEmpty(destIp)){
            return ResponseUtil.badArgument("终点Ip不能为空");
        }
        if(!IpUtil.verifyIp(destIp)){
            return ResponseUtil.badArgument("终点Ip格式错误");
        }
        Arp destArp = null;
        if(type == 1){
            destArp = this.queryArp(destIp, time);
//            if(destArp == null){
//                return ResponseUtil.badArgument("终点Ip不存在");
//            }
            destMac = destArp.getMac();
        }
        // 查询路由
        Map map = new HashMap();
        List<IpAddress> srcIpAddresses = this.queryRoutDevice(srcIp, time);
        if(type == 0 && srcIpAddresses.size() <= 0){
            return ResponseUtil.badArgument("起点Ip不存在");
        }
        // 终点设备
        List<IpAddress> destIpAddress = this.queryRoutDevice(destIp, time);
        if(destIpAddress.size() <= 0){
            return ResponseUtil.badArgument("终点Ip不存在");
        }
//        map.put("destinationDevice", destIpAddress);

        Map params = new HashMap();
        this.routTableService.deleteObjByUserId(ShiroUserHolder.currentUser().getId());// 清除 routTable

        // 保存起点设备到路由表
        // 多起点
        User user = ShiroUserHolder.currentUser();
        for (IpAddress srcIpAddress : srcIpAddresses) {
            params.clear();
            params.put("ip", srcIpAddress.getIp());
            params.put("mask", srcIpAddress.getMask());
            params.put("deviceName", srcIpAddress.getDeviceName());
            params.put("interfaceName", srcIpAddress.getInterfaceName());
            params.put("mac", srcIpAddress.getMac());
            params.put("userId", user.getId());
            List<RouteTable> routTables = this.routTableService.selectObjByMap(params);
            RouteTable routTable = null;
            if(routTables.size() > 0){
                routTable = routTables.get(0);
            }else{
                routTable = new RouteTable();
            }
            String[] IGNORE_ISOLATOR_PROPERTIES = new String[]{"id"};
            BeanUtils.copyProperties(srcIpAddress, routTable, IGNORE_ISOLATOR_PROPERTIES);
            routTable.setUserId(user.getId());
            this.routTableService.save(routTable);
            // 路由查询
            this.routTool.generatorRout(srcIpAddress, destIp, time, user.getId());
        }
        params.clear();
        params.put("userId", user.getId());
        List<RouteTable> routTableList = this.routTableService.selectObjByMap(params);
        map.put("routTable", routTableList);

        boolean flag = false;
        for (RouteTable routeTable : routTableList) {
            for (IpAddress ipAddress : destIpAddress) {
                if(ipAddress.getDeviceUuid().equals(routeTable.getDeviceUuid())){
                    flag = true;
                    break;
                }
            }
        }

        if(flag){
            map.put("type", 0);
            map.put("msg", "从起点Ip " + srcIp +" 到终点Ip " + destIp +" 路由正常");
        }else{
            map.put("type", 1);
            map.put("msg", "从起点Ip " + srcIp +" 到终点Ip " + destIp +" 路由异常");
        }

        // 二层路径
//        if(type == 1){
//            // 多起点二层路径
//            for (IpAddress srcIpAddress : srcIpAddresses) {
//                List<Mac> src_layer_2_device = this.routTool.generetorSrcLayer_2_device(
//                        srcArp.getMac(), srcIpAddress.getDeviceName(), time);
//                int number = src_layer_2_device.size();
//                boolean flag = true;
//                if(number == 1){
//                    Mac mac = src_layer_2_device.get(0);
//                    if(mac.getTag().equals("L")){
//                        flag = false;
//                    }
//                }
//                if(number > 1 && flag){
//                    flag = false;
//                    map.put("src_layer_2_device", src_layer_2_device);
//                    map.put("srcRemoteDevice", new ArrayList<>());
//                }
//                if(flag){
//                    // 查找 二层路径 路由起点设备的remote设备为有起点ip的mac地址记录且与起点设备相连的那台设备
//                    params.clear();
//                    params.put("mac", srcArp.getMac());
//                    List<Mac> srcRemoteDevice = this.routTool.queryMac(params, time);
////                  List<Mac> srcRemoteDevice = this.macService.selectByMap(params);
//                    if(srcRemoteDevice.size() > 0){
//                        Mac mac = srcRemoteDevice.get(0);
//                        params.clear();
//                        params.put("deviceName", mac.getDeviceName());
//                        List<Mac> macs = this.routTool.queryMac(params, time);
////                      List<Mac> macs = this.macService.selectByMap(params);
//                        map.put("srcRemoteDevice", new ArrayList<>());
//                        map.put("src_layer_2_device", new ArrayList<>());
//                        for(Mac obj : macs){
//                            if(obj.getRemoteDevice() != null && srcIpAddress.getDeviceName() != null){
//                                if(obj.getRemoteDevice().equals(srcIpAddress.getDeviceName())){
//                                    map.put("srcRemoteDevice", obj);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // 查询终点二层路径
////                if(routTableList.size() > 0){
////                    List<RouteTable> destToutTables = routTableList.stream()
////                            .filter(item -> item.getStatus() == 1 || item.getStatus() == 3).collect(Collectors.toList());
////                    Map routTableMap = new HashMap();
////                    List list = new ArrayList();
////                    List destRemoteList = new ArrayList();
////                    for(RouteTable item : destToutTables){
////                        List<Mac> dest_layer_2_device = this.routTool.generetorSrcLayer_2_device(destArp.getMac(), item.getDeviceName(), time);
////                        int number1 = dest_layer_2_device.size();
////                        boolean flag1 = true;
////                        if(number1 == 1){
////                            Mac mac = dest_layer_2_device.get(0);
////                            if(mac.getTag().equals("L")){
////                                flag1 = false;
////                            }
////                        }
////                        if(number1 > 1 && flag1){
////                            flag1 = false;
////                            routTableMap.put(item.getIp(), dest_layer_2_device);
////                            list.add(dest_layer_2_device);
////                        }
////                        if(flag1){
////                            // 查找 二层路径 路由起点设备的remote设备为有起点ip的mac地址记录且与起点设备相连的那台设备
////                            params.clear();
////                            params.put("mac", destArp.getMac());
////                            List<Mac> destRemoteDevice = this.routTool.queryMac(params, time);
//////                        List<Mac> destRemoteDevice = this.macService.selectByMap(params);
////                            if(destRemoteDevice.size() > 0){
////                                params.clear();
////                                params.put("remoteDevice", destIpAddress.get(0).getDeviceName());
////                                List<Mac> mac = this.routTool.queryMac(params, time);
//////                            List<Mac> mac = this.macService.selectByMap(params);
////                                for(Mac obj : mac){
////                                    if(obj.getRemoteDevice() != null && destIpAddress.get(0).getDeviceName() != null){
////                                        if(obj.getRemoteDevice().equals(destIpAddress.get(0).getDeviceName())){
////                                            destRemoteList.add(obj);
////                                            break;
////                                        }
////                                    }
////                                }
////                            }
////                        }
////                    }
////                    map.put("dest_layer_2_device", list);
////                    map.put("destRemoteDevice", destRemoteList);
////                }
//
//            }
//        }

//                // 查询二层路径
//        if(type == 1){
//            List dest_layer_2_device = this.routTool.twoLayerPath2(srcMac, destMac);
//            map.put("dest_layer_2_device", dest_layer_2_device);
//        }
        return ResponseUtil.ok(map);
    }

    @ApiOperation("二层路径查询")
    @GetMapping("/Two-layer/equipment/path")
    public Object queryPath(@RequestParam(value = "srcIp") String srcIp,
                            @RequestParam(value = "srcGateway") String srcGateway,
                            @RequestParam(value = "destIp") String destIp,
                            @RequestParam(value = "destGateway") String destGateway,
                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date time){
        if(StringUtil.isEmpty(srcIp)){
            return ResponseUtil.badArgument("未输入起点Ip");
        }
        if(StringUtil.isEmpty(destIp)){
            return ResponseUtil.badArgument("未输入终点Ip");
        }
        if(!IpUtil.verifyIp(srcIp)){
            return ResponseUtil.badArgument("起点Ip格式错误");
        }
        if(!IpUtil.verifyIp(destIp)){
            return ResponseUtil.badArgument("终点Ip格式错误");
        }
        boolean queryFlag = false;
        if(StringUtil.isEmpty(srcGateway) && StringUtil.isEmpty(destGateway)){
            boolean flag = CompareUtils.isSameNetWork(srcIp, destIp);
            queryFlag = flag;
            if(!flag){
                return ResponseUtil.badArgument("未输入网关");
            }
        }else{
            if(StringUtil.isEmpty(srcGateway)){
                return ResponseUtil.badArgument("未输入起点网关Ip");
            }
            if(!StringUtil.isEmpty(srcGateway) && !IpUtil.verifyIp(srcGateway)){
                return ResponseUtil.badArgument("起点网关格式错误");
            }
            if(StringUtil.isEmpty(destGateway)){
                return ResponseUtil.badArgument("未输入终点网关Ip");
            }
            if(!StringUtil.isEmpty(destGateway) && !IpUtil.verifyIp(destGateway)){
                return ResponseUtil.badArgument("终点网关格式错误");
            }
        }

        List list = new ArrayList();
        Map map = new HashMap();
        map.put("one", new ArrayList<>());
        map.put("two", new ArrayList<>());
        map.put("three", new ArrayList<>());
        if(!queryFlag){
            Map first = new HashMap();
            // 第一段 起点ip至起点ip网关的二层路径查询
            String srcMac = "";
            String destMac = "";
            Arp srcArp = this.queryArp(srcIp);
            Arp destArp = this.queryArp(srcGateway);
            if(srcArp != null && destArp != null){
                srcMac = srcArp.getMac();
                destMac = destArp.getMac();
                if(time == null){
                    first = this.routTool.secondLayer(srcMac, destMac);
                }else{
                    first = this.routTool.secondLayerHistory(srcMac, destMac, time);
                }
                if(!first.isEmpty()){
                    map.put("one", first.get("path"));
                    Map param = new HashMap();
                    if(first.get("checkPath") != null && Boolean.valueOf(first.get("checkPath").toString())){
                        param.put("type", 0);
                        param.put("msg", "从起点ip " + srcIp +" 到网关 " + srcGateway +" 二层路径正常");
                    }else{
                        param.put("type", 1);
                        param.put("msg", "从起点ip " + srcIp + " 到网关" + srcGateway + " 二层路径异常");
                    }
                    list.add(param);
                }
            }else{
                map.put("one", new ArrayList<>());
                StringBuffer msg = new StringBuffer();
                msg.append("从起点Ip " + srcIp + " 到网关 "+ srcGateway +" 路径异常");
                if(srcArp == null){
                    msg.append(";起点不存在");
                }
                if(destArp == null){
                    msg.append(",").append("终点不存在");
                }
                Map param = new HashMap();
                param.put("type", 1);
                param.put("msg", msg);
                list.add(param);
            }

            // 第二段 起点网关到终点网关的路由查询
            if(first.get("destDevice") != null){
                // 终点设备
                List<IpAddress> destIpAddress = this.queryRoutDevice(destIp, time);
                List<RouteTable> two = this.routTool.queryRoutePath(srcGateway, destGateway, time, (Mac) first.get("destDevice"));
                if(two.size() > 0){
                    map.put("two", two);
                    Map param = new HashMap();
                    boolean flag = false;
                    for (RouteTable routeTable : two) {
//                        if(routeTable.getIp().equals(destGateway)){
//                            flag = true;
//                            break;
//                        }
                        for (IpAddress ipAddress : destIpAddress) {
                            if(ipAddress.getDeviceUuid().equals(routeTable.getDeviceUuid())){
                                flag = true;
                                break;
                            }
                        }
                    }
                    if(flag){
                        param.put("type", 0);
                        param.put("msg", "从起点网关 " + srcGateway +" 到终点网关 " + destGateway +" 路由正常");
                    }else{
                        param.put("type", 1);
                        param.put("msg", "从起点网关 " + srcGateway +" 到终点网关 " + destGateway +" 路由异常");
                    }
                    list.add(param);
                }
            }else{
                StringBuffer msg = new StringBuffer();
                msg.append("路由异常;起点网关 " + srcGateway + " 不存在");
                Map param = new HashMap();
                param.put("type", 1);
                param.put("msg", msg);
                list.add(param);
            }

            // 第三段 终点网关到终点ip的二层查询
            User user = ShiroUserHolder.currentUser();
            Map params = new HashMap();
            params.clear();
            params.put("userId", user.getId());
            params.put("status", 3);
            List<RouteTable> routTableList = this.routTableService.selectObjByMap(params);
            destArp = this.queryArp(destIp);
            if(routTableList.size() > 0 && destArp != null){
                destMac = destArp.getMac();
                List<Mac> threeList = new ArrayList();
                List<Mac> destDevices = new ArrayList();
                for(RouteTable routeTable : routTableList){
                    if(time == null){
                        Map result = this.routTool.secondLayers(routeTable.getDeviceUuid(), destMac);
                        if(result.get("path") != null){
                            // 正确姿势
                            List<Mac> path = JSONObject.parseObject(JSONObject.toJSONString(result.get("path")),  new TypeReference<List<Mac>>(){});
//                            List<Mac> path = JSONObject.parseObject(JSONObject.toJSONString(result.get("path")), List.class);
                            threeList.addAll(path);
                        }
                        if(result.get("destDevice") != null){
                            destDevices.add(Json.fromJson(Mac.class, JSONObject.toJSONString(result.get("destDevice"))));
                        }
                    }else{
                        Map result = this.routTool.secondLayersHistory(routeTable.getDeviceUuid(), destMac, time);
                        if(result.get("path") != null){
                            List<Mac> path = JSONObject.parseObject(JSONObject.toJSONString(result.get("path")),  new TypeReference<List<Mac>>(){});
                            threeList.addAll(path);
                        }
                        if(result.get("destDevice") != null){
                            destDevices.add(Json.fromJson(Mac.class, JSONObject.toJSONString(result.get("destDevice"))));
                        }
                    }
                }
                if(threeList.size() > 0){
                    map.put("three", threeList);
                    Map param = new HashMap();
                    boolean flag = false;
                    for (Mac mac : threeList) {
//                        if(destDevices.contains(mac)){
//                            flag = true;
//                            break;
//                        }
                        for (Mac destDevice : destDevices) {
                            if(destDevice.getUuid().equals(mac.getRemoteUuid())){
                                flag = true;
                                break;
                            }
                        }
                    }
                    if(flag){
                        param.put("type", 0);
                        param.put("msg", "从终点网关 " + destGateway +" 到终点Ip " + destIp +" 路径正常");
                    }else{
                        param.put("type", 1);
                        param.put("msg", "从终点网关 " + destGateway +" 到终点网关 " + destIp +" 路径异常");
                    }
                    list.add(param);
                }
            }else{
                srcArp = this.queryArp(destGateway);
                if(srcArp != null && destArp != null){
                    srcMac = srcArp.getMac();
                    destMac = destArp.getMac();
                    Map third = new HashMap();
                    if(time == null){
                        third = this.routTool.secondLayer(srcMac, destMac);
                    }else{
                        third = this.routTool.secondLayerHistory(srcMac, destMac, time);
                    }
                    if(!third.isEmpty()){
                        map.put("three", third.get("path"));
                        Map param = new HashMap();
                        if(Boolean.valueOf(third.get("checkPath").toString())){
                            param.put("type", 0);
                            param.put("msg", "从终点网关 " + destGateway + " 到终点ip " + destIp + " 二层路径正常");
                        }else{
                            param.put("type", 1);
                            param.put("msg", "从终点网关 " + destGateway + " 到终点ip " + destIp + " 二层路径异常");
                        }
                        list.add(param);
                    }
                }else{
                    StringBuffer msg = new StringBuffer();
                    msg.append("从终点网关 "+ destGateway + " 到终点ip " + destIp + " 路径异常");
                    if(srcArp == null){
                        msg.append(";终点网关不存在");
                    }
                    if(destArp == null){
                        msg.append(",").append("终点不存在");
                    }
                    Map param = new HashMap();
                    param.put("type", 1);
                    param.put("msg", msg);
                    list.add(param);
                }
            }
        }else{
            Arp srcArp = this.queryArp(srcIp);
            Arp destArp = this.queryArp(destIp);
            String srcMac = "";
            String destMac = "";
            if(srcArp != null && destArp != null){
                srcMac = srcArp.getMac();
                destMac = destArp.getMac();
                Map first = new HashMap();
                if(time == null){
                    first = this.routTool.secondLayer(srcMac, destMac);
                }else{
                    first = this.routTool.secondLayerHistory(srcMac, destMac, time);
                }
                if(!first.isEmpty()){
                    map.put("one", first.get("path"));
                    Map param = new HashMap();
                    if(Boolean.valueOf(first.get("checkPath").toString())){
                        param.put("0", "从起点ip " + srcIp + " 到起点网关 " + srcGateway + " 二层路径正常");
                    }else{
                        param.put("1", "从起点ip " + srcIp + " 到起点网关 " + srcGateway + " 二层路径异常");
                    }
                    map.put("oneMsg", param);
                }
            }else{
                StringBuffer msg = new StringBuffer();
                msg.append("从起点Ip "+ srcIp + " 到起点网关 " + srcGateway +" 路径异常");
                if(srcArp == null){
                    msg.append(";起点不存在");
                }
                if(destArp == null){
                    msg.append(",").append("终点不存在");
                }
                Map param = new HashMap();
                param.put("type", 1);
                param.put("msg", msg);
                list.add(param);
            }
        }
        map.put("msg", list);
        return ResponseUtil.ok(map);
    }

    public Arp queryArp(String ip, Date time){
        Arp arp = null;
        if(time == null){
            arp = this.arpService.selectObjByIp(IpUtil.ipConvertDec(ip));
        }else {
            Map params = new HashMap();
            params.clear();
            params.put("ip", IpUtil.ipConvertDec(ip));
            params.put("time", time);
            List<Arp> srcArps = this.arpHistoryService.selectObjByMap(params);
            if (srcArps.size() > 0) {
                arp = srcArps.get(0);
            }
        }
        return arp;
    }

    public Arp queryArp(String ip){
        Arp arp = this.arpService.selectObjByIp(IpUtil.ipConvertDec(ip));
        return arp;
    }

    public List<IpAddress> queryRoutDevice(String Ip, Date time){
        List<IpAddress> list = new ArrayList<IpAddress>();
        Map params = new HashMap();
        params.put("ip", IpUtil.ipConvertDec(Ip));
        List<IpAddress> ipAddresses = this.ipAddressServie.selectObjByMap(params);
        if(ipAddresses.size() > 0){
            list.add(ipAddresses.get(0));
        }else{
            // 获取起点ip网络地址和广播地址
            Map originMap =  null;
            List<Subnet> subnets = this.zabbixSubnetService.selectSubnetByParentId(null);
            if(subnets.size() > 0){
                if(Ip != null){
                    if(Ip.equals("0.0.0.0")){
                    }
                    if(!IpUtil.verifyIp(Ip)){
                    }
                    // 判断ip地址是否属于子网
                    for(Subnet subnet : subnets){
                        Subnet sub = this.subnetTool.verifySubnetByIp(subnet, Ip);
                        if(sub != null){
                            String mask = IpUtil.bitMaskConvertMask(sub.getMask());
                            originMap = IpUtil.getNetworkIpDec(sub.getIp(), mask);
                            break;
                        }
                    }
                }
            }
            if(originMap == null || originMap.isEmpty()){
                return new ArrayList<>();
            }
            if(time == null){
                list = this.ipAddressServie.querySrcDevice(originMap);
            }else{
                originMap.put("time", time);
                list = this.ipAddressHistoryServie.querySrcDevice(originMap);
            }
        }
        return list;
    }

    @GetMapping("/query/device/{ip}")
    public Object queryDevice(@PathVariable(value = "ip") String ip){
        if(StringUtils.isNotBlank(ip)){
            Map result = new HashMap();
            Map params = new HashMap();
            params.put("ip", IpUtil.ipConvertDec(ip));
            List<IpAddress> ipAddresses = this.ipAddressServie.selectObjByMap(params);
            if(ipAddresses.size() > 0){
                Set<String> set = ipAddresses.stream().map(e -> {
                    return e.getDeviceUuid();
                }).collect(Collectors.toSet());
                result.put("device", set);
            }
            params.clear();
            params.put("ip", ip);
            List<Terminal> terminals = this.terminalService.selectObjByMap(params);
            if(terminals.size() > 0){
                Set<String> set = terminals.stream().map(e -> {
                    return e.getMac();
                }).collect(Collectors.toSet());
                result.put("terminal", set);
            }
            return ResponseUtil.ok(result);
        }
       return ResponseUtil.badArgument();
    }

    @ApiOperation("设备 流量")
    @GetMapping(value = {"/terminal/flux"})
    public Object getObjByUuid(@RequestParam(value = "id", required = false) String id) {
        if(Strings.isNotBlank(id)){
            Map params = new HashMap();
            Terminal terminal = this.terminalService.selectObjById(Long.parseLong(id));
            if(terminal == null){
                return ResponseUtil.badArgument();
            }
            // 流量
            Map flux = new HashMap();
            NetworkElement ne = this.networkElementService.selectObjByUuid(terminal.getUuid());
            if(ne != null){
                // 根据端口获取流量
                // ifreceived
                params.clear();
                params.put("ip", ne.getIp());
                params.put("filterValue", terminal.getInterfaceName());
                params.put("tag", "ifreceived");
                params.put("available", 1);
                params.put("filterTag", "ifname");// 根据名字查询tag
                List<Item> items = this.itemService.selectTagByMap(params);
                if(items.size() > 0){
                    for (Item item : items) {
                        String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
                        flux.put("received", lastvalue);
                        break;
                    }
                } else{
                    flux.put("received", "0");
                }
                // sent
                params.clear();
                params.put("ip", ne.getIp());
                params.put("filterValue", terminal.getInterfaceName());
                params.put("tag", "ifsent");
                params.put("available", 1);
                params.put("filterTag", "ifname");// 根据名字查询tag
                List<Item> sentItem = this.itemService.selectTagByMap(params);
                if(sentItem.size() > 0){
                    for (Item item : sentItem) {
                        String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
                        flux.put("sent", lastvalue);
                        break;
                    }
                }else{
                    flux.put("sent", "0");
                }
            }
            // 查询设备信息
            Map deviceInfo = this.rsmsDeviceUtils.getDeviceInfo(terminal);
            flux.put("deviceInfo", deviceInfo);
            return ResponseUtil.ok(flux);
        }
        return ResponseUtil.ok();
    }

}
