package jpabook.jpastore.api;

import jpabook.jpastore.domain.Address;
import jpabook.jpastore.domain.Order;
import jpabook.jpastore.domain.OrderItem;
import jpabook.jpastore.domain.OrderStatus;
import jpabook.jpastore.repository.OrderRepository;
import jpabook.jpastore.repository.OrderSearch;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * V1. 엔티티 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다.
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 *
 * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 *
 * V3. 엔티티를 조회해서 DTO로 변환 (fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능)
 *
 * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 *
 * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 *
 * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
 * - 페이징 불가능...
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore 추가 필요
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        for (Order order : all) {
            order.getMember().getName(); // Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(orderItem -> orderItem.getItem().getName()); // Lazy 강제 초기화
        }

        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDTO> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDTO> result = orders.stream()
                .map(order -> new OrderDTO(order))
                .collect(Collectors.toList());

        return result;
    }

    @Getter
    static class OrderDTO {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDTO> orderItems;

        public OrderDTO(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();

//            order.getOrderItems().stream().forEach(orderItem -> orderItem.getItem().getName());
//            orderItems = order.getOrderItems();

            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDTO(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDTO {

        private String itemName; // 상품명
        private int orderPrice; // 주문 가격
        private int count; // 주문 수량

        public OrderItemDTO(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

}
