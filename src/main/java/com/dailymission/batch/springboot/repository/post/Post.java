package com.dailymission.batch.springboot.repository.post;


import com.dailymission.batch.springboot.repository.common.BaseTimeEntity;
import com.dailymission.batch.springboot.repository.mission.Mission;
import com.dailymission.batch.springboot.repository.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;


@Getter
@NoArgsConstructor
@Entity
public class Post extends BaseTimeEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MISSION_ID", referencedColumnName = "id",  nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "TITLE", length = 500, nullable = false)
    private String title;

    @Column(name = "CONTENT", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "ORIGINAL_FILE_NAME", nullable = false)
    private String originalFileName;

    @Column(name = "FILE_EXTENSION", nullable = false)
    private String fileExtension;

    @Column(name="IMAGE_URL", nullable = false)
    private String imageUrl;

    @Column(name="THUMBNAIL_URL", nullable = false)
    private String thumbnailUrl;

    @Column(name="THUMBNAIL_URL_MISSION", nullable = false)
    private String thumbnailUrlMission;

    @Column(name="THUMBNAIL_URL_MY", nullable = false)
    private String thumbnailUrlMy;

    @Column(name = "DELETED")
    private boolean deleted;

}
