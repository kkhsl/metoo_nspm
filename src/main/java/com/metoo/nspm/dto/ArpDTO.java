package com.metoo.nspm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metoo.nspm.dto.page.PageDto;
import com.metoo.nspm.entity.nspm.Arp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@ApiModel("ARP")
@Data
@Accessors
@AllArgsConstructor
@NoArgsConstructor
public class ArpDTO extends PageDto<Arp> {

    @ApiModelProperty("设备名称")
    private String deviceIp;
    @ApiModelProperty("设备名称")
    private String deviceName;
    @ApiModelProperty("设备类型")
    private String deviceType;
    @ApiModelProperty("接口名称")
    private String interfaceName;
    @ApiModelProperty("接口序号")
    private String index;
    @ApiModelProperty("ip地址")
    private String ip;
    @ApiModelProperty("ip地址")
    private String ipAddress;
    @ApiModelProperty("MAC地址")
    private String mac;
    @ApiModelProperty("厂商")
    private String macVendor;
    @ApiModelProperty("标记")
    private String tag;
    @ApiModelProperty("对端设备名称")
    private String remoteDevice;
    @ApiModelProperty("对端接口名称")
    private String remoteInterface;
    @ApiModelProperty("对端设备类型")
    private String remoteDeviceType;
    @ApiModelProperty("对端设备Ip")
    private String remoteIp;
    @ApiModelProperty("对端Ip")
    private String remoteDeviceIp;
    @ApiModelProperty("对端Uuid")
    private String remoteUuid;
    private String segment;
    private String mask;
    private String uuid;
    private String filter;
    private String macFilter;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm",timezone = "GMT+8")
    private Date time;
}
