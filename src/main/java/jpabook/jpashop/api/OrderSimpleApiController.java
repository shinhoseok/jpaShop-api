package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.*;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 *
 * xToOne(ManyToOne, OneToOne) 관계 최적화
 * Order
 * Order -> Member
 * Order -> Delivery
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository; //의존관계 주입

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore Order의 member를 보고 Member를 갓다가
     * Member에 order를 보고 Order에 간다. 무한루프에 빠진다. 따라서 둘중 하나를 @JsonIgnore
     * 처리 해줘야한다.
     * 
     * Lazy가 아닌 EAGER로 설정을 해줘도 find같은 쿼리가 아닌 JPQL은 Order를 먼저조회하고 EAGER확인 Member
     * 를 후에 가져온다. 즉 LAZY와 동일하게 동작한다. 그리고 다른 비즈니스로직에서는 멤버가 필요없을 수 있으므로
     * EAGER로 변환하는 것은 최악의 선택이다.
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() { //List<Order>를 바로 리턴하는게 아닌 객체로 한번 감싸서 확장성이 좋아짐
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
     * List<SimpleOrderDto>를 바로 리턴하는게 아닌 객체로 한번 감싸서 리턴해야함
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());

        return result;
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
     * - fetch join으로 쿼리 1번 호출
     * 참고: fetch join에 대한 자세한 내용은 JPA 기본편 참고(정말 중요함)
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());
        return result;
    }
    
    //성능최적화 4.엔티티 결과를 DTO에 넣는 작업을 빼고 쿼리를 바로 DTO로 받는법
    //페치조인은 공용으로 사용할 수 있지만, 이건 로직 재활용이 불가능하다.
    //조회한 걸로 뭘 못한다. 영속성컨텍스트가 관리하지 않는다. DTO로 받기 때문
    //화면이 바뀌면 얘도 바껴야 하므로 3번이 좀 나은거 같다.
    //성능은 v4번이 낫다.
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

}
