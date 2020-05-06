package com.it.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.it.bean.SkuImage;
import com.it.bean.SkuInfo;
import com.it.bean.SkuSaleAttrValue;
import com.it.bean.SpuSaleAttr;
import com.it.gmall.config.LoginRequire;
import com.it.service.ListService;
import com.it.service.ManagerService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManagerService managerService;

    @Reference
    private ListService listService;

    @LoginRequire
    @RequestMapping("{skuId}.html")
    public String Item(@PathVariable(value = "skuId") String skuId, HttpServletRequest request){
        /*通过skuid获取数据*/
        SkuInfo skuInfo = managerService.getSkuInfo(skuId);
        /*显示图片列表*/
        List<SkuImage> skuImageList = managerService.getSkuImageBySkuId(skuId);
        /*显示属性集合*/
        List<SpuSaleAttr> saleAttrList = managerService.getSpuSaleAttrListCheckBySku(skuInfo);
        /*获取销售属性值Id*/
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = managerService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        //把列表变换成 valueid1|valueid2|valueid3 ：skuId  的 哈希表 用于在页面中定位查询
        String valueIdsKey="";

        Map<String,String> valuesSkuMap=new HashMap<>();

        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if(valueIdsKey.length()!=0){
                valueIdsKey= valueIdsKey+"|";
            }
            valueIdsKey+=skuSaleAttrValue.getSaleAttrValueId();

            if((i+1)== skuSaleAttrValueListBySpu.size()||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())  ){

                valuesSkuMap.put(valueIdsKey,skuSaleAttrValue.getSkuId());
                valueIdsKey="";
            }

        }
        //把map变成json串
        String valuesSkuJson = JSON.toJSONString(valuesSkuMap);

        System.out.println(valuesSkuMap);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuImageList",skuImageList);
        request.setAttribute("saleAttrList",saleAttrList);
        listService.incrHotScore(skuId);
        return "item";
    }
}
