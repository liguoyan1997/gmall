package com.it.service;

import com.it.bean.*;

import javax.persistence.Transient;
import java.util.List;

public interface ManagerService {

    /*获取所有一级分类数据*/
    List<BaseCatalog1> getCatalog1List();

    /*根据一级分类查询二级分类数据*/
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /*根据二级分类查询三级分类数据*/
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /*平台属性*/
    List<BaseAttrInfo> getAtrList(String catalog3Id);

    void saveAtrrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    /*通过三级分类查询商品*/
    List<SpuInfo> getSpuList(String catalog3Id);
    /*通过三级分类查询商品*/
    List<SpuInfo> getSpuList(SpuInfo spuInfo);

    /*获取所有销售属性数据*/
    List<BaseSaleAttr> getBaseSaleAttrList();

    List<SpuImage> getSpuImageList(SpuImage spuImage);

    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    @Transient
    void saveSpuInfo(SpuInfo spuInfo);

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(String skuId);

    List<SkuImage> getSkuImageBySkuId(String skuId);

    /**
     * 查询销售属性值集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询销售属性值集合
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String skuId);

    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
