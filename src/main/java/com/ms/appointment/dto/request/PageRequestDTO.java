package com.ms.appointment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDTO {

    @Min(value = 1, message = "Page must be greater than or equal to 1")
    @Max(value = 100, message = "Page cannot be greater than 100")
    private int page = 1;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = 100, message = "Size cannot be greater than 100")
    private int size = 10;

    private String sortBy;

    @Pattern(
            regexp = "ASC|DESC",
            message = "Sort direction must be ASC or DESC"
    )
    private String sortDirection = "ASC";
}
