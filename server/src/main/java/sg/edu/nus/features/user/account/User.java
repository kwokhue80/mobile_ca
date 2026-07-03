package sg.edu.nus.features.user.account;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.common.Updatable;
import sg.edu.nus.features.wellness.model.ActivityRecord;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.model.ExerciseLog;
import sg.edu.nus.features.wellness.model.FoodLog;
import sg.edu.nus.features.wellness.model.HydrationLog;
import sg.edu.nus.features.wellness.model.MoodLog;
import sg.edu.nus.features.wellness.model.SleepLog;
import sg.edu.nus.features.wellness.model.WeightLog;
import sg.edu.nus.features.user.profile.UserProfile;

/*
*   AUTHOR: Amelia
*   PURPOSE: User entity for login/registration
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends Updatable {
    
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email_address", length = 100, nullable = false, unique = true)
    private String emailAddress;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    // ASSOCIATIONS
    // No JPA delete cascade; already set in schema.sql
    @OneToOne(mappedBy = "user")
    private UserProfile profile;

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<SleepLog> sleepLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<FoodLog> foodLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<WeightLog> weightLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<HydrationLog> hydrationLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ExerciseLog> exerciseLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<MoodLog> moodLogs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ActivityRecord> activityRecords = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<DailyWellnessSummary> dailyWellnessSummaries = new ArrayList<>();

}
