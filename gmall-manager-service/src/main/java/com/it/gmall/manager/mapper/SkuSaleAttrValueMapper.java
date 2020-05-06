package com.it.gmall.manager.mapper;

import com.it.bean.SkuAttrValue;
import com.it.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    /**
     * 根据spuId查询数据
     * @param skuId
     * @return
     */
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String skuId);
}
