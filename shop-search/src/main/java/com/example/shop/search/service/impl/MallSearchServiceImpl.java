package com.example.shop.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.to.es.SkuEsModel;
import com.example.common.utils.R;
import com.example.shop.search.config.ShopElasticSearchConfig;
import com.example.shop.search.constant.EsConstant;
import com.example.shop.search.feign.ProductFeignService;
import com.example.shop.search.service.MallSearchService;
import com.example.shop.search.vo.AttrResponseVo;
import com.example.shop.search.vo.BrandVo;
import com.example.shop.search.vo.SearchParam;
import com.example.shop.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {
//1?????????????????????????????????DSL??????
        SearchResult result = null;
        //2?????????????????????
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2?????????????????????
            SearchResponse response = client.search(searchRequest, ShopElasticSearchConfig.COMMON_OPTIONS);
            //3???????????????????????????????????????????????????
            result = buildSearchResult(response,param);
            System.out.println(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }


    /**
     * ??????????????????
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//??????DSL??????
        /**
         * ?????????????????????????????????????????????????????????????????????????????????
         */
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 bool-must????????????
        if (!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
        //1.2.1 bool-filter -??????????????????ID??????
        if (param.getCatalog3Id() != null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        //1.2.2 ????????????ID??????
        if (param.getBrandId() != null && param.getBrandId().size()>0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
        //1.2.3 ???????????????????????????????????????
        if (param.getAttrs() != null && param.getAttrs().size()>0){
            //attrs=1_5???:8???&attrs=2_16g:8g
            for (String attr :  param.getAttrs()) {
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];//???????????????ID
                //??????????????????
                String[] attrValues = s[1].split(":");
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedboolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                //?????????????????????????????????nested??????
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }

        }
        //1.2.4 ???????????????????????????
        if (param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock()==1));
        }
        //1.2.5 ????????????????????????
        if (!StringUtils.isEmpty(param.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if (param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }

            boolQuery.filter(rangeQuery);
        }
        //??????????????????????????????
        sourceBuilder.query(boolQuery);
        /**
         * ????????????????????????
         */
        //2.1 ??????
        if (!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            //sort=hotScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0],order);
        }
        //2.2 ??????
        sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3 ??????
        if (!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        /**
         * ????????????
         */
        //????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);
        //????????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);
        //????????????
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //????????????????????????attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //?????????????????????attrId???????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //?????????????????????attrId??????????????????????????????attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue"));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        String s = sourceBuilder.toString();
        System.out.println("?????????dsl?????????" + s);
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1?????????????????????????????????
        SearchHits hits = response.getHits();
        List<SkuEsModel> list = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length>0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString,SkuEsModel.class);
                //??????????????????
                if (!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                list.add(esModel);
            }
        }
        result.setProduct(list);
        //2???????????????????????????????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            //????????????ID
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //???????????????
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //????????????????????????
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = ((Terms.Bucket) item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
        //3???????????????????????????????????????????????????
        List<SearchResult.BrandVo> brands = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<? extends Terms.Bucket> brandAggBuckets = brand_agg.getBuckets();
        for (Terms.Bucket bucket : brandAggBuckets) {
            //??????????????????
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //???????????????ID
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //??????????????????
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //??????????????????
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);
            brands.add(brandVo);
        }
        result.setBrands(brands);
        //4???????????????????????????????????????????????????
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //????????????ID
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //??????????????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        //5???????????????-??????
        result.setPageNum(param.getPageNum());
        //5???????????????-????????????
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //5???????????????-?????????
        int totalPages = total%EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total/EsConstant.PRODUCT_PAGESIZE : (int)total/EsConstant.PRODUCT_PAGESIZE+1;
        result.setTotalPages(totalPages);
        //??????????????????
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //6??????????????????????????????  ????????????
        if (param.getAttrs() !=null && param.getAttrs().size()>0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //1???????????????attrs????????????????????????
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0){
                    AttrResponseVo attrResponseVo = (AttrResponseVo) r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(attrResponseVo.getAttrName());
                }else{
                    navVo.setNavName(s[0]);
                }
                //2???????????????????????????????????????????????????????????????????????????url???????????????????????????
                //???????????????????????????????????????
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimao.com/list.html?"+replace);

                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }
        //????????????
        if (param.getBrandId() != null && param.getBrandId().size() >0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("??????");
            //????????????????????????
            R r = productFeignService.brandsInfo(param.getBrandId());
            if (r.getCode() == 0){
                List<BrandVo> brand = (List<BrandVo>) r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brand) {
                    buffer.append(brandVo.getBrandName()+";");
                    replace = replaceQueryString(param, brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.gulimao.com/list.html?"+replace);
            }
            navs.add(navVo);
            result.setNavs(navs);

        }
        return result;
    }

    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+", "%20");//????????????java???+?????????????????????
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
    }

}
