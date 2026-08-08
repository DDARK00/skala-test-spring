package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원가입 요청")
public record CustomerCreateRequestDto(

                @Schema(description = "고객 로그인 ID (unique)", example = "skala01") @NotBlank(message = "customerId는 필수입니다.") String customerId,

                @Schema(description = "고객 비밀번호", example = "pw1234") @NotBlank(message = "customerPassword는 필수입니다.") String customerPassword,

                @Schema(description = "고객 이름", example = "홍길동") @NotBlank(message = "customerName은 필수입니다.") String customerName,

                @Schema(description = "초기 지급 포인트. 미지정 시 서버 기본 정책값이 적용됨", example = "10000", nullable = true) Long point

) {
}