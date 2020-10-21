package com.it.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 平台属性值表
 */
@Data
public class BaseAttrValue implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)  //获取表主键自增
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    /*声明一个变量*/
    @Transient
    private String urlParam;
}
