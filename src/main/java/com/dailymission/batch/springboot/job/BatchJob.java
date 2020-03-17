package com.dailymission.batch.springboot.job;

import com.dailymission.batch.springboot.repository.mission.Mission;
import com.dailymission.batch.springboot.repository.mission.rule.Week;
import com.dailymission.batch.springboot.repository.participant.Participant;
import com.dailymission.batch.springboot.repository.post.Post;
import com.dailymission.batch.springboot.repository.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchJob {

    /**
     * Web hook Test 12
     * */
    public final String JOB_NAME = "banJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize = 100;

    /**
     * [ 2020-03-16 : 이민호 ]
     * 설명 : 1. 미션 인증 요일에 인증하지 않은 사용자를 강퇴한다.
     *        2. 미션의 참여자가 0이거나, 미션의 endDate 가 지났을경우 미션을 종료한다.
     * */
    @Bean
    public Job job(){
        return jobBuilderFactory.get(JOB_NAME)
                                .start(banStep())
                                .next(endStep())
                                .build();
    }


    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 미션 인증 요일에 인증하지 않은 사용자를 강퇴한다.
     * */
    @Bean
    @JobScope
    public Step banStep(){
        return stepBuilderFactory.get("banStep")
                .<Participant, Participant>chunk(chunkSize)
                .reader(participantsReader())
                .processor(banProcessor(null))
                .writer(bannedParticipantWriter())
                .build();
    }


    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 미션의 참여자가 0이거나, 미션의 endDate 가 지났을경우 미션을 종료한다.
     * */
    @Bean
    @JobScope
    public Step endStep(){
        return stepBuilderFactory.get("endStep")
                .<Mission, Mission>chunk(chunkSize)
                .reader(missionsReader())
                .processor(endProcessor(null))
                .writer(endMissionWriter())
                .build();
    }

    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 전체 미션의 참여자 중 강퇴되지 않은 참여자들을 조회한다.
     * */
    @Bean
    @StepScope
    public JpaPagingItemReader<Participant> participantsReader(){

        // 페이지 고정
        JpaPagingItemReader<Participant> reader = new JpaPagingItemReader<Participant>(){
              @Override
              public int getPage(){
                  return 0;
              }
        };
        reader.setName("participantsReader");
        reader.setQueryString("SELECT p FROM Participant p where banned = false order by id");
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(chunkSize);

        return reader;
    }


    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 종료 및 삭제되지 않은 모든 미션을 조회힌다.
     * */
    @Bean
    @StepScope
    public JpaPagingItemReader<Mission> missionsReader(){

        // 페이지 고정
        JpaPagingItemReader<Mission> reader = new JpaPagingItemReader<Mission>(){
            @Override
            public int getPage(){
                return 0;
            }
        };
        reader.setName("missionsReader");
        reader.setQueryString("SELECT p FROM Mission p where deleted = false and ended = false order by id");
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(chunkSize);

        return reader;
    }


    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 미션 인증 요일에 인증하지 않은 사용자를 강퇴한다.
     * */
    @Bean
    @StepScope
    public ItemProcessor<Participant, Participant> banProcessor(@Value("#{jobParameters[requestDate]}") String requestDate){
        return participant -> {
            // now
            LocalDate now = LocalDate.now();

            // 03:00 ~ 03:00
            LocalDateTime endTime =  LocalDateTime.of(now.getYear(),now.getMonth(),now.getDayOfMonth(), 03, 00, 00);
            LocalDateTime startTime = endTime.minusDays(1);

            // 요일
            DayOfWeek week = startTime.getDayOfWeek();
            log.info(">>>>>>>>>>>>>>  current date {}", endTime.toString());
            log.info(">>>>>>>>>>>>>>  current day of week {}", week.getDisplayName(TextStyle.FULL, Locale.KOREAN));

            // Entity
            User user = participant.getUser();
            Mission mission = participant.getMission();
            Week rule = mission.getMissionRule().getWeek();
            log.info(">>>>>>>>>>>>>>  current mission {}", mission.getTitle());
            log.info(">>>>>>>>>>>>>>  user {}", user.getName());

            /**
             * [ 2020-03-17 : 이민호 ]
             * 설명 : 미션이 종료되었거나, 삭제되었으면 패스한다.
             * */
            if(mission.isDeleted() || mission.isEnded()) {
                return null;
            }

            /**
             * [ 2020-03-17 : 이민호 ]
             * 설명 : 아직 미션이 시작하지 않았으면 패스한다.
             *        ex) 시작일 : 2020-03-17
             *            현재 : 2020-03-17 03:00:00
             *            -> 2020-03-16 03:00 ~ 2020-03-17 03:00 (즉, 최소 16일날엔 시작한 미션이여야 한다.)
             * */
            if(mission.getStartDate().isAfter(now.minusDays(1))){
                log.info(">>>>>>>>>>>>>>  mission startDate is {}", mission.getStartDate());
                log.info(">>>>>>>>>>>>>>  ban check requestDate is {}",now.minusDays(1));
                return null;
            }


            /**
             * [ 2020-03-17 : 이민호 ]
             * 설명 : 제출 의무 요일이 아니면 패스한다.
             * */
            switch (week.getValue()){
                case 1:
                    if(!rule.isMon()){
                        log.info("Monday is not mandatory");
                        return null;
                    }
                    break;
                case 2:
                    if(!rule.isTue()){
                        log.info("Tuesday is not mandatory");
                        return null;
                    }
                    break;
                case 3:
                    if(!rule.isWed()){
                        log.info("Wednesday is not mandatory");
                        return null;
                    }
                    break;
                case 4:
                    if(!rule.isThu()){
                        log.info("Thursday is not mandatory");
                        return null;
                    }
                    break;
                case 5:
                    if(!rule.isFri()){
                        log.info("friday is not mandatory");
                        return null;
                    }
                    break;
                case 6:
                    if(!rule.isSat()){
                        log.info("saturday is not mandatory");
                        return null;
                    }
                    break;
                case 7:
                    if(!rule.isSun()){
                        log.info("sunday is not mandatory");
                        return null;
                    }
                    break;
            }


            // 제출한 포스트 목록 (전날 03:00 ~ 현재 03:00)
            List<Post> posts = mission.getPosts()
                    .stream()
                    .filter(post -> (post.getCreatedDate().isBefore(endTime))&&(post.getCreatedDate().isAfter(startTime)))
                    .filter(post -> post.getUser().equals(user))
                    .collect(Collectors.toList());

            // 포스트 제출 유저는 미션 성공
            if(posts.size()>0){
                for(Post p : posts){
                    log.info(">>>>>>>>>>>>>> 포스트 {}", p.getTitle());
                }
            }else{
                // 미 제출자 강퇴 처리
                log.info(">>>>>>>>>>>>>> {} 강퇴", user.getName());
                participant.ban();
            }

            return participant;
        };
    }

    /**
     * [ 2020-03-17 : 이민호 ]
     * 설명 : 강퇴 후 참여자가 0명이거나, endDate 가 지난 미션을 종료한다.
     * */
    @Bean
    @StepScope
    public ItemProcessor<Mission, Mission> endProcessor(@Value("#{jobParameters[requestDate]}") String requestDate){
        return mission -> {

            LocalDate now = LocalDate.now();

            // Entity
            log.info(">>>>>>>>>>>>>>  current mission is {}", mission.getTitle());

            /**
             * [ 2020-03-17 : 이민호 ]
             * 설명 : 참여자가 0명인 미션을 종료한다.
             * */
            Long count = mission.getParticipants().stream().filter(p->p.isBanned()==false).count();
            log.info(">>>>>>>>>>>>>>  current mission participants is {}", count);
            if(count==0){
                mission.end();
                return mission;
            }

            /**
             * [ 2020-03-17 : 이민호 ]
             * 설명 : 미션 endDate 가 지난 미션을 종료한다.
             * */
            log.info(">>>>>>>>>>>>>>  mission endDate is {}", mission.getEndDate());
            if(now.isAfter(mission.getEndDate())){
                mission.end();
                return mission;
            }

            return null;
        };
    }

    @Bean
    @StepScope
    public JpaItemWriter<Participant> bannedParticipantWriter(){
        JpaItemWriter<Participant> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);

        return writer;
    }

    @Bean
    @StepScope
    public JpaItemWriter<Mission> endMissionWriter(){
        JpaItemWriter<Mission> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);

        return writer;
    }
}
