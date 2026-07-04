package com.echo.util;

/**
 * OAuth/JWT 처리에 쓰는 문자열 변환 유틸.
 */
public final class AttributeUtils {

	private AttributeUtils() {
	}

	/**
	 * 객체를 문자열로 변환한다. null이면 null을 반환한다.
	 */
	public static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

}
