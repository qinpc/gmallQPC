package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;
import java.util.Map;

public interface ManageService {

    //查询一级分类
    List<BaseCatalog1> getCatalog1();



    //查询二级分类 根据一级分类ID
    List<BaseCatalog2> getCatalog2(String catalog1Id);


    //查询三级分类 根据二级分类ID
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类查询平台属性
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    //根据平台属性id 查询平台属性的详情 顺便把该属性的属性值列表也取到
    BaseAttrInfo getBaseAttrInfo(String attrId);


    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);


    //删除平台属性


    //根据三级分类查询spu列表
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存SPU信息
    void saveSpuInfo(SpuInfo spuInfo);


    //根据spuid查询销售属性
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    // 根据spuId获取spuImage中的所有图片列表
    List<SpuImage> getSpuImageList(String spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    //根据商品id查找商品信息
    SkuInfo getSkuInfo(String skuId);

    //查询sku对应SPU的销售属性
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String skuId ,String spuId);

    //根据spuid查询已有的sku涉及的销售属性清单
    public Map getSkuValueIdsMap(String spuId);
}
