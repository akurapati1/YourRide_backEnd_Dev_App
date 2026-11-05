package UberBackendDevApp.CabBookingAppBackend.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RideStartDto {
    @NotBlank(message = "otp is required")
    private String otp;
}
