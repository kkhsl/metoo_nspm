package com.metoo.nspm.core.mapper.zabbix;

import com.metoo.nspm.entity.zabbix.Item;
import com.metoo.nspm.entity.zabbix.ItemTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {

    List<Item> query(String ip);

    List<Item> arpTable(String ip);

    List<Item> gatherItemByTag(Map params);

    List<Item> gatherItemByTagAndClock(Map params);

    List<Item> gatherItemByTagAndRtdata(Map params);

    List<Item> gatherItemByTagAndRtdata2(Map params);

    List<Item> gatherItemByTagAndIndex(Map params);

    List<Item> selectTagByMap(Map params);// 根据条件查询itemtag

    List<Item> selectItemTagByMap(Map params);

    List<Item> interfaceTable(Map params);

    List<Item> selectNameObjByIndex(String index);

    List<Item> gatherItemBySTP(Map params);

    // 查询端口状态
    List<Item> selectInterfaceStatus(Map params);

    List<Item> selectItemTagByIpAndObjToPort(Map params);

    //
    List<Item> selectItemTagByIpAndObj(Map params);


}
