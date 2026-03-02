package com.fakel.repository;

import com.fakel.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Все сообщения для получателя (с пагинацией)
    Page<Message> findByRecipientIdOrderBySentAtDesc(Long recipientId, Pageable pageable);

    // Все сообщения от отправителя (для преподавателя - история отправленных)
    Page<Message> findBySenderIdOrderBySentAtDesc(Long senderId, Pageable pageable);

    // Фильтр по дате для получателя
    Page<Message> findByRecipientIdAndSentAtBetweenOrderBySentAtDesc(
            Long recipientId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Фильтр по дате для отправителя
    Page<Message> findBySenderIdAndSentAtBetweenOrderBySentAtDesc(
            Long senderId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Непрочитанные сообщения для получателя
    Page<Message> findByRecipientIdAndIsReadingFalseOrderBySentAtDesc(Long recipientId, Pageable pageable);

    // Количество непрочитанных
    Long countByRecipientIdAndIsReadingFalse(Long recipientId);

    // Обновление статуса для списка сообщений (принадлежащих получателю)
    @Modifying
    @Query("UPDATE Message m SET m.isReading = :isReading WHERE m.id IN :messageIds AND m.recipient.id = :recipientId")
    int updateStatusForRecipient(@Param("messageIds") List<Long> messageIds,
                                 @Param("recipientId") Long recipientId,
                                 @Param("isReading") Boolean isReading);

    // Удаление сообщений (принадлежащих получателю)
    @Modifying
    @Query("DELETE FROM Message m WHERE m.id IN :messageIds AND m.recipient.id = :recipientId")
    int deleteForRecipient(@Param("messageIds") List<Long> messageIds,
                           @Param("recipientId") Long recipientId);

    // Проверка принадлежности сообщения получателю
    @Query("SELECT COUNT(m) > 0 FROM Message m WHERE m.id = :messageId AND m.recipient.id = :recipientId")
    boolean existsByIdAndRecipientId(@Param("messageId") Long messageId,
                                     @Param("recipientId") Long recipientId);

    // Для преподавателя - статистика
    Long countBySenderId(Long senderId);
    Long countBySenderIdAndIsReadingTrue(Long senderId);
    Long countBySenderIdAndIsReadingFalse(Long senderId);

    // Отправленные с фильтром по прочитанности
    Page<Message> findBySenderIdAndIsReadingFalseOrderBySentAtDesc(Long senderId, Pageable pageable);

    // Отправленные с фильтром по дате и прочитанности
    Page<Message> findBySenderIdAndSentAtBetweenAndIsReadingFalseOrderBySentAtDesc(
            Long senderId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Удаление для отправителя
    @Modifying
    @Query("DELETE FROM Message m WHERE m.id IN :messageIds AND m.sender.id = :senderId")
    int deleteForSender(@Param("messageIds") List<Long> messageIds, @Param("senderId") Long senderId);
}