package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {

    @Value("${xuecheng.course.index}")
    private String index;

    @Value("${xuecheng.course.type}")
    private String type;

    @Value("${xuecheng.course.source_field}")
    private String source_field;


    @Value("${xuecheng.media.index}")
    private String media_index;

    @Value("${xuecheng.media.type}")
    private String media_type;

    @Value("${xuecheng.media.source_field}")
    private String media_source_field;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    //课程搜索
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        if (courseSearchParam == null) {
            courseSearchParam = new CourseSearchParam();
        }
        //创建搜索请求对象
        SearchRequest searchRequest = new SearchRequest(index);
        //设置搜索类型
        searchRequest.types(type);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //过虑源字段
        String[] source_field_array = source_field.split(",");
        searchSourceBuilder.fetchSource(source_field_array, new String[]{});
        //创建布尔查询对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //搜索条件， 这里使用的是多个查询的MultiMatch 查询，根据几个字段联合查询
        //根据关键字搜索
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(
                    courseSearchParam.getKeyword(), "name", "description", "teachplan")
                    .minimumShouldMatch("70%")
                    .field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }

        // 分类搜索
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            //根据一级分类
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            //根据二级分类
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", courseSearchParam.getSt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            //根据难度等级
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }

        //设置boolQueryBuilder到searchSourceBuilder
        searchSourceBuilder.query(boolQueryBuilder);

        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 12;
        }
        //起始记录下标
        int from = (page - 1) * size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);

        QueryResult<CoursePub> queryResult = new QueryResult();
        List<CoursePub> list = new ArrayList<>();

        try {
            //执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            //获取响应结果
            SearchHits hits = searchResponse.getHits();
            //匹配的总记录数
            long totalHits = hits.totalHits;
            queryResult.setTotal(totalHits);
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                CoursePub coursePub = new CoursePub();
                //源文档
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                //这里是后面新增的 取出课程的id
                String id = (String) sourceAsMap.get("id");
                coursePub.setId(id);

                //取出name
                String name = (String) sourceAsMap.get("name");

                // 在这里取出高亮的字段
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (highlightFields.get("name") != null) {
                    HighlightField highlightFieldName = highlightFields.get("name");
                    Text[] fragments = highlightFieldName.getFragments();

                    StringBuffer sb = new StringBuffer();
                    for (Text txt : fragments) {
                        sb.append(txt);
                    }
                    name = sb.toString();
                }
                coursePub.setName(name);

                //图片
                String pic = (String) sourceAsMap.get("pic");
                coursePub.setPic(pic);
                //价格
                Double price = null;
                try {
                    if (sourceAsMap.get("price") != null) {
                        price = (Double) sourceAsMap.get("price");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice(price);
                //旧价格
                Double price_old = null;
                try {
                    if (sourceAsMap.get("price_old") != null) {
                        price_old = (Double) sourceAsMap.get("price_old");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice_old(price_old);
                //将coursePub对象放入list
                list.add(coursePub);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        queryResult.setList(list);
        QueryResponseResult<CoursePub> queryResponseResult =
                new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return queryResponseResult;
    }


    //根据课程Id查询课程信息
    public Map<String, CoursePub> getAll(String id) {

        //设置索引库
        SearchRequest searchRequest = new SearchRequest(index);
        //设置类型
        searchRequest.types(type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //根据课程id进行查询
        searchSourceBuilder.query(QueryBuilders.termsQuery("id", id)); //name value

        //可以设置过滤字段，但是这里是查询所有的
//        searchSourceBuilder.fetchSource(new String[]{"name"}, new String[]{}); //设置包含的和不包含的

        searchRequest.source(searchSourceBuilder);

        //获取搜索结果
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Map<String, CoursePub> resMap = new HashMap<>();

        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            //取出来
            String courseId = (String) sourceAsMap.get("id");
            String name = (String) sourceAsMap.get("name");
            String grade = (String) sourceAsMap.get("grade");
            String charge = (String) sourceAsMap.get("charge");
            String pic = (String) sourceAsMap.get("pic");
            String description = (String) sourceAsMap.get("description");
            String teachplan = (String) sourceAsMap.get("teachplan");

            CoursePub coursePub = new CoursePub();
            coursePub.setId(courseId);
            coursePub.setName(name);
            coursePub.setGrade(grade);
            coursePub.setCharge(charge);
            coursePub.setPic(pic);
            coursePub.setDescription(description);
            coursePub.setTeachplan(teachplan);

            resMap.put(courseId, coursePub); //推入结果集
        }
        return resMap;
    }

    //根据课程计划id数组查询 TeachplanMediaPub 集合
    public QueryResponseResult<TeachplanMediaPub> getMedia(String[] teachplanIds) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(media_index);
        //设置类型
        searchRequest.types(media_type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //source源字段过虑
        String[] source_fields = media_source_field.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{}); // 需要的、不需要的

        //查询条件，根据课程计划id查询(可传入多个id) // 注意是terms
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        long total = hits.getTotalHits();

        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();

        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();

            //取出课程计划媒资信息
            String courseid = (String) sourceAsMap.get("courseid");
            String media_id = (String) sourceAsMap.get("media_id");
            String media_url = (String) sourceAsMap.get("media_url");
            String teachplan_id = (String) sourceAsMap.get("teachplan_id");
            String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");

            teachplanMediaPub.setCourseId(courseid);
            teachplanMediaPub.setMediaUrl(media_url);
            teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
            teachplanMediaPub.setMediaId(media_id);
            teachplanMediaPub.setTeachplanId(teachplan_id);

            //将数据加入列表
            teachplanMediaPubList.add(teachplanMediaPub);

        }

        //构建返回课程媒资信息对象
        QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();

        queryResult.setList(teachplanMediaPubList);
        queryResult.setTotal(total);

        return new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
    }
}
