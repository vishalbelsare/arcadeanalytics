package com.arcadeanalytics.service;

/*-
 * #%L
 * Arcade Analytics
 * %%
 * Copyright (C) 2018 - 2019 ArcadeAnalytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.arcadeanalytics.data.Sprite;
import com.arcadeanalytics.data.SpritePlayer;
import com.arcadeanalytics.domain.DataSource;
import com.arcadeanalytics.domain.DataSourceIndex;
import com.arcadeanalytics.domain.enumeration.IndexingStatus;
import com.arcadeanalytics.index.ElasticBulkGraphIndexer;
import com.arcadeanalytics.provider.DataSourceGraphProvider;
import com.arcadeanalytics.provider.DataSourceInfo;
import com.arcadeanalytics.provider.DataSourceProviderFactory;
import com.arcadeanalytics.repository.DataSourceIndexRepository;
import com.arcadeanalytics.repository.DataSourceRepository;
import com.arcadeanalytics.service.dto.SearchQueryDTO;
import com.arcadeanalytics.service.util.DataSourceUtil;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.arcadeanalytics.index.IndexConstants.ARCADE_ID;
import static com.arcadeanalytics.index.IndexConstants.ARCADE_TYPE;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.truncate;
import static org.elasticsearch.index.query.QueryStringQueryBuilder.Operator.AND;

@Service
public class ElasticGraphIndexerService {

    private final Logger log = LoggerFactory.getLogger(ElasticGraphIndexerService.class);

    private final ElasticsearchTemplate searchTemplate;

    private final ObjectMapper mapper;
    private final CacheManager cacheManager;
    private final DataSourceProviderFactory<DataSourceGraphProvider> dataSourceGraphProviderFactory;

    private final DataSourceIndexRepository dataSourceIndexRepository;
    private final DataSourceRepository dataSourceRepository;


    @Autowired
    public ElasticGraphIndexerService(ElasticsearchTemplate searchTemplate,
                                      DataSourceRepository dataSourceRepository,
                                      DataSourceIndexRepository dataSourceIndexRepository,
                                      ObjectMapper mapper,
                                      CacheManager cacheManager,
                                      DataSourceProviderFactory<DataSourceGraphProvider> dataSourceGraphProviderFactory) {
        this.searchTemplate = searchTemplate;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourceIndexRepository = dataSourceIndexRepository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
        this.dataSourceGraphProviderFactory = dataSourceGraphProviderFactory;
    }

    /**
     * Perform indexing of the given {@link DataSource} fetching data with the given set of queries
     *
     * @param datasource the {@link DataSource} to be indexed
     * @param queries    a set of queries used to fetch data from the {@link DataSource}
     * @return a future with the {@link DataSourceIndex} descriptor
     */
    @Async
    @Timed
    public Future<DataSourceIndex> index(DataSource datasource, String... queries) {

        log.info("start indexing of datasource {} with queries {}  ", datasource.getId(), queries);

        datasource.indexing(IndexingStatus.INDEXING);

        dataSourceRepository.save(datasource);

        DataSourceIndex index = new DataSourceIndex()
                .startedAt(LocalDate.now());

        final DataSourceInfo dsInfo = DataSourceUtil.toDataSourceInfo(datasource);
        final DataSourceGraphProvider graphProvider = dataSourceGraphProviderFactory.create(dsInfo);

        try {
            SpritePlayer indexer = new ElasticBulkGraphIndexer(searchTemplate.getClient(), datasource.getId().toString());

            graphProvider.provideTo(dsInfo, indexer);

            index.documents(indexer.processed())
                    .report("Indexing completed")
                    .endedAt(LocalDate.now())
                    .dataSource(datasource);

            datasource.indexing(IndexingStatus.INDEXED);

            log.info("end of indexing datasource {} ", datasource.getId());

            return new AsyncResult<>(index);
        } catch (Exception e) {
            log.error("error while indexing datasource " + datasource.getId(), e);

            index.documents(0L)
                    .report("Error while indexing: " + e.getMessage())
                    .endedAt(LocalDate.now())
                    .dataSource(datasource);

            datasource.indexing(IndexingStatus.NOT_INDEXED);

            throw e;
        } finally {
            dataSourceIndexRepository.save(index);
            dataSourceRepository.save(datasource);

        }
    }


    public boolean hasIndex(DataSource dataSource) {
        log.info("check index for data-source {} ", dataSource.getId());

        final Client client = searchTemplate.getClient();
        IndicesAdminClient indices = client.admin().indices();

        String indexName = dataSource.getId().toString();
        IndicesExistsResponse existsResponse = indices
                .exists(Requests.indicesExistsRequest(indexName)).actionGet();

        return existsResponse.isExists();
    }

    public boolean deleteIndex(DataSource dataSource) {
        log.info("delete index for data-source {} ", dataSource.getId());

        final Client client = searchTemplate.getClient();
        IndicesAdminClient indices = client.admin().indices();

        String indexName = dataSource.getId().toString();
        IndicesExistsResponse existsResponse = indices
                .exists(Requests.indicesExistsRequest(indexName)).actionGet();

        //delete if present
        if (existsResponse.isExists()) {
            final boolean acknowledged = indices.delete(Requests.deleteIndexRequest(indexName))
                    .actionGet().isAcknowledged();
            log.info("index for data-source {} deleted:: {} ", indexName, acknowledged);
            return acknowledged;
        }

        return true;

    }

    /**
     * Search over the indexed {@link DataSource} by the given query.
     *
     * @param dataSource the datasource
     * @param query      the lucene query
     * @return the list of results
     * @throws IOException if somethig goes wrong
     */
    @Timed
    public String search(DataSource dataSource, SearchQueryDTO query) throws IOException {
        SearchResponse searchResponse = getSearchResponse(dataSource, query);

        return searchResponse.toString();
//        SearchHit[] hits = searchResponse.getHits().getHits();
//
//
//        log.info("found {} documents", hits.length);
//
//        return Stream.of(hits)
////                .peek(h -> log.info("h:: " + h.toString()))
////                .peek(h -> log.info("hf:: " + h.getHighlightFields().size()))
////                .map(h -> h.getSource())
////                .peek(s -> log.info("s:: " + s))
//                .map(hit -> {
//
//                    Sprite sprite = new Sprite().load(hit.getSource());
////                    hit.getHighlightFields().f
//                    return sprite;
//                })
//                .collect(Collectors.toList());

    }

    @NotNull
    private SearchResponse getSearchResponse(DataSource dataSource, SearchQueryDTO query) throws IOException {
        log.info("search on data-source {}  with query:: {} - filters:: {} ", dataSource.getId(), query, query.getIds());

        Client client = searchTemplate.getClient();

        final String indexName = dataSource.getId().toString();

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName)
                .setHighlighterForceSource(true)
                .setHighlighterEncoder("default")
                .setHighlighterPreTags("<strong>")
                .setHighlighterPostTags("</strong>")
                .setSize(query.getNumOfDocuments());

        final QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(query.getQuery()).defaultOperator(AND);

        indexFields(indexName, client, query.isUseEdges())
                .stream()
                .forEach(field -> {
                    searchRequestBuilder.addHighlightedField(field);
                    queryBuilder.field(field);
//                    searchRequestBuilder.addHighlightedField(field + ".raw");
                });

        if (query.getIds().length > 0) {
            searchRequestBuilder.setPostFilter(QueryBuilders.termsQuery(ARCADE_ID, query.getIds()));
        }

        searchRequestBuilder.setQuery(queryBuilder);

        log.debug("query:: {}", searchRequestBuilder.toString());
        SearchResponse searchResponse = searchRequestBuilder
                .execute()
                .actionGet();

        log.debug("response:: {} ", searchResponse.toString());
        return searchResponse;
    }


    public List<Sprite> searchAndMap(DataSource dataSource, SearchQueryDTO query) throws IOException {

        SearchHit[] hits = getSearchResponse(dataSource, query).getHits().getHits();

        log.info("found {} documents", hits.length);

        return Stream.of(hits)
//                .peek(h -> log.info("h:: " + h.toString()))
//                .peek(h -> log.info("hf:: " + h.getHighlightFields().size()))
//                .map(h -> h.getSource())
//                .peek(s -> log.info("s:: " + s))
                .map(hit -> {

                    Sprite sprite = new Sprite().load(hit.getSource());
//                    hit.getHighlightFields().f
                    return sprite;
                })
                .collect(Collectors.toList());

    }

    @Timed
    public String aggregate(DataSource dataSource,
                            SearchQueryDTO query,
                            Set<String> classes,
                            Set<String> fields,
                            long minDocCount,
                            int maxValuesPerField) throws IOException {

        final String indexName = dataSource.getId().toString();

        log.info("start aggregation on data-source {} with minDocCount {}, maxValuesPerField {}, classes {} , fields {}",
                dataSource.getId(),
                minDocCount,
                maxValuesPerField,
                classes,
                fields);

        Client client = searchTemplate.getClient();

        if (fields.isEmpty()) fields = indexFields(indexName, client, query.isUseEdges());

        final SearchResponse searchResponse = termAggregations(client, indexName, classes, fields, query, minDocCount, maxValuesPerField);

        return searchResponse.toString();
    }

    @Timed
    public Map<String, Object> aggregateAndMap(DataSource dataSource,
                                               SearchQueryDTO query,
                                               Set<String> classes,
                                               Set<String> fields,
                                               long minDocCount,
                                               int maxValuesPerField) throws IOException {

        final String indexName = dataSource.getId().toString();

        log.info("start aggregation on data-source {} with minDocCount {}, maxValuesPerField {}, classes {} , fields {}",
                dataSource.getId(),
                minDocCount,
                maxValuesPerField,
                classes,
                fields);

        Client client = searchTemplate.getClient();

        if (fields.isEmpty()) fields = indexFields(indexName, client, query.isUseEdges());

        final SearchResponse searchResponse = termAggregations(client, indexName, classes, fields, query, minDocCount, maxValuesPerField);
        log.debug("tree: {}", searchResponse.toString());

        Map<String, Object> facetsTree = searchResponseToMap(fields, searchResponse);

        log.info("done aggregation on data-source {}", dataSource.getId(), minDocCount, maxValuesPerField);

        return facetsTree;

    }

    @Timed
    public String aggregate(DataSource dataSource,
                            Set<String> classes,
                            Set<String> fields,
                            long minDocCount,
                            int maxValuesPerField) throws IOException {

        final String indexName = dataSource.getId().toString();

        log.info("start aggregation on data-source {} on classes {} and fields {}  with minDocCount {}, maxValuesPerField {}", dataSource.getId(), classes, fields, minDocCount, maxValuesPerField);

        Client client = searchTemplate.getClient();

        if (fields.isEmpty()) fields = indexFields(indexName, client, true);

        final SearchResponse searchResponse = termAggregations(client, indexName, classes, fields, new SearchQueryDTO(), minDocCount, maxValuesPerField);

        return searchResponse.toString();
    }

    @Timed
    public Map<String, Object> aggregateAndMap(DataSource dataSource,
                                               Set<String> classes,
                                               Set<String> fields,
                                               long minDocCount,
                                               int maxValuesPerField) throws IOException {

        final String indexName = dataSource.getId().toString();

        log.info("start aggregation on data-source {} on classes {} and fields {}  with minDocCount {}, maxValuesPerField {}", dataSource.getId(), classes, fields, minDocCount, maxValuesPerField);

        Client client = searchTemplate.getClient();

        if (fields.isEmpty()) fields = indexFields(indexName, client, true);

        final SearchResponse searchResponse = termAggregations(client, indexName, classes, fields, new SearchQueryDTO(), minDocCount, maxValuesPerField);

        Map<String, Object> facetsTree = searchResponseToMap(fields, searchResponse);

        log.info("done aggregation on data-source {}", dataSource.getId(), minDocCount, maxValuesPerField);

        return facetsTree;

    }

    private Set<String> indexFields(String indexName, Client client, boolean useEdges) throws IOException {
        ClusterState cs = client.admin()
                .cluster()
                .prepareState()
                .setIndices(indexName)
                .execute().actionGet().getState();
        final IndexMetaData imd = cs.getMetaData().index(indexName);
        if (imd == null)
            throw new RuntimeException("No index '" + indexName + "' has been defined");

        final Set<String> fields = indexFieldList("", imd.mappingOrDefault("node").getSourceAsMap())
                .stream()
                .filter(f -> !f.startsWith("_a_"))
                .filter(f -> !f.startsWith("@"))
                .filter(f -> !f.startsWith("in_"))
                .filter(f -> !f.startsWith("out_"))
                .collect(Collectors.toSet());

        if (useEdges) {
            fields.addAll(indexFieldList("", imd.mappingOrDefault("edge").getSourceAsMap())
                    .stream()
                    .filter(f -> !f.startsWith("_a_"))
                    .filter(f -> !f.startsWith("@"))
                    .filter(f -> !f.startsWith("in_"))
                    .filter(f -> !f.startsWith("out_"))
                    .filter(f -> !f.startsWith("in"))
                    .filter(f -> !f.startsWith("out"))
                    .collect(Collectors.toSet()));
        }

        log.info("fields:: {} ", fields);
        return fields;
    }

    private SearchResponse termAggregations(Client client,
                                            String indexName,
                                            Set<String> classes,
                                            Set<String> fields,
                                            SearchQueryDTO query,
                                            long minDocCount,
                                            int maxValuesPerField) {


        final List<AggregationBuilder> termsAggregations = fields.stream()
                .map(field -> AggregationBuilders.terms(field)
                        .field(field + ".raw")
                        .order(Terms.Order.compound(Terms.Order.count(false), Terms.Order.term(true)))
                        .minDocCount(minDocCount)
                        .size(maxValuesPerField)
                ).collect(Collectors.toList());


        TermsBuilder classAggregation = AggregationBuilders.terms("class")
                .field("@class.raw");

        if (!classes.isEmpty()) classAggregation.include(classes.toArray(new String[]{}));

        termsAggregations.stream()
                .forEach(a -> classAggregation.subAggregation(a));

        AggregationBuilder agg =
                AggregationBuilders
                        .terms("type")
                        .field(ARCADE_TYPE)
                        .subAggregation(classAggregation);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addAggregation(agg)
                .setSize(0);


        if (query.getIds().length > 0) {
            log.debug("aggregate only on :: {}", truncate(join(",", query.getIds()), 100));
            searchRequestBuilder.setQuery(QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery(ARCADE_ID, query.getIds())));
        }

        SearchResponse searchResponse = searchRequestBuilder
                .execute()
                .actionGet();

        return searchResponse;
    }

    @NotNull
    private Map<String, Object> searchResponseToMap(Set<String> fields, SearchResponse searchResponse) throws IOException {
        Map<String, Object> facetsTree = new HashMap<>();

        final JsonNode jsonNode = mapper.readTree(searchResponse.toString());
        final Iterator<JsonNode> types = jsonNode
                .path("aggregations")
                .path("type")
                .path("buckets")
                .elements();

        while (types.hasNext()) {

            final JsonNode classes = types.next()
                    .path("class")
                    .path("buckets");

            final Iterator<JsonNode> elements = classes.elements();

            while (elements.hasNext()) {
                final JsonNode aClass = elements.next();

                Map<String, Object> classMap = new HashMap<>();
                facetsTree.put(aClass.get("key").asText(), classMap);
                classMap.put("doc_count", aClass.get("doc_count").asLong());

                Map<String, Object> props = new HashMap<>();
                classMap.put("propertyValues", props);

                for (String field : fields) {

                    final JsonNode fieldNode = aClass.get(field);
                    if (fieldNode.get("buckets").iterator().hasNext()) {
                        Map<String, Object> vals = new HashMap<>();
                        props.put(field, vals);
                        fieldNode.get("buckets").iterator()
                                .forEachRemaining(p -> vals.put(p.get("key").asText(), p.get("doc_count").asLong()));
                    }
                }
            }
        }

        return facetsTree;
    }


    private List<String> indexFieldList(String fieldName, Map<String, Object> mapProperties) {
        List<String> fieldList = new ArrayList<>();
        Map<String, Object> map = (Map<String, Object>) mapProperties.getOrDefault("properties", Collections.emptyMap());
        Set<String> keys = map.keySet();
        for (String key : keys) {
            if (((Map<String, Object>) map.get(key)).containsKey("type")) {
                fieldList.add(fieldName + "" + key);
            } else {
                List<String> tempList = indexFieldList(fieldName + "" + key + ".", (Map<String, Object>) map.get(key));
                fieldList.addAll(tempList);
            }
        }
        return fieldList;
    }


}
