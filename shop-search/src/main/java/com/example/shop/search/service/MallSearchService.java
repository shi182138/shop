package com.example.shop.search.service;

import com.example.shop.search.vo.SearchParam;
import com.example.shop.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
