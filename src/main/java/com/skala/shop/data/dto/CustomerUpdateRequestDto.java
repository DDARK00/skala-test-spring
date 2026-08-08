package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "고객 정보 수정 요청")
public record CustomerUpdateRequestDto(

                @Schema(description = "수정 대상 고객 ID", example = "skala01") @NotBlank String customerId,

                @Schema(description = "변경할 고객 이름", example = "홍길동") @NotBlank String customerName

) {
}