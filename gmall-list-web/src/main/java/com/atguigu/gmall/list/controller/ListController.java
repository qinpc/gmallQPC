package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;


    @GetMapping("list.html")
    public String getList(SkuLsParams skuLsParams, Model model) {
        //System.out.println(skuLsParams);
        //根据参数返回sku列表
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        model.addAttribute("skuLsInfoList", skuLsResult.getSkuLsInfoList());
        //System.out.println(skuLsResult.getSkuLsInfoList());

        //从结果中取出平台属性值列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList", attrList);
        //System.out.println(attrList);


        String urlParam = makeUrlParam(skuLsParams);

        //已选择的平台属性值信息列表
        List<BaseAttrValue> baseAttrValueList= new ArrayList<>();


        // 已选的属性值列表

        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        //选中的属性值 和 查询结果的属性值
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            //2 添加到已选择列表
                            //baseAttrValue 增加已经面包屑的url
                            String makeUrlParam=makeUrlParam(skuLsParams,valueId);
                            baseAttrValue.setParamUrl(makeUrlParam);
                            baseAttrValueList.add(baseAttrValue);
                        }
                    }
                }
            }
        }
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("baseAttrValueList",baseAttrValueList);


        model.addAttribute("keyword",skuLsParams.getKeyword());

        model.addAttribute("pageNo",skuLsParams.getPageNo());

        model.addAttribute("totalPages",skuLsResult.getTotalPages());
        return "list";
    }


    //拼接条件方法
    public String makeUrlParam(SkuLsParams skuLsParam,String... excludeValueIds) {
        String urlParam = "";

        if (skuLsParam.getKeyword() != null) {
            urlParam += "keyword=" + skuLsParam.getKeyword();
        }
        if (skuLsParam.getCatalog3Id() != null) {
            if (urlParam.length() > 0) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParam.getCatalog3Id();
        }
        // 构造属性参数

        if (skuLsParam.getValueId() != null && skuLsParam.getValueId().length > 0) {

            for (int i = 0; i < skuLsParam.getValueId().length; i++) {
                String valueId = skuLsParam.getValueId()[i];
                if(excludeValueIds!=null&&excludeValueIds.length>0)
                    // 跳出代码，后面的参数则不会继续追加【后续代码不会执行】
                    if(excludeValueIds[0].equals(valueId)){
                        continue;
                    }

                if (urlParam.length() > 0) {
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;

            }

        }


        return urlParam;
    }

}
