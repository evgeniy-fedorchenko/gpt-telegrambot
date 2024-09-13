package com.efedorchenko.gptbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;

import static org.hibernate.annotations.SourceType.VM;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class BotUser {

    @Id
    @EqualsAndHashCode.Include
    private long chatId;

    @CreationTimestamp(source = VM)
    @Column(columnDefinition = "TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private LocalDateTime createdAt;

    @CurrentTimestamp(source=VM)
    @Column(columnDefinition = "TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastModified;

    private String username;

    private String name;

    private int yagptReqsTotal;
    private int yaartReqsTotal;
    private long tokensSpentTotal;

    private int yagptReqsToday;
    private int yaartReqsToday;
    private int tokensSpentToday;

}
