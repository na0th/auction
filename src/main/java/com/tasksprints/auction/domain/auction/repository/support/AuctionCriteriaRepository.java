package com.tasksprints.auction.domain.auction.repository.support;

import com.tasksprints.auction.domain.auction.model.Auction;
import com.tasksprints.auction.domain.auction.model.AuctionCategory;
import com.tasksprints.auction.domain.auction.model.AuctionStatus;
import com.tasksprints.auction.domain.product.model.ProductCategory;


import java.time.LocalDateTime;
import java.util.List;

public interface AuctionCriteriaRepository {
    public List<Auction> getAuctionsByFilters(ProductCategory productCategory,
                                              AuctionCategory category);
    public List<Auction> getAuctionsEndWith24Hours(LocalDateTime now, LocalDateTime next24Hours, AuctionStatus auctionStatus);
}
