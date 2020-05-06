package com.it.gmall.manager.mapper;

import com.it.bean.BaseAttrInfo;
import com.it.bean.BaseCatalog3;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /*当传多个数值时需要使用@Param*/
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}
