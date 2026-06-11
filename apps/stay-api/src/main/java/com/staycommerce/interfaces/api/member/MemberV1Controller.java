package com.staycommerce.interfaces.api.member;

import com.staycommerce.domain.member.MemberInfo;
import com.staycommerce.domain.member.MemberService;
import com.staycommerce.interfaces.api.ApiResponse;
import com.staycommerce.interfaces.api.auth.AuthenticatedMember;
import com.staycommerce.interfaces.api.auth.AuthenticatedMemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/members")
public class MemberV1Controller implements MemberV1ApiSpec {

    private final MemberService memberService;

    @PostMapping
    @Override
    public ApiResponse<MemberV1Dto.RegisterResponse> register(
            @RequestBody MemberV1Dto.RegisterRequest request) {
        MemberInfo info = memberService.register(
            request.loginId(),
            request.password(),
            request.name(),
            request.birthday(),
            request.email(),
            request.phoneNumber()
        );
        return ApiResponse.success(MemberV1Dto.RegisterResponse.from(info));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<MemberV1Dto.MyInfoResponse> getMyInfo(
            @AuthenticatedMember AuthenticatedMemberInfo auth) {
        MemberInfo info = memberService.getMyInfo(auth.loginId());
        return ApiResponse.success(MemberV1Dto.MyInfoResponse.from(info));
    }

    @PatchMapping("/me/password")
    @Override
    public ApiResponse<Object> changePassword(
            @AuthenticatedMember AuthenticatedMemberInfo auth,
            @RequestBody MemberV1Dto.ChangePasswordRequest request) {
        memberService.changePassword(auth.loginId(), request.currentPassword(), request.newPassword());
        return ApiResponse.success();
    }
}
