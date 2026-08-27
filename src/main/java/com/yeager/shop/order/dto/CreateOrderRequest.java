package com.yeager.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {
    @NotBlank(message = "{order.create.recipient-name.not-blank}")
    @Size(max = 200, message = "{order.create.recipient-name.size}")
    private String recipientName;

    @NotBlank(message = "{order.create.recipient-phone.not-blank}")
    @Size(max = 30, message = "{order.create.recipient-phone.size}")
    private String recipientPhone;

    @NotBlank(message = "{order.create.country.not-blank}")
    @Size(max = 100, message = "{order.create.country.size}")
    private String country;

    @NotBlank(message = "{order.create.city.not-blank}")
    @Size(max = 100, message = "{order.create.city.size}")
    private String city;

    @NotBlank(message = "{order.create.street.not-blank}")
    @Size(max = 255, message = "{order.create.street.size}")
    private String street;

    @NotBlank(message = "{order.create.house.not-blank}")
    @Size(max = 50, message = "{order.create.house.size}")
    private String house;

    @Size(max = 50, message = "{order.create.apartment.size}")
    private String apartment;

    @Size(max = 20, message = "{order.create.postal-code.size}")
    private String postalCode;
}
