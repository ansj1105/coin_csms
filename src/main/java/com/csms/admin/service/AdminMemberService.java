package com.csms.admin.service;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
import com.csms.admin.repository.AdminMemberRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMemberService extends com.csms.common.service.BaseService {
    
    private final AdminMemberRepository repository;
    
    public AdminMemberService(PgPool pool) {
        super(pool);
        this.repository = new AdminMemberRepository(pool);
    }
    
    public Future<MemberListDto> getMembers(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sanctionStatus
    ) {
        // 기본값 설정
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (offset == null || offset < 0) {
            offset = 0;
        }
        
        // 검색어 길이 제한
        if (searchKeyword != null && searchKeyword.length() > 20) {
            searchKeyword = searchKeyword.substring(0, 20);
        }
        
        return repository.getMembers(limit, offset, searchCategory, searchKeyword, activityStatus, sanctionStatus);
    }
    
    public Future<MemberDetailDto> getMemberDetail(Long memberId) {
        return repository.getMemberDetail(memberId);
    }
    
    public Future<Void> updateSanctionStatus(Long memberId, SanctionRequestDto request) {
        String sanctionStatus = request.getSanctionStatus();
        
        // 현재 제재 상태 조회
        return repository.getMemberDetail(memberId)
            .compose(member -> {
                String currentSanction = member.getSanctionStatus();
                
                // 현재 제재 상태와 동일하면 해제 (null)
                if (sanctionStatus != null && sanctionStatus.equals(currentSanction)) {
                    sanctionStatus = null;
                }
                
                // null이면 빈 문자열로 변환 (DB에서 null로 저장)
                String finalSanctionStatus = (sanctionStatus == null || sanctionStatus.trim().isEmpty()) 
                    ? null 
                    : sanctionStatus;
                
                return repository.updateSanctionStatus(memberId, finalSanctionStatus);
            });
    }
}

