package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    @Autowired
    RedisUtil redisUtil;

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";


    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        Index.Builder indexBuilder = new Index.Builder(skuLsInfo);
        indexBuilder.index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId());
        Index index = indexBuilder.build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //商品名称查询搜索
        if (skuLsParams.getKeyword() != null) {
            boolQueryBuilder.must(new MatchQueryBuilder("skuName", skuLsParams.getKeyword()));
            //高亮
            searchSourceBuilder.highlight(new HighlightBuilder().field("skuName").preTags("<span style='color:red'>").postTags("</span>"));

        }
        //三级分类过滤
        if (skuLsParams.getCatalog3Id() != null) {
            boolQueryBuilder.filter(new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id()));
        }
        //平台属性过滤
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            String[] valueIds = skuLsParams.getValueId();
            for (int i = 0; i < valueIds.length; i++) {
                String valueid = valueIds[i];
                boolQueryBuilder.filter(new TermQueryBuilder("skuAttrValueList.valueId", valueid));
            }
        }
        //价格
        // boolQueryBuilder.filter(new RangeQueryBuilder("price").gte("3200"));

        searchSourceBuilder.query(boolQueryBuilder);
        // 起始行
        searchSourceBuilder.from((skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize());
        // 页行数
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //  聚合
        TermsBuilder aggsBuilder = AggregationBuilders.terms("groupby_value_id").field("skuAttrValueList.valueId").size(1000);
        searchSourceBuilder.aggregation(aggsBuilder);
        //  排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);


        String resl = searchSourceBuilder.toString();
        System.out.println(resl);
        Search.Builder searchBuilder = new Search.Builder(resl);
        Search seach = searchBuilder.addIndex(ES_INDEX).addType(ES_TYPE).build();

        //返回的参数
        SkuLsResult skuLsResult = new SkuLsResult();

        try {
            SearchResult searchResult = jestClient.execute(seach);

            //商品信息列表
            ArrayList<SkuLsInfo> SkuLsInfoList = new ArrayList<>();
            List<SearchResult.Hit<SkuLsInfo, Void>> resultHits = searchResult.getHits(SkuLsInfo.class);
            for (SearchResult.Hit<SkuLsInfo, Void> resultHit : resultHits) {
                SkuLsInfo skuLsInfo = resultHit.source;


                if (resultHit.highlight != null) {
                    String skuName = resultHit.highlight.get("skuName").get(0);
                    skuLsInfo.setSkuName(skuName);

                    //SkuLsInfoList.add(skuLsInfo);
                }
                SkuLsInfoList.add(skuLsInfo);

            }
            //插入
            skuLsResult.setSkuLsInfoList(SkuLsInfoList);

            //总数
            Long total = searchResult.getTotal();
            skuLsResult.setTotal(total);

            //总页数 =  （总数+ 每页行数 -1） /每页行数
            long totalPage = (total + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
            skuLsResult.setTotalPages(totalPage);


            //聚合部分   商品设计的平台属性
            List<String> attrValueIdList = new ArrayList<>();

            List<TermsAggregation.Entry> groupby_value_id = searchResult.getAggregations().getTermsAggregation("groupby_value_id").getBuckets();
            for (TermsAggregation.Entry entry : groupby_value_id) {
                String entryKey = entry.getKey();
                attrValueIdList.add(entryKey);
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        String key = "SkuHotScore:" + skuId + ":hotScore";
        Long aLong = jedis.incr(key);
        if (aLong%10 == 0) {
            updateHotScore(skuId, aLong);
        }
        jedis.close();

    }


    private void updateHotScore(String skuId, Long hotScore) {
        String updateJson = "{\n" +
                "  \"doc\":{\n" +
                "    \"hotScore\":" + hotScore + "\n" +
                "  }\n" +
                "}";
        Update update = new Update.Builder(updateJson)
                .index("gmall").type("SkuInfo").id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
