package com.dailymission.batch.springboot.repository.mission;


import com.dailymission.batch.springboot.repository.common.BaseTimeEntity;
import com.dailymission.batch.springboot.repository.mission.rule.MissionRule;
import com.dailymission.batch.springboot.repository.participant.Participant;
import com.dailymission.batch.springboot.repository.post.Post;
import com.dailymission.batch.springboot.repository.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;


import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

@Getter
@NoArgsConstructor
@Entity
public class Mission extends BaseTimeEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "MISSION_RULE_ID")
    @JsonManagedReference
    private MissionRule missionRule;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @OneToMany(mappedBy = "mission")
    @JsonBackReference
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "mission")
    private List<Post> posts = new ArrayList<>();

    @Column(name = "TITLE", nullable = false, length = 20)
    // @Size(min = 1, max = 20)
    private String title;

    @Column(name = "CONTENT", nullable = false, length = 50)
    // @Size(min = 10, max = 50)
    private String content;

    @Column(name = "ORIGINAL_FILE_NAME", nullable = false)
    private String originalFileName;

    @Column(name = "FILE_EXTENSION", nullable = false)
    private String fileExtension;

    @Column(name="IMAGE_URL", nullable = false, length = 2000)
    private String imageUrl;

    // 썸네일 (Hot)
    @Column(name="THUMBNAIL_URL_HOT", nullable = false, length = 2000)
    private String thumbnailUrlHot;

    // 썸네일 (New)
    @Column(name="THUMBNAIL_URL_NEW", nullable = false, length = 2000)
    private String thumbnailUrlNew;

    // 썸네일 (전체)
    @Column(name="THUMBNAIL_URL_ALL", nullable = false, length = 2000)
    private String thumbnailUrlAll;

    // 썸네일 (디테일)
    @Column(name="THUMBNAIL_URL_DETAIL", nullable = false, length = 2000)
    private String thumbnailUrlDetail;

    @Column(name = "CREDENTIAL", nullable = false)
    private String credential;

    @Column(name = "START_DATE", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(name = "ENDED", nullable = false)
    private boolean ended;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    // 종료
    public void end(){
        this.ended = true;
    }
}
