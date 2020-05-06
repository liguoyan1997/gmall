package com.it.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.SpuInfo;
import com.it.service.ManagerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin /*跨域*/
public class SpuManageController {

    /*调用dubbo的service接口*/
    @Reference
    private ManagerService managerService;

    @RequestMapping("spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return managerService.getSpuList(spuInfo);
    }

    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        managerService.saveSpuInfo(spuInfo);
    }
}
