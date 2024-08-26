package com.food.ordering.system.restaurant.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.restaurant.service.domain.exception.RestaurantApplicationServiceException;
import com.food.ordering.system.restaurant.service.domain.exception.RestaurantNotFoundException;
import com.food.ordering.system.restaurant.service.domain.ports.input.message.listener.RestaurantApprovalRequestMessageListener;
import com.food.ordering.system.restaurant.service.messaging.mapper.RestaurantMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalRequestKafkaListener implements KafkaConsumer<RestaurantApprovalRequestAvroModel> {

    private final RestaurantApprovalRequestMessageListener restaurantApprovalRequestMessageListener;
    private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
            topics = "${restaurant-service.restaurant-approval-request-topic-name}")
    public void receive(@Payload List<RestaurantApprovalRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{}개의 메시지수신 ... messages='{}' key='{}' partitions='{}' and offset='{}'",
                messages.size()
                , messages
                , keys.toString()
                , partitions.toString()
                , offsets.toString());

        messages.forEach(restaurantApprovalRequestAvroModel -> {
            try {
                log.info("식당승인요청을 처리중입니다. order id: {}", restaurantApprovalRequestAvroModel.getOrderId());
                restaurantApprovalRequestMessageListener.approveOrder(
                        restaurantMessagingDataMapper.restaurantApprovalRequestAvroModelToRestaurantApprovalRequest(restaurantApprovalRequestAvroModel)
                );
            }catch (DataAccessException e){
                SQLException sqlException = (SQLException) e.getRootCause();
                if(sqlException != null
                        && sqlException.getSQLState() != null
                        && PSQLState.UNIQUE_VIOLATION.getState().equals(sqlException.getSQLState())){
                    log.error("RestaurantApprovalRequestKafkaListener에서 주문 ID: {}에 대해 SQL 상태: {}를 가진 고유 제약 조건 예외가 발생했습니다.",
                            sqlException.getSQLState(),
                            restaurantApprovalRequestAvroModel.getOrderId());
                } else {
                    throw new RestaurantApplicationServiceException("식당승인요청을 처리하는 동안 예외가 발생했습니다." + e.getMessage(), e);
                }
            }catch (RestaurantNotFoundException e){
                log.error("RestaurantApprovalRequestKafkaListener에서 주문 ID: {}에 대해 레스토랑 ID:{} 식당을 찾을 수 없습니다.",
                        restaurantApprovalRequestAvroModel.getOrderId(),
                        restaurantApprovalRequestAvroModel.getRestaurantId());
            }
        });
    }
}

