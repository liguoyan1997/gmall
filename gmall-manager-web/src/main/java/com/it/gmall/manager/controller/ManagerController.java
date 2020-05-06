package com.it.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.*;
import com.it.service.ManagerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin /*跨域请求*/
public class ManagerController {

    @Reference
    private ManagerService managerService;

    @RequestMapping("/getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return managerService.getCatalog1List();
    }

    @RequestMapping("/getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return managerService.getCatalog2(catalog1Id);
    }

    @RequestMapping("/getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return managerService.getCatalog3(catalog2Id);
    }

    @RequestMapping("/attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return managerService.getAtrList(catalog3Id);
    }

    /*将json数据转化为对象*/
    @RequestMapping("/saveAttrInfo")
    public void saveAtrrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        managerService.saveAtrrInfo(baseAttrInfo);
    }

    @RequestMapping("/getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        return managerService.getAttrValueList(attrId);
    }

    @RequestMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        return managerService.getBaseSaleAttrList();
    }

}
