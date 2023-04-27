package com.metoo.nspm.core.websocket.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.metoo.nspm.core.config.websocket.demo.NoticeWebsocketResp;
import com.metoo.nspm.core.service.api.zabbix.ZabbixService;
import com.metoo.nspm.core.service.zabbix.ItemService;
import com.metoo.nspm.entity.zabbix.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RequestMapping("/websocket/api/zabbix")
@RestController
public class ZabbixManagerApi {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ZabbixService zabbixService;
    @Autowired
    private RedisResponseUtils redisResponseUtils;

    public static void main(String[] args){
        Scanner sc=new Scanner(System.in);
        String str=sc.next();
        for(char c:str.toCharArray()){
            System.out.printf(" %c",c);
        }
    }
    // 根据ip 名称 查询端口流量
    @RequestMapping("/lastvalue")
    public Object history(@RequestParam(value = "requestParams") String requestParams) {
        List result = new ArrayList();
        String sessionId = "";
        NoticeWebsocketResp rep = new NoticeWebsocketResp();
        if(requestParams != null && !requestParams.equals("")){
            Map param = JSONObject.parseObject(String.valueOf(requestParams), Map.class);
            sessionId = (String) param.get("sessionId");
            List<String> list = JSONObject.parseObject(String.valueOf(param.get("params")), List.class);
            if(list.size() > 0){
                Map map = new HashMap();
                for (String str : list){
                    String[] data = str.split("&");
                    if(data.length >= 2) {
                        Map params = new HashMap();
                        params.put("ip", data[0]);
                        // 采集ifbasic,然后查询端口对应的历史流量
                        params.put("tag", "ifreceived");
                        params.put("available", 1);
                        params.put("filterValue", data[1]);
                        List<Item> items = this.itemService.selectTagByMap(params);
                        Map ele = new HashMap();
                        if(items.size() > 0){
                            for (Item item : items) {
                                String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
                                ele.put("received", lastvalue);
                                break;
                            }
                        } else{
                            ele.put("received", "0");
                        }
                        params.clear();
                        params.put("ip", data[0]);
                        // 采集ifbasic,然后查询端口对应的历史流量
                        params.put("tag", "ifsent");
                        params.put("available", 1);
                        params.put("filterValue", data[1]);
                        List<Item> ifsents = this.itemService.selectTagByMap(params);
                        if(ifsents.size() > 0){
                            for (Item item : ifsents) {
                                String lastvalue = this.zabbixService.getItemLastvalueByItemId(item.getItemid().intValue());
                                ele.put("sent", lastvalue);
                                break;
                            }
                        }else{
                            ele.put("sent", "0");
                        }
                        map.put(str, ele);
                    }
                }
                rep.setNoticeType("3");
                rep.setNoticeInfo(map);

                this.redisResponseUtils.syncStrRedis(sessionId, JSON.toJSONString(result), 3);
//                this.redisResponseUtils.syncRedis(sessionId, result, 3);
                return rep;
            }
        }
        rep.setNoticeType("3");
        return rep;
    }




}
