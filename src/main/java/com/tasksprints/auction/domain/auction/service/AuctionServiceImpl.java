package com.tasksprints.auction.domain.auction.service;

import com.tasksprints.auction.domain.auction.dto.response.AuctionResponse;
import com.tasksprints.auction.domain.auction.exception.AuctionAlreadyClosedException;
import com.tasksprints.auction.domain.auction.exception.AuctionNotFoundException;
import com.tasksprints.auction.domain.auction.exception.InvalidAuctionTimeException;
import com.tasksprints.auction.domain.auction.model.Auction;
import com.tasksprints.auction.domain.auction.model.AuctionCategory;
import com.tasksprints.auction.domain.auction.repository.AuctionRepository;
import com.tasksprints.auction.domain.auction.model.AuctionStatus;
import com.tasksprints.auction.domain.auction.dto.request.AuctionRequest;
import com.tasksprints.auction.domain.user.exception.UserNotFoundException;
import com.tasksprints.auction.domain.user.model.User;
import com.tasksprints.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    @Override
    public AuctionResponse createAuction(Long userId, AuctionRequest.Create auctionRequest) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (auctionRequest.getStartTime().isAfter(auctionRequest.getEndTime())) {
            throw new InvalidAuctionTimeException("End time must be after start time");
        }

        Auction newAuction = Auction.create(
                auctionRequest.getStartTime(),
                auctionRequest.getEndTime(),
                auctionRequest.getStartingBid(),
                auctionRequest.getAuctionCategory(),
                auctionRequest.getAuctionStatus(),
                seller
        );

        Auction savedAuction = auctionRepository.save(newAuction);
        /**
         * Product 생성에 대한 부분 고려 필요
         * STEP 1
         * - S3 버킷에 올리는 api 따로 구성 ( 독립적 시행 ) url 반환
         * - 해당 url 을 토대로 Auction 생성 시, Product 도 같이 생성
         * STEP 2
         * - 각각의 기능을 완전 분리
         */
        return AuctionResponse.of(savedAuction);
    }

    @Override
    public void closeAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        if (auction.getAuctionStatus() == AuctionStatus.CLOSED) {
            throw new AuctionAlreadyClosedException("Auction is already closed");
        }
        auction.setAuctionStatus(AuctionStatus.CLOSED);
    }

    @Override
    public String getAuctionStatus(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        return auction.getAuctionStatus().name();
    }

    @Override
    public List<AuctionResponse> getAuctionsByUser(Long userId) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Auction> foundAuctions =  auctionRepository.findAuctionsByUserId(seller.getId());

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }


    @Override
    public List<AuctionResponse> getAllAuctions() {
        List<Auction> foundAuctions =  auctionRepository.findAll();
        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }

    @Override
    public AuctionResponse getAuctionById(Long auctionId) {
        Auction foundAuction =  auctionRepository.findAuctionById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        return AuctionResponse.of(foundAuction);
    }
    @Deprecated
    @Override
    public List<AuctionResponse> getAuctionsByProductCategory(AuctionRequest.ProductCategoryParam param) {
        List<Auction> foundAuctions = auctionRepository.findAuctionByProduct_Category(param.getProductCategory());

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();

    }

    @Deprecated
    @Override
    public List<AuctionResponse> getAuctionsByAuctionCategory(AuctionRequest.AuctionCategoryParam param) {
        List<Auction> foundAuctions =  auctionRepository.findAuctionsByAuctionCategory(param.getAuctionCategory());

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }
    /**
     * NULL POINTER EXCEPTION 발생
     * NULL 안정성 보장을 해줬음**/
    @Override
    public List<AuctionResponse> getAuctionsByFilter(AuctionRequest.ProductCategoryParam productCategoryParam, AuctionRequest.AuctionCategoryParam auctionCategoryParam) {
        List<Auction> foundAuctions = auctionRepository.getAuctionsByFilters(
                productCategoryParam!=null?productCategoryParam.getProductCategory():null,
                auctionCategoryParam!=null?auctionCategoryParam.getAuctionCategory():null);

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }

    @Override
    public List<AuctionResponse> getAuctionsByEndTimeBetweenOrderByEndTimeAsc() {
        // auction의 endTime까지 24시간 이하로 남은 진행중인 경매 목록 조회
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);

        //endTime이 now ~ now + 24hour에 포함되는 진행 상태인 경매 목록 조회
        List<Auction> foundAuctions = auctionRepository.findAuctionsByEndTimeBetweenAndAuctionStatusOrderByEndTimeAsc(now, next24Hours, AuctionStatus.ACTIVE);

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }

    @Override
    public List<AuctionResponse> getAuctionsEndWith24Hours() {
        // auction의 endTime까지 24시간 이하로 남은 진행중인 경매 목록 조회
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);

        //endTime이 now ~ now + 24hour에 포함되는 진행 상태인 경매 목록 조회
        List<Auction> foundAuctions = auctionRepository.getAuctionsEndWith24Hours(now, next24Hours, AuctionStatus.ACTIVE);

        return foundAuctions.stream()
                .map(AuctionResponse::of)
                .toList();
    }
}