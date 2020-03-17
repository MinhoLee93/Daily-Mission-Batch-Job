package com.dailymission.batch.springboot.repository.user;



import com.dailymission.batch.springboot.repository.common.BaseTimeEntity;
import com.dailymission.batch.springboot.repository.mission.Mission;
import com.dailymission.batch.springboot.repository.participant.Participant;
import com.dailymission.batch.springboot.repository.post.Post;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/*
* Let’s now create the Entity classes of our application.
* Following is the definition of the User class -
* */
@Getter
@Setter
@Entity
@Table(name = "user", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@NoArgsConstructor
public class User extends BaseTimeEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , length = 20)
    //@Size(min = 1, max = 20)
    private String name;

    //@Email
    @Column(nullable = false)
    private String email;

    private String imageUrl;

    private String thumbnailUrl;

    @Column(name = "ORIGINAL_FILE_NAME")
    private String originalFileName;

    @Column(name = "FILE_EXTENSION")
    private String fileExtension;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @JsonIgnore
    private String password;

    //@NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Mission> missions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Participant> participants = new ArrayList<>();

}
