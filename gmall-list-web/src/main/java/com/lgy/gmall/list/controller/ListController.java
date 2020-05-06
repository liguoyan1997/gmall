package com.lgy.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.it.bean.*;
import com.it.service.ListService;
import com.it.service.ManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManagerService managerService;

    @RequestMapping("list.html")
//    @ResponseBody
    public String getList(SkuLsParams skuLsParams, Model model){
        /*设置每页的数据条数*/
        skuLsParams.setPageSize(2);
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        /* 获取sku属性值列表*/
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        /*从es中查询出属性值*/
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = managerService.getAttrList(attrValueIdList);
        /*制作面包屑*/
        List<BaseAttrValue> baseAttrValueList = new LinkedList<>();
        /* // 已选的属性值列表\*/
        String urlParam = makeUrlParam(skuLsParams);
        /*迭代*/
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            /*平台属性*/
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            /*平台属性值遍历*/
            for(BaseAttrValue baseAttrValue : attrValueList){
                if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
                    for(String valueId:skuLsParams.getValueId()){
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            /*定义面包屑*/
                            String name = baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName();
                            /*将平台的属性名称改为面包屑*/
                            baseAttrValue.setValueName(name);
                            /*面包屑移除*/
                            String urlParam1 = makeUrlParam(skuLsParams, valueId);
                            /*面包屑移除后重新制作url参数*/
                            baseAttrValue.setUrlParam(urlParam1);
                            baseAttrValueList.add(baseAttrValue);
                        }
                    }
                }
            }
        }
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        model.addAttribute("totalPages",skuLsResult.getTotalPages());
        model.addAttribute("urlParam",urlParam);
        /*面包屑*/
        model.addAttribute("Keyword",skuLsParams.getKeyword());
        model.addAttribute("baseAttrValueList",baseAttrValueList);
        model.addAttribute("attrList",attrList);
        model.addAttribute("skuLsInfoList",skuLsInfoList);
        return "list";
//        return JSON.toJSONString(skuLsResult);
    }

    private String makeUrlParam(SkuLsParams skuLsParams,String... valueIds) {
        String urlParam = "";
        /*拼接keyWord*/
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="Keyword="+skuLsParams.getKeyword();
        }
        /*判断三级分类Id*/
        if(skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if(urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        /*拼接属性值*/
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){

            for(String valueId:skuLsParams.getValueId()){
                /*移除面包屑*/
                if(valueIds!=null && valueIds.length>0){
                    String value = valueIds[0];
                    if (value.equals(valueId)){
                        continue;
                    }
                }
                if(urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return urlParam;
    }
}
