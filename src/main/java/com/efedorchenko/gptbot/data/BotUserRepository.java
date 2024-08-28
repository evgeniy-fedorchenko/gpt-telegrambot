package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.entity.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    @Modifying
    @Query("UPDATE BotUser u SET u.yagptReqsToday = 0, u.yaartReqsToday = 0, u.tokensSpentToday = 0")
    void resetDailyMetrics();

}
