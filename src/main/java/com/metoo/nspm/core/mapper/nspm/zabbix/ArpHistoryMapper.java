package com.metoo.nspm.core.mapper.nspm.zabbix;

import com.metoo.nspm.dto.ArpDTO;
import com.metoo.nspm.entity.nspm.Arp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArpHistoryMapper {

    Arp selectObjByIp(String ip);

    List<Arp> selectObjConditionQuery(ArpDTO dto);

    List<Arp> selectObjByMap(Map params);

    List<Arp> selectDistinctObjByMap(Map params);

    int deleteObjByMap(Map params);

    int batchDelete(List<Long> ids);

    void copyArpTemp();
}
