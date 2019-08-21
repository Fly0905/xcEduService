package com.xuecheng.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.management.Query;
import java.io.IOException;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestSearch {

    @Autowired
    RestHighLevelClient client;

    @Autowired
    RestClient restClient;

    //搜索type下的全部记录
    @Test
    public void testSearchAll() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course_test");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {

            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();

            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 分页查询
    @Test
    public void testSearchPage() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());

        // 在这里设置分页的参数, from 和 size
        int page = 2, size = 1; //这里page是起始的页码，如果从2开始，则起始from = size
        int from = (page - 1) * size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // termQuery 精确匹配
    @Test
    public void testSearchTerm() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(QueryBuilders.termQuery("name", "spring"));

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 根据id查询
    @Test
    public void testIdQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        // 注意是termsQuery而不是termQuery
        String[] ids = new String[]{"1", "2"};
        searchSourceBuilder.query(QueryBuilders.termsQuery("_id", ids));

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 根据id查询
    @Test
    public void testMatchQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        //匹配关键字 , 匹配spring或者开发
//        searchSourceBuilder.query(QueryBuilders.matchQuery("name", "spring开发")
//                .operator(Operator.OR));
        searchSourceBuilder.query(QueryBuilders.matchQuery("name", "spring开发")
                .minimumShouldMatch("80%"));

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 根据MultiQuery查询
    @Test
    public void testMultiQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        searchSourceBuilder.query(QueryBuilders.multiMatchQuery("spring框架",
                "name", "description")
                .minimumShouldMatch("50%").field("name", 10));

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 布尔查询
    @Test
    public void testBoolQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        // 先给出一个MutilQ Query
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery("spring框架",
                "name", "description")
                .minimumShouldMatch("50%");
        multiMatchQueryBuilder.field("name", 10);

        //然后给出一个 TermQuery
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("studymodel", "201001");

        //布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 下面使用must，表示两个都必须要满足
        boolQueryBuilder.must(multiMatchQueryBuilder);
        boolQueryBuilder.must(termQueryBuilder);

        //设置布尔查询对象
        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // 过滤器
    @Test
    public void testFilter() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        // 先给出一个MutilQ Query
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery("spring框架",
                "name", "description")
                .minimumShouldMatch("50%");
        multiMatchQueryBuilder.field("name", 10);

        //布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(multiMatchQueryBuilder);

        //----在这里进行过滤的功能
        boolQueryBuilder.filter(QueryBuilders.termQuery("studymodel", "201001"));
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(60).lte(100));

        //设置布尔查询对象
        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    @Test
    public void testSort() throws IOException {
        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        // 排序，按照studymodel降序，和price升序
        searchSourceBuilder.sort("studymodel", SortOrder.DESC);
        searchSourceBuilder.sort("price", SortOrder.ASC);


        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }

    // HighLight ， 注意这里不能少了MultiMatchQueryBuilder，不然结果里面就没有了<tag></tag>
    @Test
    public void testHighLight() throws IOException {

        SearchRequest searchRequest = new SearchRequest("xc_course");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "description"}, new String[]{});

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<tag>");
        highlightBuilder.postTags("</tag>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
//        highlightBuilder.fields().add(new HighlightBuilder.Field("description"));
        searchSourceBuilder.highlighter(highlightBuilder);


        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();

        for (SearchHit hit : searchHits) {

            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                //取出name高亮字段
                HighlightField nameHighlightField = highlightFields.get("name");
                if (nameHighlightField != null) {
                    Text[] fragments = nameHighlightField.getFragments();
                    StringBuffer sb = new StringBuffer();
                    for (Text text : fragments) {
                        sb.append(text);
                    }
                    name = sb.toString();
                }
            }
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }
    }


    //Highlight2， 加上了MultiMatchQueryBuilder，就可以正常加标签了
    @Test
    public void testHighlight2() throws IOException {
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //指定类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

//        //boolQuery搜索方式
        //先定义一个MultiMatchQuery
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery("开发框架", "name", "description")
                .minimumShouldMatch("50%")
                .field("name", 10);

        //定义一个boolQuery
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(multiMatchQueryBuilder);

        searchSourceBuilder.query(boolQueryBuilder);
        //设置源字段过虑,第一个参数结果集包括哪些字段，第二个参数表示结果集不包括哪些字段
        searchSourceBuilder.fetchSource(new String[]{"name","studymodel"},new String[]{});

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<tag>");
        highlightBuilder.postTags("</tag>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
//        highlightBuilder.fields().add(new HighlightBuilder.Field("description"));
        searchSourceBuilder.highlighter(highlightBuilder);

        //向搜索请求对象中设置搜索源
        searchRequest.source(searchSourceBuilder);
        //执行搜索,向ES发起http请求
        SearchResponse searchResponse = client.search(searchRequest);
        //搜索结果
        SearchHits hits = searchResponse.getHits();
        //匹配到的总记录数
        long totalHits = hits.getTotalHits();
        //得到匹配度高的文档
        SearchHit[] searchHits = hits.getHits();
        //日期格式化对象
        for(SearchHit hit:searchHits){
            //源文档内容
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //源文档的name字段内容
            String name = (String) sourceAsMap.get("name");
            //取出高亮字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(highlightFields!=null){
                //取出name高亮字段
                HighlightField nameHighlightField = highlightFields.get("name");
                if(nameHighlightField!=null){
                    Text[] fragments = nameHighlightField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for(Text text:fragments){
                        stringBuffer.append(text);
                    }
                    name = stringBuffer.toString();
                }
            }
            String description = (String) sourceAsMap.get("description");
            String studymodel = (String) sourceAsMap.get("studymodel");
            System.out.println(name);
            System.out.println(studymodel);
//            System.out.println(description);
        }
    }

}
