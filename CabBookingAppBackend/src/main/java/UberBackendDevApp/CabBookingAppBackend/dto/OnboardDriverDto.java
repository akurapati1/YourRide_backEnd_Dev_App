package UberBackendDevApp.CabBookingAppBackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OnboardDriverDto {
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;
}
