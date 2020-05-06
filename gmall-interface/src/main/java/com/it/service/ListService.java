package com.it.service;

import com.it.bean.SkuLsInfo;
import com.it.bean.SkuLsParams;
import com.it.bean.SkuLsResult;

public interface ListService {
    /*保存数据到es中*/
    public void saveSkuInfo(SkuLsInfo skuLsInfo);

    /*检索数据*/
    SkuLsResult search(SkuLsParams skuLsParams);

    /*记录每个商品被访问的次数*/
    public void incrHotScore(String skuId);
}
