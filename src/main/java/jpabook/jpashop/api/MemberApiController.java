package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

//@Controller + @ResponseBody = @RestController
@RestController //response를 json, xml로 리턴
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 등록 V1: 요청 값으로 Member 엔티티를 직접 받는다.
     * 문제점
     * - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     *   - 엔티티에 API 검증을 위한 로직이 들어간다. (@NotEmpty 등등)
     *   - 실무에서는 회원 엔티티를 위한 API가 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한 
     *     모든 요청 요구사항을 담기는 어렵다.
     * - 엔티티가 변경되면 API 스펙이 변한다.
     * 결론
     * - API 요청 스펙에 맞추어 별도의 DTO를 파라미터로 받는다.
     */
    @PostMapping("/api/v1/members") //@RequestBody json 형태를 멤버객체에 넣는다.
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
    	//1. Entity Member에 validation을 넣엇는데, 어떤 api에서는 필요없을 수 있고, 어떤api에는
    	//필요할 수 있다.
    	//2. 또 다른데에선 멤버엔티티의 name이 아닌 memNm으로 받고 싶다고 한다면 Member 엔티티를 건드려야한다.
    	//이러면 API 스펙이 변경되어 다른 곳에서 오류가 발생할 수 있다. 따라서 별도의 DTO를 통해 받자.
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {

        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 수정 API
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, 
    		@RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName()); //커맨드(insert, update, delete 등의 동작)
        Member findMember = memberService.findOne(id); //쿼리 분리
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * 조회 V1: 응답 값으로 엔티티를 직접 외부에 노출한다.
     * 문제점
     * - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     *   - 기본적으로 엔티티의 모든 값이 노출된다.
     *   - 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등등)
     *   - 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 
     *     위한 프레젠테이션 응답 로직을 담기는 어렵다.
     * - 엔티티가 변경되면 API 스펙이 변한다.
     * - 추가로 컬렉션을 직접 반환하면 항후 API 스펙을 변경하기 어렵다.(별도의 Result 클래스 생성으로 해결)
     * 결론
     * - API 응답 스펙에 맞추어 별도의 DTO를 반환한다.
     */
    //조회 V1: 안 좋은 버전, 모든 엔티티가 노출, @JsonIgnore -> 이건 정말 최악, 
    // api가 이거 하나인가! 화면에 종속적이지 마라!
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    /**
     * 조회 V2: 응답 값으로 엔티티가 아닌 별도의 DTO를 반환한다.
     * 요렇게 리턴하면 확장이 어렵다.
     * {
     *   id : 1,
     *   name : shin
     * }
     * 아래 처럼 count를 추가할 수도 있으므로 ResultDTO처럼 반환한다.
     * {
     *   data : [
     *     {id : 1, name : shin}
     *   ],
     *   count : 1
     * }
     */
    @GetMapping("/api/v2/members")
    public Result membersV2() {

        List<Member> findMembers = memberService.findMembers();
        //엔티티 -> DTO 변환
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName())) //map : 바꿔치기
                .collect(Collectors.toList());
//        return new Result(collect.size(), collect); 요런식으로 확장가능
        return new Result(collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
//    	private int count; 요런식으로 확장가능
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }
    //api 별로 넘어오는 파라미터가 다르기 때문에 별도의 DTO를 사용
    //소스만 보고도 파라미터 뭐가 넘어오는지 알 수 있다.
    //API문서를 까봐야안다.
    //밸리데이션이 있다면 notEmpty를 넣으면된다. 엔티티를 공유하면
    //각기 다른 API들이 다 밸리데이션이 필요하지 않을 수 있다.
    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}