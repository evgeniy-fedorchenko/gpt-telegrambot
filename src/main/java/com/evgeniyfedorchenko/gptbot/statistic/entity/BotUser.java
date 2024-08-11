package com.evgeniyfedorchenko.gptbot.statistic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;

import static org.hibernate.annotations.SourceType.VM;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Entity
@Table(name = "bot_user")
public class BotUser {

    @Id
    @EqualsAndHashCode.Include
    private long chatId;

    @Column(unique = true)
    private String username;

    private String name;

    private int yaGptRequestCount;
    private int yaArtRequestCount;

    @CurrentTimestamp(source=VM)
    @Column(columnDefinition = "TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastModified;

}
