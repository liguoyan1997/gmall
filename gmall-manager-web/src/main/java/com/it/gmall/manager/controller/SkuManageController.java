package com.it.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.SkuInfo;
import com.it.bean.SkuLsInfo;
import com.it.bean.SpuImage;
import com.it.bean.SpuSaleAttr;
import com.it.service.ListService;
import com.it.service.ManagerService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManagerService managerService;

    @Reference
    private ListService listService;

    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage){
        return managerService.getSpuImageList(spuImage);
    }

    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        return managerService.getSpuSaleAttrList(spuId);
    }

    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        managerService.saveSkuInfo(skuInfo);
    }

    /**
     * 上架 创建索引库索引表
     * @param skuId
     */
    @RequestMapping("onSale")
    public void onSale(String skuId){
        /*获取商品数据*/
        SkuInfo skuInfo = managerService.getSkuInfo(skuId);
        /*定义es空表*/
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        /*属性拷贝*/
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        listService.saveSkuInfo(skuLsInfo);
    }
}
