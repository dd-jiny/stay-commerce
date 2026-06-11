package com.staycommerce.interfaces.api.member;

import com.staycommerce.interfaces.api.ApiResponse;
import com.staycommerce.interfaces.api.auth.AuthenticatedMemberInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member V1 API", description = "회원 API")
public interface MemberV1ApiSpec {

    @Operation(summary = "회원가입", description = "새 회원을 등록합니다.")
    ApiResponse<MemberV1Dto.RegisterResponse> register(MemberV1Dto.RegisterRequest request);

    @Operation(summary = "내 정보 조회", description = "인증된 회원의 정보를 조회합니다.")
    ApiResponse<MemberV1Dto.MyInfoResponse> getMyInfo(AuthenticatedMemberInfo auth);

    @Operation(summary = "비밀번호 수정", description = "인증된 회원의 비밀번호를 수정합니다.")
    ApiResponse<Object> changePassword(AuthenticatedMemberInfo auth, MemberV1Dto.ChangePasswordRequest request);
}
