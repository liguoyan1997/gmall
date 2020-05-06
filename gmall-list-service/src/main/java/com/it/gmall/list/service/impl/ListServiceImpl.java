package com.it.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.it.bean.SkuLsInfo;
import com.it.bean.SkuLsParams;
import com.it.bean.SkuLsResult;
import com.it.gmall.config.RedisUtil;
import com.it.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    /*声明es客户端*/
    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    /*数据库名*/
    private static final String ES_INDEX="gmall";

    /*表名*/
    private static final String ES_TYPE="SkuInfo";


    /*
    * 建立es库索引表
    * */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        /*
        * 定义动作(建索引库)
        * 执行动作(向索引库中导入数据)
        * */
        Index build = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * 1.定义dsl语句
    * 定义动作
    * 执行动作
    * 获取结果集
    * */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*执行dsl语句*/
        String query=makeQueryStringForSearch(skuLsParams);
        /*定义动作*/
        Search build = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            /*执行动作*/
            searchResult = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*设置返回结果*/
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        return skuLsResult;
    }

    /**
     * 现在redis中记录热度  满10添加到es为1
     * @param skuId
     */
    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        if(hotScore%10==0){
            updateHotScore(skuId,  Math.round(hotScore));
        }
    }

    /**
     *
     * @param skuId
     * @param hotScore
     */
    private void updateHotScore(String skuId, long hotScore) {
        /*编写dsl语句*/
        String dsl = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":\"1\"\n" +
                "  }\n" +
                "}";
        /*定义动作*/
        Update build = new Update.Builder(dsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        /*执行动作*/
        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*设置返回结果*/
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();

//        List<SkuLsInfo> skuLsInfoList;
        /*声明一个属性值来存储SkuLsInfo数据*/
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        /*给集合复制*/
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        /*循环遍历*/
        for(SearchResult.Hit<SkuLsInfo, Void> hit : hits){
            SkuLsInfo skuLsInfo = hit.source;
            /*获取高亮*/
            Map<String, List<String>> highlight = hit.highlight;
            if(highlight != null && highlight.size()>0){
                List<String> list = highlight.get("skuName");
                String heightHI = list.get(0);
                skuLsInfo.setSkuName(heightHI);
            }
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
//        long total;
        skuLsResult.setTotal(searchResult.getTotal());
//        long totalPages;
//        long totalPage= searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()%skuLsParams.getPageSize()+1;
        long totalPage= (searchResult.getTotal() + skuLsParams.getPageSize() -1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);
//        /*平台属性ID集合*/
//        List<String> attrValueIdList;
        ArrayList<String> skuLsInfoValueId = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby = aggregations.getTermsAggregation("groupby");
        List<TermsAggregation.Entry> buckets = groupby.getBuckets();
        for(TermsAggregation.Entry entry : buckets){
            String valueId = entry.getKey();
            skuLsInfoValueId.add(valueId);
        }
        skuLsResult.setAttrValueIdList(skuLsInfoValueId);
        return skuLsResult;
    }


    /*生成dsl语句*/
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        /*定义一个查询器*/
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        /*创建一个bool*/
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        /*判断keyword是否为空*/
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length()>0){
//            TermQueryBuilder termQueryBuilder = new TermQueryBuilder(,);
            /*创建match*/
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            /*创建must*/
            boolQueryBuilder.must(matchQueryBuilder);
            /*设置高亮*/
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            /*设置高亮的规则*/
            highlighter.field("skuName");
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");
            /*将设置号的高亮放到查询器中*/
            searchSourceBuilder.highlight(highlighter);
        }

        /*判断平台属性值Id*/
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {
                /*创建team*/
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                /*创建filter*/
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        /*判断三级分类Id*/
        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length()>0){
            /*创建team*/
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            /*将term添加到filter中*/
            /*创建filter*/
            boolQueryBuilder.filter(termQueryBuilder);
        }

        searchSourceBuilder.query(boolQueryBuilder);

        /*设置分页*/
        searchSourceBuilder.from((skuLsParams.getPageNo()-1)*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());

        /*设置排序*/
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        /*聚合*/
        /*创建一个对象*/
        TermsBuilder groupby = AggregationBuilders.terms("groupby");
        /*放入聚合字段*/
        groupby.field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby);

        String query = searchSourceBuilder.toString();
        System.out.println("query:"+query);
        return query;
    }
}
