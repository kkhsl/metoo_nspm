package com.metoo.nspm.core.service.zabbix;

import java.util.Date;

public interface IGatherService {

    /**
     * 采集Arp
     * @param time
     */
    void gatherArpItem(Date time);

    /**
     * 采集Mac
     * @param time
     */
    void gatherMacItem(Date time);


    /**
     *  Stream 批量处理
     * @param time
     * @throws InterruptedException
     */
    void gatherMacBatch(Date time) throws InterruptedException;


    void gatherMacBatchStream(Date time);

    /**
     * 使用线程池
     * @param time
     * @throws InterruptedException
     */
    void gatherMacThreadPool(Date time);

    void gatherMacThreadPool2(Date time) throws InterruptedException;

    void gatherMacThreadPool3(Date time);

    void gatherMacThreadPool4(Date time);

    /**
     * 采集路由
     * @param time
     */
    void gatherRouteItem(Date time);

    /**
     * 采集Ip地址
     * @param time
     */
    void gatherIpaddressItem(Date time);

    /**
     * 采集告警信息
     * @param time
     */
    void gatherProblemItem(Date time);

    /**
     * 采集主机接口（主机状态）
     */
//    void gatherInterfaceitem(Date time);

    void testTransactional();

    /**
     * 采集主机状态
     */
//    void gatherSnmpAvailable();

    void gatherSpanningTreeProtocol(Date time);

    // 采集主机的snmp状态
    void gatherHostSnmp(Date time);

}
