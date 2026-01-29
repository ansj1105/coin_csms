package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminAirdropRepository;
import com.csms.common.exceptions.BadRequestException;
import com.csms.common.exceptions.NotFoundException;
import com.csms.common.service.BaseService;
import com.csms.user.repository.UserRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
public class AdminAirdropService extends BaseService {
    
    private final AdminAirdropRepository repository;
    private final UserRepository userRepository;
    
    public AdminAirdropService(PgPool pool) {
        super(pool);
        this.repository = new AdminAirdropRepository();
        this.userRepository = new UserRepository();
    }
    
    /**
     * 에어드랍 Phase 목록 조회
     */
    public Future<AirdropPhaseListDto> getPhases(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        Long userId,
        Integer phase,
        String status
    ) {
        log.info("에어드랍 Phase 목록 조회 - limit: {}, offset: {}, userId: {}, phase: {}, status: {}", 
            limit, offset, userId, phase, status);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        
        return repository.getPhases(client, finalLimit, finalOffset, searchCategory, searchKeyword, userId, phase, status);
    }
    
    /**
     * 에어드랍 Phase 단건 조회
     */
    public Future<AirdropPhaseDto> getPhaseById(Long phaseId) {
        log.info("에어드랍 Phase 조회 - phaseId: {}", phaseId);
        
        return repository.getPhaseById(client, phaseId)
            .recover(throwable -> {
                log.error("에어드랍 Phase 조회 실패 - phaseId: {}", phaseId, throwable);
                return Future.failedFuture(new NotFoundException("에어드랍 Phase를 찾을 수 없습니다."));
            });
    }
    
    /**
     * 에어드랍 Phase 생성
     */
    public Future<AirdropPhaseDto> createPhase(CreateAirdropPhaseRequestDto request) {
        log.info("에어드랍 Phase 생성 - userId: {}, phase: {}, amount: {}", 
            request.getUserId(), request.getPhase(), request.getAmount());
        
        // 유효성 검사
        if (request.getUserId() == null) {
            return Future.failedFuture(new BadRequestException("userId는 필수입니다."));
        }
        if (request.getPhase() == null || request.getPhase() < 1 || request.getPhase() > 5) {
            return Future.failedFuture(new BadRequestException("phase는 1~5 사이의 값이어야 합니다."));
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Future.failedFuture(new BadRequestException("amount는 0보다 큰 값이어야 합니다."));
        }
        
        // unlockDate 계산
        LocalDateTime requestUnlockDate = request.getUnlockDate();
        Integer requestDaysRemaining = request.getDaysRemaining();
        
        if (requestUnlockDate == null && requestDaysRemaining == null) {
            return Future.failedFuture(new BadRequestException("unlockDate 또는 daysRemaining 중 하나는 필수입니다."));
        }
        
        // final 변수로 계산
        final LocalDateTime unlockDate;
        final Integer daysRemaining;
        
        if (requestUnlockDate == null && requestDaysRemaining != null) {
            unlockDate = LocalDateTime.now().plusDays(requestDaysRemaining);
            daysRemaining = requestDaysRemaining;
        } else if (requestUnlockDate != null && requestDaysRemaining == null) {
            unlockDate = requestUnlockDate;
            // unlockDate가 있으면 daysRemaining 계산
            int calculatedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), requestUnlockDate);
            daysRemaining = calculatedDays < 0 ? 0 : calculatedDays;
        } else {
            // 둘 다 있는 경우
            unlockDate = requestUnlockDate;
            daysRemaining = requestDaysRemaining;
        }
        
        // 사용자 존재 여부 확인
        return userRepository.getUserById(client, request.getUserId())
            .compose(user -> {
                if (user == null) {
                    log.warn("에어드랍 Phase 생성 실패 - 사용자를 찾을 수 없음: userId: {}", request.getUserId());
                    return Future.failedFuture(new NotFoundException("사용자를 찾을 수 없습니다. userId: " + request.getUserId()));
                }
                
                // Phase 생성
                return repository.createPhase(
                    client,
                    request.getUserId(),
                    request.getPhase(),
                    request.getAmount(),
                    unlockDate,
                    daysRemaining
                );
            })
            .recover(throwable -> {
                // 외래 키 제약 조건 위반 에러 처리
                if (throwable.getMessage() != null && 
                    (throwable.getMessage().contains("foreign key constraint") || 
                     throwable.getMessage().contains("fk_airdrop_phases_user"))) {
                    log.error("에어드랍 Phase 생성 실패 - 외래 키 제약 조건 위반: userId: {}", request.getUserId(), throwable);
                    return Future.failedFuture(new NotFoundException("사용자를 찾을 수 없습니다. userId: " + request.getUserId()));
                }
                return Future.failedFuture(throwable);
            });
    }
    
    /**
     * 에어드랍 Phase 수정
     */
    public Future<AirdropPhaseDto> updatePhase(Long phaseId, UpdateAirdropPhaseRequestDto request) {
        log.info("에어드랍 Phase 수정 - phaseId: {}", phaseId);
        
        return repository.getPhaseById(client, phaseId)
            .recover(throwable -> {
                log.error("에어드랍 Phase 조회 실패 - phaseId: {}", phaseId, throwable);
                return Future.failedFuture(new NotFoundException("에어드랍 Phase를 찾을 수 없습니다."));
            })
            .compose(existingPhase -> {
                // unlockDate 계산
                LocalDateTime unlockDate = request.getUnlockDate();
                Integer daysRemaining = request.getDaysRemaining();
                
                if (unlockDate == null && daysRemaining != null) {
                    unlockDate = LocalDateTime.now().plusDays(daysRemaining);
                } else if (unlockDate != null && daysRemaining == null) {
                    daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), unlockDate);
                    if (daysRemaining < 0) {
                        daysRemaining = 0;
                    }
                }
                
                return repository.updatePhase(
                    client,
                    phaseId,
                    request.getAmount(),
                    unlockDate,
                    daysRemaining,
                    request.getStatus()
                );
            });
    }
    
    /**
     * 에어드랍 Phase 삭제
     */
    public Future<Void> deletePhase(Long phaseId) {
        log.info("에어드랍 Phase 삭제 - phaseId: {}", phaseId);
        
        return repository.getPhaseById(client, phaseId)
            .recover(throwable -> {
                log.error("에어드랍 Phase 조회 실패 - phaseId: {}", phaseId, throwable);
                return Future.failedFuture(new NotFoundException("에어드랍 Phase를 찾을 수 없습니다."));
            })
            .compose(phase -> repository.deletePhase(client, phaseId));
    }
    
    /**
     * 에어드랍 전송 내역 조회
     */
    public Future<AirdropTransferListDto> getTransfers(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        Long userId,
        String status
    ) {
        log.info("에어드랍 전송 내역 조회 - limit: {}, offset: {}, userId: {}, status: {}", 
            limit, offset, userId, status);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        
        return repository.getTransfers(client, finalLimit, finalOffset, searchCategory, searchKeyword, userId, status);
    }
}
