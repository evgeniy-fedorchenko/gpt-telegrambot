package com.evgeniyfedorchenko.gptbot.statistic.repository;

import com.evgeniyfedorchenko.gptbot.statistic.entity.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {
}
