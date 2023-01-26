package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
class MemberRepositoryTest {

    @Autowired

    MemberRepository memberRepository;

    @Autowired
    EntityManager em;
    @Test
    public void basicTest() throws Exception{

        Member member = new Member("member1",10);

        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        Assertions.assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        Assertions.assertThat(result2).containsExactly(member);


    }

    @Test
    public void serachTest() throws Exception{

        Team teamA=new Team("teamA");
        Team teamB=new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1=new Member("member1",10,teamA);
        Member member2=new Member("member2",20,teamA);

        Member member3=new Member("member3",30,teamB);
        Member member4=new Member("member4",40,teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        memberSearchCondition.setAgeGoe(35);
        memberSearchCondition.setAgeLoe(40);
        memberSearchCondition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(memberSearchCondition);

        Assertions.assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void serachPageSimple() throws Exception{

        Team teamA=new Team("teamA");
        Team teamB=new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1=new Member("member1",10,teamA);
        Member member2=new Member("member2",20,teamA);

        Member member3=new Member("member3",30,teamB);
        Member member4=new Member("member4",40,teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3); //0페이지에 3개의 데이터를 가져올것이다

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition,pageRequest);

        Assertions.assertThat(result.getSize()).isEqualTo(3);
        Assertions.assertThat(result.getContent()).extracting("username").containsExactly("member1","member2","member3");
    }

}