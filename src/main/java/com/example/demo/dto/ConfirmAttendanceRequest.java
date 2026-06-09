package com.example.demo.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConfirmAttendanceRequest(Integer rewardAmount) {

    public static final int DEFAULT_REWARD_AMOUNT = 5;

    public int resolvedRewardAmount() {
        return rewardAmount == null ? DEFAULT_REWARD_AMOUNT : rewardAmount;
    }
}
