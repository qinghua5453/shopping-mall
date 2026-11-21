package com.shopmall.security.api.facade;

import com.shopmall.security.api.dto.AdminLoginCommand;
import com.shopmall.security.api.dto.AdminMeResponse;
import com.shopmall.security.api.dto.AdminRefreshCommand;
import com.shopmall.security.api.dto.AdminTokenResponse;

public interface AdminAuthFacade {
    AdminTokenResponse login(AdminLoginCommand command);

    AdminTokenResponse refresh(AdminRefreshCommand command);

    AdminMeResponse me(Long adminId);

    void logout(Long adminId, String sessionId);
}
