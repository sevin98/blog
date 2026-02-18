package sevin.dev.blog.common.util;

/**
 * 문자열 유틸리티 클래스
 * CodeRabbit 테스트를 위한 샘플 클래스
 */
public class StringUtils {

    /**
     * 문자열이 비어있는지 확인
     * 
     * @param str 확인할 문자열
     * @return 비어있으면 true, 아니면 false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 문자열이 비어있지 않은지 확인
     * 
     * @param str 확인할 문자열
     * @return 비어있지 않으면 true, 아니면 false
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 문자열을 대문자로 변환
     * 
     * @param str 변환할 문자열
     * @return 대문자로 변환된 문자열, null이면 null 반환
     */
    public static String toUpperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }
}
