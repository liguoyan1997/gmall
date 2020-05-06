package com.it.gmall.manager.mapper;

import com.it.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;
import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    /**
     * 查询spu属性属性值得集合
     * SpuSaleAttrMapper.xml
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    /**
     * 根据spuId查询属性值集合根绝skuId定位属性值
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String skuId, String spuId);
}
