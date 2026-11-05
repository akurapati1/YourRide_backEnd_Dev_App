package UberBackendDevApp.CabBookingAppBackend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupDto {
    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "email is required") @Email(message = "email must be valid")
    private String email;
    @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}
