package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record CustomerLoginRequestDto(

        @Schema(description = "고객 로그인 ID", example = "skala01") @NotBlank String customerId,

        @Schema(description = "고객 비밀번호", example = "pw1234") @NotBlank String customerPassword

) {
}