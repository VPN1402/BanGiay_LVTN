package com.example.LVTN.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên nhà cung cấp / xưởng giày không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "0[0-9]{9,10}", message = "Số điện thoại phải từ 10-11 số và bắt đầu bằng số 0")
    private String phone;

    @Email(message = "Địa chỉ Email đối tác không đúng định dạng")
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp //tu dong thêm time
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "supplier")
    private List<ImportReceipt> importReceipts;
}