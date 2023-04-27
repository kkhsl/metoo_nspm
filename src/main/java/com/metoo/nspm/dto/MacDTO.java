package com.metoo.nspm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metoo.nspm.dto.page.PageDto;
import com.metoo.nspm.entity.nspm.Mac;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@Accessors
@AllArgsConstructor
@NoArgsConstructor
public class MacDTO extends PageDto<Mac> {

    @ApiModelProperty("设备名称")
    private String deviceIp;
    @ApiModelProperty("设备名称")
    private String deviceName;
    @ApiModelProperty("设备名称")
    private String deviceType;
    @ApiModelProperty("接口名称")
    private String interfaceName;
    @ApiModelProperty("Mac地址")
    private String mac;
    @ApiModelProperty("Mac标记")
    private String tag;
    @ApiModelProperty("Mac索引")
    private String index;
    @ApiModelProperty("索引")
    private String uuid;
    @ApiModelProperty("接口索引")
    private String interfaceIndex;
    @ApiModelProperty("接口ip")
    private String ip;
    @ApiModelProperty("接口ip")
    private String ipAddress;
    @ApiModelProperty("对端设备名称")
    private String remoteDevice;
    @ApiModelProperty("对端接口名称")
    private String remoteInterface;
    @ApiModelProperty("对端设备Ip")
    private String remoteDeviceIp;
    @ApiModelProperty("对端设备类型")
    private String remoteDeviceType;
    private String remoteUuid;
    private String vendor;
    private String vlan;
    private String filter;
    private String macFilter;

    private String unMac;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date time;
}
