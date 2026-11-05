package UberBackendDevApp.CabBookingAppBackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RatingDto {
    @NotNull(message = "rideId is required")
    private Long rideId;
    @NotNull @Min(value = 1, message = "rating must be 1-5") @Max(value = 5, message = "rating must be 1-5")
    private Integer rating;
}
