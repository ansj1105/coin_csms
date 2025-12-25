package com.csms.common;

import io.vertx.sqlclient.SqlClient;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.any;

/**
 * 테스트용 ArgumentMatcher 헬퍼 클래스
 * 
 * <p>테스트 코드에서 타입 안전한 ArgumentMatcher를 사용할 수 있도록 헬퍼 메서드를 제공합니다.
 * 
 * <p>사용 예:
 * <pre>{@code
 * import static com.csms.common.TestArgumentMatchers.anySqlClient;
 * 
 * when(repository.getReferralBonus(anySqlClient())).thenReturn(...);
 * verify(repository, times(1)).getReferralBonus(anySqlClient());
 * }</pre>
 */
public final class TestArgumentMatchers {
    
    private TestArgumentMatchers() {
        // 유틸리티 클래스는 인스턴스화 불가
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * SqlClient 타입의 any() 매처를 반환합니다.
     * 
     * <p>이 메서드는 {@code any(SqlClient.class)}의 타입 안전한 래퍼입니다.
     * 테스트 코드의 가독성을 높이고 타입 안전성을 보장합니다.
     * 
     * @return SqlClient 타입의 ArgumentMatcher
     */
    public static SqlClient anySqlClient() {
        return any(SqlClient.class);
    }
}

