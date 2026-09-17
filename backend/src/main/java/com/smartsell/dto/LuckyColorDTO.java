package com.smartsell.dto;

import java.util.List;

public class LuckyColorDTO {

    // sent from the frontend after the customer picks a birth day + a goal
    public record Request(String sessionToken, String birthWeekday, String luckyGoal) {}

    public record Result(String color, List<String> colorHexes, String reasoning, List<ProductDTO> products) {}
}
