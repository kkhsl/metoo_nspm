package com.metoo.nspm.core.service.nspm;

import com.github.pagehelper.Page;
import com.metoo.nspm.dto.DeviceTypeDTO;
import com.metoo.nspm.entity.nspm.DeviceType;
import com.metoo.nspm.vo.DeviceTypeVO;

import java.util.List;
import java.util.Map;

public interface IDeviceTypeService {

    DeviceType selectObjById(Long id);

    DeviceType selectObjByName(String name);

    Page<DeviceType> selectConditionQuery(DeviceTypeDTO dto);

    List<DeviceType> selectObjByMap(Map params);

    List<DeviceType> selectCountByLeftJoin();

    List<DeviceType> selectCountByJoin();

    List<DeviceType> selectDeviceTypeAndNeByJoin();

    List<DeviceTypeVO> statistics();

   int save(DeviceType instance);

    int update(DeviceType instance);

    int delete(Long id );

    int batcheDel(Long[] ids);
}

