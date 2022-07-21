package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded
    private Address address;

    @JsonIgnore //json 변환시 skip 요런걸 프레젠테이션 계층의
    //로직을 넣으면 어떤건 필요하고 어떤건 필요없고 지저분해진다.
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

}
