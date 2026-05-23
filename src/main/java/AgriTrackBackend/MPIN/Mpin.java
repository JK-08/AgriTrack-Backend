package AgriTrackBackend.MPIN;

import AgriTrackBackend.USERS.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "MPINS")
public class Mpin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MPIN_ID")
    private Long mpinId;

    @OneToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "MPIN")
    private String mpin;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}