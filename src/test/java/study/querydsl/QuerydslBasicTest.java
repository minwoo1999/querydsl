package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.criterion.Projection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.swing.*;
import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory=new JPAQueryFactory(em);
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

    }

    @Test
    public void startJPQL() throws Exception{

        //member1을 찾아라

        Member findMember = em.createQuery("select m from Member m where m.username= :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() throws Exception{



//        QMember m = new QMember("m");
//        QMember m =QMember.member;


        Member findMember=queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");


    }
    
    @Test
    public void search() throws Exception{


        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.between(10,30)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }
    
    @Test
    public void searchAndParam() throws Exception{

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    
    @Test
    public void resultFetch() throws Exception{

//        List<Member> fetch= queryFactory
//                .selectFrom(member)
//                .fetch();
//        Member fetchOne=queryFactory
//                    .selectFrom(member)
//                    .fetchOne();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        //페이징용 쿼리
        results.getTotal();
        List<Member> content=results.getResults();
        results.getLimit();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     *
     */
    @Test
    public void sort() throws Exception{

            em.persist(new Member(null,100));
            em.persist(new Member("member5",100));
            em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();



    }

    @Test
    public void paging1() throws Exception{

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() throws Exception{

        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);


    }

    /**
     *
     * 팀의 이름과 각팀의 평균 연령을 구해라
     *
     */

    @Test
    public void group() throws Exception{

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA= result.get(0);
        Tuple teamB= result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);


        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * 팀 A에 소속된 모든회원
     *
     */
    
    @Test
    public void join() throws Exception{

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    
    }

    /**
     *
     * 세타 조인
     * 맞조인 둘다 조인해버림
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception{
        
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result).extracting("username")
                .containsExactly("teamA","teamB");

    }

    /**
     *
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인해라, 회원은 모두조회
     * JPQL:select m,t from Member m left join m.team t on t.name="teamA"
     */
    @Test
    public void join_on_filtering() throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team,team)
//                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple="+tuple);
        }

    }
    /**
     * 연관관계가 없는 엔티티 외부조인
        회원의 이름이 팀 이름과 같은 대상 외부 조인
     */

    @Test
    public void join_on_no_realation() throws Exception{

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .where(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple="+tuple);
        }


    }

    @PersistenceUnit
    EntityManagerFactory emf;
    
    @Test
    public void fetchJoinNO() throws Exception{

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        Assertions.assertThat(loaded).as("패치 조인 미적용").isFalse();

    }

    @Test
    public void fetchJoin() throws Exception{

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        Assertions.assertThat(loaded).as("패치 조인 적용").isTrue();

    }


    @Test
    public void simpleProjection() throws Exception{

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for(String s :result){
            System.out.println("s="+s);
        }
    }
    
    @Test
    public void tupleProjection() throws Exception{


        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for(Tuple tuple:result){
            String username=tuple.get(member.username);
            Integer age=tuple.get(member.age);
            System.out.println("username="+username);
            System.out.println("age="+age);
        }
    }
    
    @Test
    public void findDtoByJPQL() throws Exception{

        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m ", MemberDto.class)
                .getResultList();

        for(MemberDto memberDto: result){
            System.out.println("memberdto="+memberDto);
        }

    }

    //getter setter를 이용해서 값을 가져옴
    @Test
    public void findDtoBySetter() throws Exception{

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto:result){
            System.out.println("memberDto="+memberDto);
        }

    }

    //getter ,setter 무시하고 필드에 값이 바로 꽂힘
    @Test
    public void findDtoByField() throws Exception{

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto:result){
            System.out.println("memberDto="+memberDto);
        }

    }


    @Test
    public void findDtoByConstructor() throws Exception{

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto:result){
            System.out.println("memberDto="+memberDto);
        }

    }

    @Test
    public void findUserDto() throws Exception{

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for(UserDto userDto:result){
            System.out.println("userDto="+userDto);
        }

    }
    
    @Test
    public void findDtoByQueryProjection() throws Exception{

        List<MemberDto> result = queryFactory.
                select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto="+memberDto);
        }

    }

    @Test
    public void dynamicQuery_BoolBuilder() throws Exception{

        String usernameParam="member1";
        Integer ageParam=null;

        List<Member> result=searchMember1(usernameParam,ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);

    }


    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder booleanBuilder=new BooleanBuilder();

        if(usernameCond!=null){
            booleanBuilder.and(member.username.eq(usernameCond));
        }

        if(ageCond!=null){
            booleanBuilder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(booleanBuilder)
        .fetch();
    }
    
    @Test
    public void dynamicQuery_WhereParam() throws Exception{

        String usernameParam="member1";
        Integer ageParam=10;

        List<Member> result=searchMember2(usernameParam,ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        if(usernameCond!=null){
            return member.username.eq(usernameCond);
        }else{
            return null;
        }

    }
    private Predicate ageEq(Integer ageCond) {
        if (ageCond!=null){
            return member.age.eq(ageCond);
        }else{
            return null;
        }
    }

    @Commit
    @Test
    public void bulkUpdate() throws Exception{

        //member1=10->비회원
        //member2=20->비회원
        //member3=30->유지
        //member4=40->유지

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //DB에서 꺼내온 데이터보다 영속성컨텍스트가 우선권을가짐
        //벌크 연산후 flush와 clear을 해줘라

        em.flush();
        em.clear();
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1:"+member1);
        }
        


    }
    @Test
    public void bulkAdd() throws Exception{

        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

//        long multi = queryFactory
//                .update(member)
//                .set(member.age, member.age.multiply(1))
//                .execute();
    }
    
    @Test
    public void bulkDelete() throws Exception{

        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

    }






}
